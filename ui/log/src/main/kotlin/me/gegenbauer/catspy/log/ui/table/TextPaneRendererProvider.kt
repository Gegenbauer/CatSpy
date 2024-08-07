package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.configuration.LogColorScheme
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.log.filter.DefaultLogFilter
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.render.StringRenderer
import me.gegenbauer.catspy.render.TextPaneRenderer
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.getMatchedList
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JTable
import javax.swing.JTextPane
import javax.swing.plaf.ColorUIResource
import javax.swing.table.TableCellRenderer

class TextPaneRendererProvider: BaseLogCellRendererProvider() {
    private val dialogTextComponent = JTextPane()

    override fun createRenderer(column: Column): TableCellRenderer {
        if (column.partIndex < 0) {
            return indexRenderer
        }

        if (column.supportFilter && column.uiConf.column.isHidden.not()) {
            return SearchableCellRenderer(getFilterIndex(column))
        }

        return SimpleLogCellRenderer()
    }

    override fun showSelectedRowsInDialog(
        logTable: LogTable,
        rows: List<Int>,
        popupActions: List<LogDetailDialog.PopupAction>
    ) {
        val renderer = TextPaneRenderer.obtain()
        renderer.setTextPane(dialogTextComponent)
        renderRows(logTable, rows, renderer)
        renderer.render()
        renderer.recycle()
        val frame = logTable.contexts.getContext(JFrame::class.java)!!
        val logViewDialog = LogDetailDialog(frame, dialogTextComponent, popupActions)
        logViewDialog.setLocationRelativeTo(frame)
        logViewDialog.isVisible = true
    }

    private fun renderRows(table: LogTable, rows: List<Int>, renderer: StringRenderer): String {
        val logFilter = table.tableModel.getLogFilter()
        val messageFilter = (logFilter as? DefaultLogFilter)?.filters?.firstOrNull { it.column is Column.MessageColumn }
        val logFilterItem = messageFilter?.filterItem
        val contentBuffer = StringBuilder()
        rows.forEachIndexed { index, row ->
            val logItem = table.tableModel.getItemInCurrentPage(row)
            val content = logItem.logLine
            if (index != rows.lastIndex) {
                contentBuffer.appendLine(content)
            } else {
                contentBuffer.append(content)
            }
        }
        val content = contentBuffer.toString()
        renderer.updateRaw(content)
        var offset = 0
        rows.forEach { row ->
            val logItem = table.tableModel.getItemInCurrentPage(row)
            val filterMatchedList = logFilterItem?.getMatchedList(logItem.logLine) ?: emptyList()
            filterMatchedList.forEach {
                renderer.highlight(offset + it.first, offset + it.second, LogColorScheme.filteredBGs[0])
                renderer.foreground(offset + it.first, offset + it.second, LogColorScheme.filteredFGs[0])
            }
            val foregroundColor = getLevel(logItem).color.color
            renderer.foreground(offset, offset + logItem.logLine.length - 1, foregroundColor)
            offset += logItem.logLine.length + 1
        }
        return content
    }

    private abstract class DefaultLogTableCellRenderer : TextPaneLogTableCellRenderer() {
        private val emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0)

        init {
            border = emptyBorder
        }

        override fun render(table: LogTable, textPane: JTextPane, renderer: StringRenderer, row: Int, content: String) {
            super.render(table, textPane, renderer, row, content)
            val logItem = table.tableModel.getItemInCurrentPage(row)
            updateBackground(table.getColumnBackground(logItem.num, row))
        }
    }

    private inner class SimpleLogCellRenderer : DefaultLogTableCellRenderer() {

        override fun render(table: LogTable, textPane: JTextPane, renderer: StringRenderer, row: Int, content: String) {
            super.render(table, textPane, renderer, row, content)
            renderer.foreground(0, content.length - 1, getLevel(table.tableModel.getItemInCurrentPage(row)).color.color)
        }
    }

    private inner class SearchableCellRenderer(private val filterIndex: Int) : DefaultLogTableCellRenderer() {
        override fun render(table: LogTable, textPane: JTextPane, renderer: StringRenderer, row: Int, content: String) {
            super.render(table, textPane, renderer, row, content)

            val logItem = table.tableModel.getItemInCurrentPage(row)
            val foreground = getLevel(logItem).color.color
            renderer.foreground(0, content.length - 1, foreground)

            table.tableModel.searchFilterItem.getMatchedList(content).forEach {
                renderer.highlight(it.first, it.second, LogColorScheme.searchBG)
                renderer.foreground(it.first, it.second, LogColorScheme.searchFG)
            }

            val logFilter = table.tableModel.getLogFilter()
            if (logFilter !is DefaultLogFilter) return
            // filter 比 render 更新慢时出现异常
            if (filterIndex >= logFilter.filters.size) return
            logFilter.filters[filterIndex].filterItem.getMatchedList(content).forEach {
                renderer.highlight(it.first, it.second, LogColorScheme.filteredBGs[0])
                renderer.foreground(it.first, it.second, LogColorScheme.filteredFGs[0])
            }
        }

        override fun processContent(table: LogTable, content: String): String {
            val logFilter = table.tableModel.getLogFilter()
            if (logFilter !is DefaultLogFilter) return content
            if (filterIndex >= logFilter.filters.size) return content
            return content.take(logFilter.filters[filterIndex].column.uiConf.column.charLen)
        }
    }

    companion object {
        private val indexRenderer = object : DefaultLogTableCellRenderer() {
            private val border = LineNumBorder(LogColorScheme.numLogSeparatorBG, 1)

            override fun render(
                table: LogTable,
                textPane: JTextPane,
                renderer: StringRenderer,
                row: Int,
                content: String
            ) {
                super.render(table, textPane, renderer, row, content)
                border.color = LogColorScheme.numLogSeparatorBG
                renderer.foreground(0, content.length - 1, LogColorScheme.lineNumFG)
            }
        }
    }
}

open class TextPaneLogTableCellRenderer : TableCellRenderer, JTextPane() {

    private val emptyBorder = BorderFactory.createEmptyBorder(0, 5, 0, 0)
    private var background: Color = ColorUIResource(Color.white)

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
        val renderer = TextPaneRenderer.obtain().setTextPane(this).updateRaw(processContent)
        render(logTable, this, renderer, row, processContent)
        renderer.render()
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