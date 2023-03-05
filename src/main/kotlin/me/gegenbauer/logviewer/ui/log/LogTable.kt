package me.gegenbauer.logviewer.ui.log

import me.gegenbauer.logviewer.manager.BookmarkManager
import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.VStatusPanel
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.table.DefaultTableCellRenderer


class LogTable(var tableModel: LogTableModel) : JTable(tableModel){
    private val tableColor: ColorManager.TableColor
    private val bookmarkManager = BookmarkManager.getInstance()

    companion object {
        const val VIEW_LINE_ONE = 0
        const val VIEW_LINE_WRAP = 1

        const val COLUMN_0_WIDTH = 80
    }

    init {
        setShowGrid(false)
        tableHeader = null
        autoResizeMode = AUTO_RESIZE_OFF
        autoscrolls = false
        dragEnabled = true
        dropMode = DropMode.INSERT

        val columnNum = columnModel.getColumn(0)
//        columnNum.preferredWidth = COLUMN_0_WIDTH
        columnNum.cellRenderer = NumCellRenderer()

        val columnLog = columnModel.getColumn(1)
//        columnLog.preferredWidth = gd.displayMode.width - COLUMN_0_WIDTH - 25
        columnLog.cellRenderer = LogCellRenderer()
        intercellSpacing = Dimension(0, 0)

        val enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, "none")

        addMouseListener(MouseHandler())
        addKeyListener(TableKeyHandler())

        tableColor = if (this.tableModel.isFullDataModel()) {
            ColorManager.getInstance().fullTableColor
        }
        else {
            ColorManager.getInstance().filterTableColor
        }
    }

    fun updateColumnWidth(width: Int, scrollVBarWidth: Int) {
        if (rowCount <= 0) {
            return
        }

        val fontMetrics = getFontMetrics(font)
        val value = this.tableModel.getValueAt(rowCount - 1, 0)
        val column0Width = fontMetrics.stringWidth(value.toString()) + 20
        var newWidth = width
        if (width < 1920) {
            newWidth = 1920
        }
        val preferredLogWidth = newWidth - column0Width - VStatusPanel.VIEW_RECT_WIDTH - scrollVBarWidth - 2

        val columnNum = columnModel.getColumn(0)
        val columnLog = columnModel.getColumn(1)
        if (columnNum.preferredWidth != column0Width) {
            columnNum.preferredWidth = column0Width
            columnLog.preferredWidth = preferredLogWidth
        }
        else {
            if (columnLog.preferredWidth != preferredLogWidth) {
                columnLog.preferredWidth = preferredLogWidth
            }
        }
    }

    var scanMode = false
        set(value) {
            field = value
            val columnLog = columnModel.getColumn(1)
//            if (value == true) {
                columnLog.cellRenderer = LogCellRenderer()
//            }
//            else {
//                if (viewMode == VIEW_LINE_ONE) {
//                    columnLog.cellRenderer = LogCellRenderer()
//                }
//                else if (viewMode == VIEW_LINE_WRAP) {
//                    columnLog.cellRenderer = LogWrapCellRenderer()
//                }
//            }
        }

    var viewMode = VIEW_LINE_ONE
        set(value) {
            field = value
            val columnLog = columnModel.getColumn(1)
//            if (value == VIEW_LINE_ONE) {
                columnLog.cellRenderer = LogCellRenderer()
//            }
//            else if (value == VIEW_LINE_WRAP) {
//                columnLog.cellRenderer = LogWrapCellRenderer()
//            }
        }

    internal class LineNumBorder(val color: Color, private val thickness: Int) : AbstractBorder() {
        override fun paintBorder(
            c: Component?, g: Graphics?, x: Int, y: Int, width: Int, height: Int
        ) {
            if (width > 0) {
                g?.color = color
                for (i in 1..thickness) {
                    g?.drawLine(width - i , y, width - i, height)
                }
            }
        }

        override fun getBorderInsets(c: Component): Insets {
            return getBorderInsets(c, Insets(0, 0, 0, thickness))
        }

        override fun getBorderInsets(c: Component?, insets: Insets): Insets {
            insets.top = 0
            insets.left = 0
            insets.bottom = 0
            insets.right = thickness
            return insets
        }

        override fun isBorderOpaque(): Boolean {
            return true
        }
    }
    internal inner class NumCellRenderer : DefaultTableCellRenderer() {
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

//            println("NumCellRenderer getTableCellRendererComponent $isSelected, $hasFocus, $row, $col, ${isRowSelected(row)}")
            val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col) as JLabel

            label.border = LineNumBorder(tableColor.numLogSeparatorBG, 1)

            foreground = tableColor.lineNumFG
            background = if (bookmarkManager.bookmarks.contains(num)) {
                if (isRowSelected(row)) {
                    tableColor.numBookmarkSelectedBG
                }
                else {
                    tableColor.numBookmarkBG
                }
            } else if (isRowSelected(row)) {
                tableColor.numSelectedBG
            } else {
                tableColor.lineNumBG
            }

            return label
        }
    }

    internal inner class LogCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            col: Int
        ): Component {
//            println("LogCellRenderer getTableCellRendererComponent $isSelected, $hasFocus, $row, $col, ${isRowSelected(row)}")

            val newValue:String = if (value != null) {
                this@LogTable.tableModel.getPrintValue(value.toString(), row, isSelected)
            } else {
                ""
            }
            val label:JLabel
            if (newValue.isEmpty()) {
                label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col) as JLabel
                foreground = this@LogTable.tableModel.getFgColor(row)
            }
            else {
                label = super.getTableCellRendererComponent(table, newValue, isSelected, hasFocus, row, col) as JLabel
            }

            label.border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
            val numValue = this@LogTable.tableModel.getValueAt(row, 0)
            val num = numValue.toString().trim().toInt()
            if (bookmarkManager.bookmarks.contains(num)) {
                if (isRowSelected(row)) {
                    background = tableColor.bookmarkSelectedBG
                }
                else {
                    background = tableColor.bookmarkBG
                }
            } else if (isRowSelected(row)) {
                background = tableColor.selectedBG
            } else {
                background = tableColor.logBG
            }

            return label

        }
    }

