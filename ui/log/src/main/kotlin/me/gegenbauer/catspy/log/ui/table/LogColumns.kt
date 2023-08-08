package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.common.log.FilterItem.Companion.getMatchedList
import me.gegenbauer.catspy.common.support.ColorScheme
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.model.LogcatLogItem.Companion.fgColor
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.render.html.HtmlStringRenderer
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.border.AbstractBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableColumn


internal val columnIndex = object : Column {
    override val name: String = "index"
    override val maxCharCount: Int = 7
    override val index: Int = 0

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : DefaultLogTableCellRenderer() {
            init {
                horizontalAlignment = JLabel.RIGHT
                verticalAlignment = JLabel.CENTER
            }

            override fun render(table: LogTable, label: JLabel, row: Int, col: Int, content: String) {
                label.border = LineNumBorder(ColorScheme.numLogSeparatorBG, 1)
                foreground = ColorScheme.lineNumFG
            }
        }
    }
}

private val columnTime = object : Column {
    override val name: String = "time"
    override val maxCharCount: Int = 15
    override val index: Int = 1

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return SimpleLogCellRenderer()
    }
}

private val columnPid = object : Column {
    override val name: String = "pid"
    override val maxCharCount: Int = 7
    override val index: Int = 2

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : BoldLogCellRenderer() {
            override fun shouldBold(table: LogTable): Boolean {
                return table.tableModel.boldPid
            }

            override fun getBoldColor(): Color {
                return ColorScheme.pidFG
            }
        }
    }
}

private val columnTid = object : Column {
    override val name: String = "tid"
    override val maxCharCount: Int = 7
    override val index: Int = 3

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : BoldLogCellRenderer() {
            override fun shouldBold(table: LogTable): Boolean {
                return table.tableModel.boldTid
            }

            override fun getBoldColor(): Color {
                return ColorScheme.tidFG
            }
        }
    }
}

private val columnLevel = object : Column {
    override val name: String = "level"
    override val maxCharCount: Int = 4
    override val index: Int = 4

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return SimpleLogCellRenderer()
    }
}

private val columnTag = object : Column {
    override val name: String = "tag"
    override val maxCharCount: Int = 21
    override val index: Int = 5

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return object : BoldLogCellRenderer() {
            override fun shouldBold(table: LogTable): Boolean {
                return table.tableModel.boldTag
            }

            override fun getBoldColor(): Color {
                return ColorScheme.tagFG
            }
        }
    }
}

private val columnMessage = object : Column {
    override val name: String = "message"
    override val maxCharCount: Int = 250
    override val index: Int = 6

    override fun getCellRenderer(): DefaultTableCellRenderer {
        return MessageLogCellRenderer()
    }
}

internal interface Column {
    val name: String
    val maxCharCount: Int
    val index: Int

    fun getCellRenderer(): DefaultTableCellRenderer

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

private class SimpleLogCellRenderer : DefaultLogTableCellRenderer() {
    init {
        horizontalAlignment = JLabel.LEFT
        verticalAlignment = JLabel.CENTER
    }

    override fun render(table: LogTable, label: JLabel, row: Int, col: Int, content: String) {
        foreground = table.tableModel.getItemInCurrentPage(row).fgColor
    }
}

private abstract class BoldLogCellRenderer : DefaultLogTableCellRenderer() {

    init {
        horizontalAlignment = JLabel.LEFT
        verticalAlignment = JLabel.CENTER
    }

    override fun render(table: LogTable, label: JLabel, row: Int, col: Int, content: String) {
        foreground = table.tableModel.getItemInCurrentPage(row).fgColor
        if (shouldBold(table)) {
            val renderer = HtmlStringRenderer(content)
            renderer.foreground(0, renderer.raw.length - 1, getBoldColor())
            renderer.bold(0, renderer.raw.length - 1)
            label.text = renderer.render()
        }
    }

    abstract fun shouldBold(table: LogTable): Boolean

    abstract fun getBoldColor(): Color
}

private open class MessageLogCellRenderer : DefaultLogTableCellRenderer() {
    init {
        horizontalAlignment = JLabel.LEFT
        verticalAlignment = JLabel.CENTER
    }

    override fun render(table: LogTable, label: JLabel, row: Int, col: Int, content: String) {
        label.text = getRenderedContent(table, row, content)
    }

    protected open fun getRenderedContent(logTable: LogTable, row: Int, content: String): String {
        val renderer = HtmlStringRenderer(content)
        val logItem = logTable.tableModel.getItemInCurrentPage(row)
        val foreground = logItem.fgColor
        renderer.foreground(0, content.length - 1, foreground)
        logTable.tableModel.searchFilterItem.getMatchedList(content).forEach {
            renderer.highlight(it.first, it.second, ColorScheme.searchBG)
            renderer.foreground(it.first, it.second, ColorScheme.searchFG)
        }
        logTable.tableModel.highlightFilterItem.getMatchedList(content).forEach {
            renderer.highlight(it.first, it.second, ColorScheme.highlightBG)
            renderer.foreground(it.first, it.second, ColorScheme.highlightFG)
        }
        logTable.tableModel.getLogFilter().filterLog.getMatchedList(content).forEach {
            renderer.highlight(it.first, it.second, ColorScheme.filteredBGs[0])
            renderer.foreground(it.first, it.second, ColorScheme.filteredFGs[0])
        }
        addRenderItem(logTable, row, renderer)
        return renderer.render()
    }

    open fun addRenderItem(logTable: LogTable, row: Int, renderer: HtmlStringRenderer) {
        // Empty Implementation
    }
}

private abstract class DefaultLogTableCellRenderer : DefaultTableCellRenderer() {
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
        label.border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
        val content = value as? String ?: ""
        val logItem = logTable.tableModel.getItemInCurrentPage(row)
        background = logTable.getColumnBackground(logItem.num, row)
        render(table, label, row, col, content)
        return label
    }

    open fun render(table: LogTable, label: JLabel, row: Int, col: Int, content: String) {
        // Empty Implementation
    }
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
    val context = contexts.getContext(LogTabPanel::class.java) ?: return ColorScheme.logBG
    val bookmarkManager = ServiceManager.getContextService(context, BookmarkManager::class.java)
    return if (bookmarkManager.isBookmark(num)) {
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
