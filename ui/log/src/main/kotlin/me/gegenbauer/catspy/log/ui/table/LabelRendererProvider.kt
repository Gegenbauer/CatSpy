package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.cache.with
import me.gegenbauer.catspy.java.ext.truncate
import me.gegenbauer.catspy.log.filter.DefaultLogFilter
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.ui.table.BaseLogCellRendererProvider.Companion.getColumnBackground
import me.gegenbauer.catspy.render.HtmlStringBuilder
import me.gegenbauer.catspy.render.LabelRenderer
import me.gegenbauer.catspy.render.RenderResult
import me.gegenbauer.catspy.render.StringRenderer
import me.gegenbauer.catspy.render.Tag
import me.gegenbauer.catspy.render.isValid
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.getMatchedList
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.JTextPane
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.text.html.HTMLEditorKit

class LabelRendererProvider : BaseLogCellRendererProvider() {

    override fun createRenderer(column: Column): LogCellRenderer {
        if (column.partIndex < 0) {
            return IndexRenderer(logMetadata)
        }
        if (column.supportFilter && column.uiConf.column.isHidden.not()) {
            return SearchableCellRenderer(logMetadata, getFilterIndex(column))
        }

        return SimpleLogCellRenderer(logMetadata)
    }

    override fun showSelectedRowsInDialog(
        logTable: LogTable,
        rows: List<Int>,
        popupActions: List<LogDetailDialog.PopupAction>
    ) {
        val renderedContent = renderRows(logTable, rows)
        val dialogTextComponent = JTextPane()
        dialogTextComponent.editorKit = HTMLEditorKit()
        dialogTextComponent.text = renderedContent
        val frame = logTable.contexts.getContext(JFrame::class.java) ?: return
        val logViewDialog = LogDetailDialog(frame, dialogTextComponent, popupActions, logMetadata)
        logViewDialog.setLocationRelativeTo(frame)
        logViewDialog.isVisible = true
    }

    private fun renderRows(logTable: LogTable, rows: List<Int>): String {
        val renderedContent = HtmlStringBuilder()
        val logFilter = logTable.tableModel.getLogFilter()
        if (logFilter !is DefaultLogFilter) return ""
        val messageFilter = logFilter.filters.firstOrNull { it.column is Column.MessageColumn } ?: return ""
        val logFilterItem = messageFilter.filterItem
        rows.forEachIndexed { index, row ->
            val logItem = logTable.tableModel.getItemInCurrentPage(row)
            val content = logItem.logLine
            LabelRenderer.obtain().with { renderer ->
                renderer.updateRaw(content)
                val foreground = getLevel(logItem).color
                renderer.foreground(foreground)
                val matchedList = logFilterItem.getMatchedList(content)
                matchedList.forEach {
                    renderer.highlight(it.first, it.second, logMetadata.colorScheme.filterContentBg)
                    renderer.foreground(it.first, it.second, logMetadata.colorScheme.filterContentFg)
                }
                renderedContent.append(renderer.renderWithoutTags())
                if (index != rows.lastIndex) renderedContent.addSingleTag(Tag.LINE_BREAK)
            }
        }
        return renderedContent.build()
    }

    private inner class SimpleLogCellRenderer(logMetadata: LogMetadata) : LabelLogTableCellRenderer(logMetadata) {
        init {
            horizontalAlignment = JLabel.LEFT
            verticalAlignment = JLabel.CENTER
        }

        override fun render(table: LogTable, renderer: StringRenderer, row: Int, content: String) {
            super.render(table, renderer, row, content)
            renderer.foreground(getLevel(table.tableModel.getItemInCurrentPage(row)).color)
        }
    }

    private open inner class SearchableCellRenderer(
        logMetadata: LogMetadata,
        private val filterIndex: Int
    ) : LabelLogTableCellRenderer(logMetadata) {

        init {
            horizontalAlignment = JLabel.LEFT
            verticalAlignment = JLabel.CENTER
        }

        override fun render(table: LogTable, renderer: StringRenderer, row: Int, content: String) {
            super.render(table, renderer, row, content)
            val logItem = table.tableModel.getItemInCurrentPage(row)
            val foreground = getLevel(logItem).color
            renderer.foreground(foreground)

            table.tableModel.searchFilterItem.getMatchedList(content).forEach {
                renderer.highlight(it.first, it.second, logMetadata.colorScheme.searchContentBg)
                renderer.foreground(it.first, it.second, logMetadata.colorScheme.searchContentFg)
            }

            val logFilter = table.tableModel.getLogFilter()
            if (logFilter !is DefaultLogFilter) return
            // filter 比 render 更新慢时出现异常
            if (filterIndex >= logFilter.filters.size) return
            logFilter.filters[filterIndex].filterItem.getMatchedList(content).forEach {
                renderer.highlight(it.first, it.second, logMetadata.colorScheme.filterContentBg)
                renderer.foreground(it.first, it.second, logMetadata.colorScheme.filterContentFg)
            }
        }

        override fun processContent(table: LogTable, content: String): String {
            return content.truncate(maxLength)
        }
    }

    private class IndexRenderer(logMetadata: LogMetadata) : LabelLogTableCellRenderer(logMetadata) {
        private val border = LineNumBorder(logMetadata.colorScheme.indexColumnSeparatorColor, 1)

        init {
            horizontalAlignment = JLabel.RIGHT
            verticalAlignment = JLabel.CENTER

            setBorder(border)
        }

        override fun render(table: LogTable, renderer: StringRenderer, row: Int, content: String) {
            super.render(table, renderer, row, content)
            border.color = logMetadata.colorScheme.indexColumnSeparatorColor
            renderer.foreground(logMetadata.colorScheme.indexColumnForeground)
        }
    }

}

private abstract class LabelLogTableCellRenderer(protected val logMetadata: LogMetadata) : DefaultTableCellRenderer(), LogCellRenderer {
    private val emptyBorder = BorderFactory.createEmptyBorder(0, 5, 0, 0)
    override var maxLength: Int = Int.MAX_VALUE
    override lateinit var logTable: LogTable

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        col: Int
    ): Component {
        border = emptyBorder
        font = table.font
        val content = value?.toString() ?: ""
        logTable = table as LogTable
        RenderResult.obtain().with { result ->
            result.raw = processContent(logTable, content)
            getRenderResult(result, logTable, row)
            if (result.background.isValid()) {
                background = result.background
            }
            if (result.foreground.isValid()) {
                foreground = result.foreground
            }
            text = result.rendered
        }
        return this
    }

    private fun getRenderResult(result: RenderResult, logTable: LogTable, row: Int) {
        LabelRenderer.obtain().with { renderer ->
            renderer.updateRaw(result.raw)
            val logItem = logTable.tableModel.getItemInCurrentPage(row)
            val logBackground = logTable.getColumnBackground(logItem.num, row, logMetadata)
            renderer.highlight(logBackground)
            render(logTable, renderer, row, result.raw)
            renderer.processRenderResult(result)
        }
    }

    open fun render(table: LogTable, renderer: StringRenderer, row: Int, content: String) {
        // Empty Implementation
    }

    open fun processContent(table: LogTable, content: String): String {
        return content
    }
}