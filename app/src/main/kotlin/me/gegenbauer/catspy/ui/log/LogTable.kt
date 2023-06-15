package me.gegenbauer.catspy.ui.log

import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.dialog.LogViewDialog
import me.gegenbauer.catspy.ui.log.LogTableModel.Companion.COLUMN_NUM
import me.gegenbauer.catspy.ui.panel.VStatusPanel
import me.gegenbauer.catspy.utils.findFrameFromParent
import me.gegenbauer.catspy.utils.isDoubleClick
import java.awt.Dimension
import java.awt.event.*
import javax.swing.*

class LogTable(val tableModel: LogTableModel) : JTable(tableModel) {

    init {
        setShowGrid(false)
        tableHeader = null
        autoResizeMode = AUTO_RESIZE_OFF
        autoscrolls = true
        dragEnabled = true
        dropMode = DropMode.INSERT
        intercellSpacing = Dimension(0, 0)

        columns.forEach { it.configureColumn(this) }

        updateRowHeight()

        val enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, "none")

        addMouseListener(MouseHandler())
        addKeyListener(TableKeyHandler())
    }

    private fun updateRowHeight() {
        setRowHeight(getFontMetrics(font).height)
    }

    // TODO use this to update column width
    fun updateColumnWidth(width: Int, scrollVBarWidth: Int) {
        if (rowCount <= 0) {
            return
        }

        val fontMetrics = getFontMetrics(font)
        val value = this.tableModel.getValueAt(rowCount - 1, 0)
        val column0Width = fontMetrics.stringWidth(value.toString()) + 20
        val newWidth = width.coerceAtLeast(2600)
        val preferredLogWidth = newWidth - column0Width - VStatusPanel.VIEW_RECT_WIDTH - scrollVBarWidth - 2

        val columnNum = columnModel.getColumn(COLUMN_NUM)
        val columnLog = columnModel.getColumn(COLUMN_NUM)
        if (columnNum.preferredWidth != column0Width) {
            columnNum.preferredWidth = column0Width
            columnLog.preferredWidth = preferredLogWidth
        } else {
            if (columnLog.preferredWidth != preferredLogWidth) {
                columnLog.preferredWidth = preferredLogWidth
            }
        }
    }

    private fun downPage() {
        val toRect = visibleRect
        toRect.y = (selectedRow + 3) * rowHeight
        scrollRectToVisible(toRect)
        return
    }

    private fun upPage() {
        val toRect = visibleRect
        toRect.y = (selectedRow - 3) * rowHeight - toRect.height
        scrollRectToVisible(toRect)
        return
    }

    private fun downLine() {
        val toRect = visibleRect
        val rowY = selectedRow * rowHeight
        if (visibleRect.y + visibleRect.height - 4 * rowHeight < rowY) {
            toRect.y += rowHeight
        }
        scrollRectToVisible(toRect)
        return
    }

    private fun upLine() {
        val toRect = visibleRect
        val rowY = selectedRow * rowHeight
        if (visibleRect.y + 3 * rowHeight > rowY) {
            toRect.y -= rowHeight
        }
        scrollRectToVisible(toRect)
        return
    }

    private fun showSelected(rows: IntArray) {
        if (rows.isEmpty()) {
            return
        }
        val displayContent = if (rows.size > 1) {
            getRowsContent(rows)
        } else {
            getRowsContent(((rows[0] - 2).coerceAtLeast(0)..(rows[0] + 3).coerceAtMost(rowCount)).toList())
        }
        val caretPos = getRowsContent(arrayListOf(rows[0])).length
        val frame = findFrameFromParent<MainUI>()
        val logViewDialog = LogViewDialog(frame, displayContent.trim(), caretPos)
        logViewDialog.setLocationRelativeTo(frame)
        logViewDialog.isVisible = true
    }

    private fun getRowsContent(rows: List<Int>): String {
        return rows.joinToString("\n") { this.tableModel.getItem(it).logLine }
    }

    //TODO add render effect for this
    private fun getRowsContent(rows: IntArray): String {
        return rows.joinToString("\n") { this.tableModel.getItem(it).logLine }
    }

    private fun updateBookmark(rows: IntArray) {
        if (BookmarkManager.checkNewRow(rows)) {
            rows.forEach(BookmarkManager::addBookmark)
        } else {
            rows.forEach(BookmarkManager::updateBookmark)
        }
    }

    private fun deleteBookmark(rows: IntArray) {
        rows.forEach(BookmarkManager::removeBookmark)
    }

    internal inner class PopUpTable : JPopupMenu() {
        var copyItem: JMenuItem = JMenuItem("Copy")
        var showEntireItem = JMenuItem("Show entire line")
        var bookmarkItem = JMenuItem("Bookmark")
        var reconnectItem = JMenuItem("Reconnect adb")
        var startItem = JMenuItem("Start")
        var stopItem = JMenuItem("Stop")
        var clearItem = JMenuItem("Clear")

        private val actionHandler = ActionHandler()

        init {
            add(copyItem)
            add(showEntireItem)
            add(bookmarkItem)
            addSeparator()
            add(reconnectItem)
            add(startItem)
            add(stopItem)
            add(clearItem)
            copyItem.addActionListener(actionHandler)
            showEntireItem.addActionListener(actionHandler)
            bookmarkItem.addActionListener(actionHandler)
            reconnectItem.addActionListener(actionHandler)
            startItem.addActionListener(actionHandler)
            stopItem.addActionListener(actionHandler)
            clearItem.addActionListener(actionHandler)
        }

        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                val frame = this@LogTable.findFrameFromParent<MainUI>()
                when (event.source) {
                    copyItem -> {
                        this@LogTable.processKeyEvent(
                            KeyEvent(
                                this@LogTable,
                                KeyEvent.KEY_PRESSED,
                                event.`when`,
                                KeyEvent.CTRL_DOWN_MASK,
                                KeyEvent.VK_C,
                                'C'
                            )
                        )
                    }

                    showEntireItem -> {
                        showSelected(selectedRows)
                    }

                    bookmarkItem -> {
                        updateBookmark(selectedRows)
                    }

                    reconnectItem -> {
                        frame.reconnectAdb()
                    }

                    startItem -> {
                        frame.startAdbLog()
                    }

                    stopItem -> {
                        frame.stopAdbLog()
                    }

                    clearItem -> {
                        frame.clearAdbLog()
                    }
                }
            }
        }
    }

    internal inner class MouseHandler : MouseAdapter() {
        private val popupMenu: JPopupMenu = PopUpTable()

        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                popupMenu.show(event.component, event.x, event.y)
            } else {
                popupMenu.isVisible = false
            }

            super.mouseReleased(event)
        }

        override fun mouseClicked(event: MouseEvent) {
            if (SwingUtilities.isLeftMouseButton(event) && event.isDoubleClick) {
                // 双击第一列更新书签，双击第二列显示详细日志
                if (columnAtPoint(event.point) == COLUMN_NUM) {
                    updateBookmark(selectedRows)
                } else {
                    showSelected(selectedRows)
                }
            }
            super.mouseClicked(event)
        }
    }

    override fun updateUI() {
        super.updateUI()
        updateRowHeight()
        if (columns != null) {
            columns.forEach { it.configureColumn(this) }
        }
    }

    internal inner class TableKeyHandler : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            when {
                event.keyCode == KeyEvent.VK_B && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> updateBookmark(
                    selectedRows
                )

                event.keyCode == KeyEvent.VK_PAGE_DOWN -> downPage()
                event.keyCode == KeyEvent.VK_PAGE_UP -> upPage()
                event.keyCode == KeyEvent.VK_DOWN -> downLine()
                event.keyCode == KeyEvent.VK_UP -> upLine()
                event.keyCode == KeyEvent.VK_ENTER -> showSelected(selectedRows)
                event.keyCode == KeyEvent.VK_DELETE -> deleteBookmark(selectedRows)
            }
            super.keyPressed(event)
        }
    }

    companion object {
        private const val TAG = "LogTable"
    }
}
