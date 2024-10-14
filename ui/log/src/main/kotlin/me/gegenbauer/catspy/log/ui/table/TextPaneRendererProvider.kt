package me.gegenbauer.catspy.log.ui.table

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.cache.use
import me.gegenbauer.catspy.concurrency.CPU
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.java.ext.truncate
import me.gegenbauer.catspy.log.filter.ContentFilter
import me.gegenbauer.catspy.log.filter.DefaultLogFilter
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.render.StringRenderer
import me.gegenbauer.catspy.render.TextPaneRenderer
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.getMatchedList
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.BorderFactory
import javax.swing.JTable
import javax.swing.JTextPane
import javax.swing.plaf.ColorUIResource
import javax.swing.text.JTextComponent

class TextPaneRendererProvider : BaseLogCellRendererProvider() {
    override fun createRenderer(column: Column): LogCellRenderer {
        if (column.partIndex < 0) {
            return IndexRenderer(logMetadata)
        }

        if (column.supportFilter && column.uiConf.column.isHidden.not()) {
            return SearchableCellRenderer(logMetadata, getFilterIndex(column))
        }

        return SimpleLogCellRenderer(logMetadata)
    }

    override suspend fun buildDetailRendererComponent(logTable: LogTable, rows: List<Int>): JTextComponent {
        fun renderLogFilter(renderer: StringRenderer, logFilterItem: FilterItem, content: String) {
            val matchedList = logFilterItem.getMatchedList(content)
            matchedList.forEach {
                renderer.highlight(it.first, it.second, logMetadata.colorScheme.filterContentBackground)
                renderer.foreground(it.first, it.second, logMetadata.colorScheme.filterContentForeground)
            }
        }

        fun renderSearchFilter(renderer: StringRenderer, searchFilterItem: FilterItem, content: String) {
            val matchedList = searchFilterItem.getMatchedList(content)
            matchedList.forEach {
                renderer.highlight(it.first, it.second, logMetadata.colorScheme.searchContentBackground)
                renderer.foreground(it.first, it.second, logMetadata.colorScheme.searchContentForeground)
            }
        }

        val logFilter = logTable.tableModel.getLogFilter() as? DefaultLogFilter
        val logFilterItems = logFilter?.filters
            ?.filter { it.filter is ContentFilter }
            ?.map { it.filterItem }
            ?: emptyList()
        val searchFilterItem = logTable.tableModel.searchFilterItem
        val textComponent = JTextPane()
        withContext(Dispatchers.CPU) {
            val content = rows.joinToString("\n") { logTable.tableModel.getItemInCurrentPage(it).toString() }
            TextPaneRenderer.obtain().use {
                var offset = 0
                rows.forEachIndexed { index, row ->
                    ensureActive()
                    val logItem = logTable.tableModel.getItemInCurrentPage(row)
                    val line = logItem.toString()
                    val logBackground = logTable.getColumnBackground(logItem.num, row, logMetadata)
                    it.highlight(offset, offset + line.length - 1, logBackground)
                    val foreground = getLevel(logItem).logForeground
                    it.foreground(offset, offset + line.length - 1, foreground)
                    logFilterItems.forEach { filterItem -> renderLogFilter(it, filterItem, line) }
                    renderSearchFilter(it, searchFilterItem, line)
                    offset += line.length + 1
                }
                it.updateRaw(content)
                it.render(textComponent)
            }
        }
        return textComponent
    }

    private abstract class DefaultLogTableCellRenderer(logMetadata: LogMetadata) :
        TextPaneLogTableCellRenderer(logMetadata) {
        private val emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0)

        init {
            border = emptyBorder
        }

