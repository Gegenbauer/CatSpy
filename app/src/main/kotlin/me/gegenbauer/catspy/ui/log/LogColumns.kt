package me.gegenbauer.catspy.ui.log

import me.gegenbauer.catspy.data.model.log.LogcatLogItem.Companion.fgColor
import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.render.html.HtmlStringRender
import me.gegenbauer.catspy.ui.ColorScheme
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Insets
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.border.AbstractBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableColumn


private val columnIndex = object : Column {
    override val name: String = "index"
    override val maxCharCount: Int = 7
    override val index: Int = 0

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : DefaultTableCellRenderer() {
            init {
                horizontalAlignment = JLabel.RIGHT
                verticalAlignment = JLabel.CENTER
            }

            override fun getTableCellRendererComponent(
                table: JTable?,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                col: Int
            ): Component {
                var num = -1
                if (value != null) {
                    num = value.toString().trim().toInt()
                }
                val logTable = table as? LogTable
                val label =
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col) as JLabel
                label.border = LineNumBorder(ColorScheme.numLogSeparatorBG, 1)

                foreground = ColorScheme.lineNumFG
                background = logTable?.getColumnBackground(num, row)

                return label
            }
        }
    }
}

private val columnTime = object : Column {
    override val name: String = "time"
    override val maxCharCount: Int = 14
    override val index: Int = 1

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return SimpleLogCellRender()
    }
}

private val columnPid = object : Column {
    override val name: String = "pid"
    override val maxCharCount: Int = 6
    override val index: Int = 2

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : SimpleLogCellRender() {
            override fun addRenderItem(logTable: LogTable, row: Int, render: HtmlStringRender) {
                if (logTable.tableModel.boldPid) {
                    render.clear(HtmlStringRender.SpanType.FOREGROUND)
                    render.foreground(0, render.raw.length - 1, ColorScheme.pidFG)
                    render.bold(0, render.raw.length - 1)
                }
            }
        }
    }
}

private val columnTid = object : Column {
    override val name: String = "tid"
    override val maxCharCount: Int = 6
    override val index: Int = 3

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : SimpleLogCellRender() {
            override fun addRenderItem(logTable: LogTable, row: Int, render: HtmlStringRender) {
                if (logTable.tableModel.boldTid) {
                    render.clear(HtmlStringRender.SpanType.FOREGROUND)
                    render.foreground(0, render.raw.length - 1, ColorScheme.tidFG)
                    render.bold(0, render.raw.length - 1)
                }
            }
        }
    }
}

private val columnLevel = object : Column {
    override val name: String = "level"
    override val maxCharCount: Int = 3
    override val index: Int = 4

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return SimpleLogCellRender()
    }
}

private val columnTag = object : Column {
    override val name: String = "tag"
    override val maxCharCount: Int = 20
    override val index: Int = 5

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : SimpleLogCellRender() {
            override fun addRenderItem(logTable: LogTable, row: Int, render: HtmlStringRender) {
                if (logTable.tableModel.boldTag) {
                    render.clear(HtmlStringRender.SpanType.FOREGROUND)
                    render.foreground(0, render.raw.length - 1, ColorScheme.tagFG)
                    render.bold(0, render.raw.length - 1)
                }
                logTable.tableModel.getLogFilter().filterTag.positiveFilter.matcher(render.raw).let {
                    while (it.find()) {
                        render.highlight(it.start(), it.end(), ColorScheme.filteredBGs[0])
                        render.foreground(it.start(), it.end(), ColorScheme.filteredFGs[0])
                    }
                }
            }
        }
    }
}

private val columnMessage = object : Column {
    override val name: String = "message"
    override val maxCharCount: Int = 250
    override val index: Int = 6

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return SimpleLogCellRender()
    }
}

internal interface Column {
    val name: String
    val maxCharCount: Int
    val index: Int

    fun getCellRenderer(): DefaultTableCellRenderer

    fun getTemplateByCount(count: Int): String {
        return (0 until count).joinToString()
    }

    fun configureColumn(table: JTable) {
        val tableColumn = if (table.columnCount <= index) {
            TableColumn(index).apply {
                table.addColumn(this)
            }
        } else {
            table.columnModel.getColumn(index)
        }
        tableColumn.cellRenderer = getCellRenderer()
        val fontMetrics = table.getFontMetrics(table.font)
        val width: Int = fontMetrics.charWidth('A') * maxCharCount + 2
        tableColumn.maxWidth = width
        tableColumn.width = width
        tableColumn.preferredWidth = width
    }
}

private open class SimpleLogCellRender : DefaultTableCellRenderer() {
    init {
        horizontalAlignment = JLabel.LEFT
        verticalAlignment = JLabel.CENTER
    }

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        col: Int
    ): Component {
        val logTable = table as LogTable
        val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col) as JLabel
        val content = value as? String ?: ""
        background = logTable.getColumnBackground(col, row)
        label.text = getRenderedContent(logTable, row, content)
        return label
    }

    private fun getRenderedContent(logTable: LogTable, row: Int, content: String): String {
        val render = HtmlStringRender(content)
        val logItem = logTable.tableModel.getItem(row)
        val foreground = logItem.fgColor
        render.foreground(0, content.length - 1, foreground)
        logTable.tableModel.searchFilterItem.positiveFilter.matcher(content).let {
            while (it.find()) {
                render.highlight(it.start(), it.end(), ColorScheme.searchBG)
                render.foreground(it.start(), it.end(), ColorScheme.searchFG)
            }
        }
        logTable.tableModel.highlightFilterItem.positiveFilter.matcher(content).let {
            while (it.find()) {
                render.highlight(it.start(), it.end(), ColorScheme.highlightBG)
                render.foreground(it.start(), it.end(), ColorScheme.highlightFG)
            }
        }
        logTable.tableModel.getLogFilter().filterLog.positiveFilter.matcher(content).let {
            while (it.find()) {
                render.highlight(it.start(), it.end(), ColorScheme.filteredBGs[0])
                render.foreground(it.start(), it.end(), ColorScheme.filteredFGs[0])
            }
        }
        addRenderItem(logTable, row, render)
        return render.render()
    }

    open fun addRenderItem(logTable: LogTable, row: Int, render: HtmlStringRender) {}
}

private class LineNumBorder(val color: Color, private val thickness: Int) : AbstractBorder() {
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        if (width > 0) {
            g.color = color
            for (i in 1..thickness) {
                g.drawLine(width - i, y, width - i, height)
            }
        }
    }

    override fun getBorderInsets(c: Component): Insets {
        return getBorderInsets(c, Insets(0, 0, 0, thickness))
    }

    override fun getBorderInsets(c: Component?, insets: Insets): Insets {
        return insets.apply { set(0, 0, 0, thickness) }
    }

    override fun isBorderOpaque(): Boolean {
        return true
    }
}

private fun LogTable.getColumnBackground(num: Int, row: Int): Color {
    return if (BookmarkManager.isBookmark(num)) {
        if (isRowSelected(row)) {
            ColorScheme.bookmarkSelectedBG
        } else {
            ColorScheme.bookmarkBG
        }
    } else if (isRowSelected(row)) {
        ColorScheme.selectedBG
    } else {
        ColorScheme.logBG
    }
}

internal val columns =
    listOf(columnIndex, columnTime, columnPid, columnTid, columnLevel, columnTag, columnMessage)