//    internal inner class LogWrapCellRenderer : JTextArea(), TableCellRenderer {
//        init {
//            lineWrap = true
//        }
//
//        override fun getTableCellRendererComponent(
//            table: JTable?,
//            value: Any?,
//            isSelected: Boolean,
//            hasFocus: Boolean,
//            row: Int,
//            col: Int
//        ): Component {
//
//            if (table == null) {
//                return this
//            } else {
//                text = value.toString()
//                val logTableModel = tableModel as LogTableModel
//                foreground = logTableModel.getFgColor(row)
//                setSize(table.columnModel.getColumn(col).width, preferredSize.height)
//                if (table.getRowHeight(row) != preferredSize.height) {
//                    table.setRowHeight(row, preferredSize.height)
//                }
//
//                var isSelectedChecked = isSelected
//                val dropLocation = table.getDropLocation()
//                if (dropLocation != null && !dropLocation.isInsertRow() && !dropLocation.isInsertColumn() && dropLocation.getRow() == row && dropLocation.getColumn() == col) {
//                    isSelectedChecked = true
//                }
//
//                background = Color(0xFF, 0xFF, 0xFF)
//                if (isSelectedChecked || row == table.selectedRow) {
//                    background = Color(0xC0, 0xC0, 0xC0)
//                }
//
//                return this
//            }
//        }
//    }

    fun downPage() {
        val toRect = visibleRect
        toRect.y = (selectedRow + 3) * rowHeight
        scrollRectToVisible(toRect)

        return
    }

    fun upPage() {
        val toRect = visibleRect
        toRect.y = (selectedRow - 3) * rowHeight - toRect.height
        scrollRectToVisible(toRect)

        return
    }

    fun downLine() {
        val toRect = visibleRect
        val rowY = selectedRow * rowHeight

        if (visibleRect.y + visibleRect.height - 4 * rowHeight < rowY) {
            toRect.y += rowHeight
        }
        scrollRectToVisible(toRect)

        return
    }

    fun upLine() {
        val toRect = visibleRect
        val rowY = selectedRow * rowHeight

        if (visibleRect.y + 3 * rowHeight > rowY) {
            toRect.y -= rowHeight
        }
        scrollRectToVisible(toRect)

        return
    }

    private fun showSelected(targetRow:Int) {
        val log = StringBuilder("")
        var caretPos = 0
        var value:String

        if (selectedRowCount > 1) {
            for (row in selectedRows) {
                value = this.tableModel.getValueAt(row, 1).toString() + "\n"
                log.append(value)
            }
        }
        else {
            var startIdx = targetRow - 2
            if (startIdx < 0) {
                startIdx = 0
            }
            var endIdx = targetRow + 3
            if (endIdx > rowCount) {
                endIdx = rowCount
            }

            for (idx in startIdx until endIdx) {
                if (idx == targetRow) {
                    caretPos = log.length
                }
                value = this.tableModel.getValueAt(idx, 1).toString() + "\n"
                log.append(value)
            }
        }

        val frame = SwingUtilities.windowForComponent(this@LogTable) as JFrame
        val logViewDialog = LogViewDialog(frame, log.toString().trim(), caretPos)
        logViewDialog.setLocationRelativeTo(frame)
        logViewDialog.isVisible = true
    }

    private fun updateBookmark(targetRow:Int) {
        if (selectedRowCount > 1) {
            var isAdd = false
            for (row in selectedRows) {
                val value = this.tableModel.getValueAt(row, 0)
                val bookmark = value.toString().trim().toInt()

                if (!bookmarkManager.isBookmark(bookmark)) {
                    isAdd = true
                    break
                }
            }

            for (row in selectedRows) {
                val value = this.tableModel.getValueAt(row, 0)
                val bookmark = value.toString().trim().toInt()

                if (isAdd) {
                    if (!bookmarkManager.isBookmark(bookmark)) {
                        bookmarkManager.addBookmark(bookmark)
                    }
                } else {
                    if (bookmarkManager.isBookmark(bookmark)) {
                        bookmarkManager.removeBookmark(bookmark)
                    }
                }
            }
        }
        else {
            val value = this.tableModel.getValueAt(targetRow, 0)
            val bookmark = value.toString().trim().toInt()
            bookmarkManager.updateBookmark(bookmark)
        }
    }

    internal inner class PopUpTable : JPopupMenu() {
        var copyItem: JMenuItem = JMenuItem("Copy")
        var showEntireItem = JMenuItem("Show entire line")
        var bookmarkItem = JMenuItem("Bookmark")
        var reconnectItem = JMenuItem("Reconnect adb")
        var startItem = JMenuItem("Start")
        var stopItem = JMenuItem("Stop")
        var clearItem = JMenuItem("Clear")
//        var clearSaveItem = JMenuItem("Clear/Save")
        private val actionHandler = ActionHandler()

        init {
            copyItem.addActionListener(actionHandler)
            add(copyItem)
            showEntireItem.addActionListener(actionHandler)
            add(showEntireItem)
            bookmarkItem.addActionListener(actionHandler)
            add(bookmarkItem)
            addSeparator()
            reconnectItem.addActionListener(actionHandler)
            add(reconnectItem)
            startItem.addActionListener(actionHandler)
            add(startItem)
            stopItem.addActionListener(actionHandler)
            add(stopItem)
            clearItem.addActionListener(actionHandler)
            add(clearItem)
//            clearSaveItem.addActionListener(mActionHandler)
//            add(clearSaveItem)
        }
        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(p0: ActionEvent?) {
                when (p0?.source) {
                    copyItem -> {
                        this@LogTable.processKeyEvent(KeyEvent(this@LogTable, KeyEvent.KEY_PRESSED, p0.`when`, KeyEvent.CTRL_MASK, KeyEvent.VK_C, 'C'))
                    }
                    showEntireItem -> {
                        showSelected(selectedRow)
                    }
                    bookmarkItem -> {
                        updateBookmark(selectedRow)
                    }
                    reconnectItem -> {
                        val frame = SwingUtilities.windowForComponent(this@LogTable) as MainUI
                        frame.reconnectAdb()
                    }
                    startItem -> {
                        val frame = SwingUtilities.windowForComponent(this@LogTable) as MainUI
                        frame.startAdbLog()
                    }
                    stopItem -> {
                        val frame = SwingUtilities.windowForComponent(this@LogTable) as MainUI
                        frame.stopAdbLog()
                    }
                    clearItem -> {
                        val frame = SwingUtilities.windowForComponent(this@LogTable) as MainUI
                        frame.clearAdbLog()
                    }
//                    clearSaveItem -> {
//                        val frame = SwingUtilities.windowForComponent(this@LogTable) as MainUI
//                        frame.clearSaveAdbLog()
//                    }
                }
            }
        }
    }

    internal inner class MouseHandler : MouseAdapter() {
        var firstClickRow = 0
        var secondClickRow = 0

        override fun mousePressed(p0: MouseEvent?) {
            super.mousePressed(p0)
        }

        private var popupMenu: JPopupMenu? = null
        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                popupMenu = PopUpTable()
                popupMenu?.show(event.component, event.x, event.y)
            }
            else {
                popupMenu?.isVisible = false
            }

            super.mouseReleased(event)
        }

        override fun mouseClicked(p0: MouseEvent?) {
            if (SwingUtilities.isLeftMouseButton(p0)) {
                if (p0?.clickCount == 2) {
                    secondClickRow = selectedRow
                    val targetRow = if (firstClickRow > secondClickRow) {
                        firstClickRow
                    } else {
                        secondClickRow
                    }
                    if (columnAtPoint(p0.point) == 0) {
                        updateBookmark(targetRow)
                    } else {
                        showSelected(targetRow)
                    }
                }
                if (p0?.clickCount == 1) {
                    firstClickRow = selectedRow
                }
            }

            super.mouseClicked(p0)
        }
    }
    internal inner class TableKeyHandler : KeyAdapter() {
//        override fun keyReleased(p0: KeyEvent?) {
//            if (KeyEvent.VK_ENTER == p0?.keyCode) {
//            }
//            super.keyReleased(p0)
//        }

        override fun keyPressed(p0: KeyEvent?) {
            if (p0?.keyCode == KeyEvent.VK_B && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {
                updateBookmark(selectedRow)
            } else if (p0?.keyCode == KeyEvent.VK_PAGE_DOWN) {
                downPage()
            } else if (p0?.keyCode == KeyEvent.VK_PAGE_UP) {
                upPage()
            } else if (p0?.keyCode == KeyEvent.VK_DOWN) {
                downLine()
            } else if (p0?.keyCode == KeyEvent.VK_UP) {
                upLine()
            } else if (p0?.keyCode == KeyEvent.VK_ENTER) {
                showSelected(selectedRow)
            }
            super.keyPressed(p0)
        }
    }
}