        override fun render(table: LogTable, textPane: JTextPane, renderer: StringRenderer, row: Int, content: String) {
            super.render(table, textPane, renderer, row, content)
            val logItem = table.tableModel.getItemInCurrentPage(row)
            updateBackground(table.getColumnBackground(logItem.num, row, logMetadata))
        }
    }

    private inner class SimpleLogCellRenderer(logMetadata: LogMetadata) : DefaultLogTableCellRenderer(logMetadata) {

        override fun render(table: LogTable, textPane: JTextPane, renderer: StringRenderer, row: Int, content: String) {
            super.render(table, textPane, renderer, row, content)
            renderer.foreground(getLevel(table.tableModel.getItemInCurrentPage(row)).logForeground)
        }
    }

    private inner class SearchableCellRenderer(
        logMetadata: LogMetadata,
        private val filterIndex: Int
    ) : DefaultLogTableCellRenderer(logMetadata) {
        override fun render(table: LogTable, textPane: JTextPane, renderer: StringRenderer, row: Int, content: String) {
            super.render(table, textPane, renderer, row, content)

            val logItem = table.tableModel.getItemInCurrentPage(row)
            val foreground = getLevel(logItem).logForeground
            renderer.foreground(0, content.length - 1, foreground)

            table.tableModel.searchFilterItem.getMatchedList(content).forEach {
                renderer.highlight(it.first, it.second, logMetadata.colorScheme.searchContentBackground)
                renderer.foreground(it.first, it.second, logMetadata.colorScheme.searchContentForeground)
            }

            val logFilter = table.tableModel.getLogFilter()
            if (logFilter !is DefaultLogFilter) return
            // filter 比 render 更新慢时出现异常
            if (filterIndex >= logFilter.filters.size) return
            logFilter.filters[filterIndex].filterItem.getMatchedList(content).forEach {
                renderer.highlight(it.first, it.second, logMetadata.colorScheme.filterContentBackground)
                renderer.foreground(it.first, it.second, logMetadata.colorScheme.filterContentForeground)
            }
        }

        override fun processContent(table: LogTable, content: String): String {
            val logFilter = table.tableModel.getLogFilter()
            if (logFilter !is DefaultLogFilter) return content
            if (filterIndex >= logFilter.filters.size) return content
            val maxLength = logFilter.filters[filterIndex].column.uiConf.column.charLen
            return content.truncate(maxLength)
        }
    }

    private class IndexRenderer(logMetadata: LogMetadata) : DefaultLogTableCellRenderer(logMetadata) {
        private val border = LineNumBorder(logMetadata.colorScheme.indexColumnSeparatorColor, 1)

        override fun render(
            table: LogTable,
            textPane: JTextPane,
            renderer: StringRenderer,
            row: Int,
            content: String
        ) {
            super.render(table, textPane, renderer, row, content)
            border.color = logMetadata.colorScheme.indexColumnSeparatorColor
            renderer.foreground(0, content.length - 1, logMetadata.colorScheme.indexColumnForeground)
        }
    }
}

open class TextPaneLogTableCellRenderer(protected val logMetadata: LogMetadata) : LogCellRenderer, JTextPane() {

    private val emptyBorder = BorderFactory.createEmptyBorder(0, 5, 0, 0)
    private var background: Color = ColorUIResource(Color.white)

    override var maxLength: Int = Int.MAX_VALUE
    override var logTable: LogTable? = null

    init {
        isEditable = false
        isOpaque = false
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        col: Int
    ): Component {
        val content = value?.toString() ?: ""
        val logTable = table as LogTable
        val processContent = processContent(logTable, content)
        val renderer = TextPaneRenderer.obtain().updateRaw(processContent)
        render(logTable, this, renderer, row, processContent)
        renderer.render(this)
        renderer.recycle()

        this.font = SettingsManager.settings.logSettings.font.nativeFont
        return this
    }

    open fun processContent(table: LogTable, content: String): String {
        return content
    }

    open fun render(table: LogTable, textPane: JTextPane, renderer: StringRenderer, row: Int, content: String) {
        // Empty Implementation
    }

    protected fun updateBackground(color: Color) {
        background = color
    }

    override fun paint(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        super.paint(g)
    }
}