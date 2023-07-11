package me.gegenbauer.catspy.log.ui.table

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.ui.LogMainUI
import me.gegenbauer.catspy.log.ui.dialog.LogViewDialog
import me.gegenbauer.catspy.log.ui.table.LogTableModel.Companion.COLUMN_NUM
import me.gegenbauer.catspy.utils.findFrameFromParent
import me.gegenbauer.catspy.utils.isDoubleClick
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.*
import javax.swing.*


class LogTable(
    val tableModel: LogTableModel,
    override val contexts: Contexts = Contexts.default
) : JTable(tableModel), Context {

    init {
        setShowGrid(false)
        tableHeader = null
        autoResizeMode = AUTO_RESIZE_OFF
        autoscrolls = false
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

    override fun changeSelection(rowIndex: Int, columnIndex: Int, toggle: Boolean, extend: Boolean) {
        super.changeSelection(rowIndex, columnIndex, toggle, extend)
        scrollColumnToVisible(rowIndex, columnIndex);
    }

    private fun scrollColumnToVisible(rowIndex: Int, columnIndex: Int) {
        val cellRect: Rectangle = getCellRect(rowIndex, columnIndex, false)
        val leftX = cellRect.x
        val rightX = cellRect.x + cellRect.width

        //assuming we're in scroll pane
        val width = width.coerceAtMost(parent.width)
        val scrolledX = -x
        var visibleLeft = scrolledX
        var visibleRight = visibleLeft + width

        //bonus, scroll if only a little of a column is visible
        visibleLeft += THRESHOLD
        visibleRight -= THRESHOLD
        val isCellVisible = (leftX < visibleRight // otherwise cell is hidden on the right
                && rightX > visibleLeft // otherwise cell is hidden on the left
                )
        if (!isCellVisible) {
            scrollRectToVisible(cellRect)
        }
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        tableModel.configureContext(context)
    }

    private fun updateRowHeight() {
        setRowHeight(getFontMetrics(font).height)
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
        val frame = findFrameFromParent<JFrame>()
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

    private fun updateBookmark(rows: List<Int>) {
        val context = contexts.getContext(LogMainUI::class.java) ?: return
        val bookmarkManager = ServiceManager.getContextService(context, BookmarkManager::class.java)
        if (bookmarkManager.checkNewRow(rows)) {
            rows.forEach(bookmarkManager::addBookmark)
        } else {
            rows.forEach(bookmarkManager::updateBookmark)
        }
    }

    private fun deleteBookmark(rows: List<Int>) {
        val context = contexts.getContext(LogMainUI::class.java) ?: return
        val bookmarkManager = ServiceManager.getContextService(context, BookmarkManager::class.java)
        rows.forEach(bookmarkManager::removeBookmark)
    }

    internal inner class PopUp : JPopupMenu() {
        var copyItem: JMenuItem = JMenuItem("Copy")
        var showEntireItem = JMenuItem("Show entire line")
        var bookmarkItem = JMenuItem("Bookmark")
        var startItem = JMenuItem("Start")
        var stopItem = JMenuItem("Stop")
        var clearItem = JMenuItem("Clear")

        private val actionHandler = ActionHandler()

        init {
            add(copyItem)
            add(showEntireItem)
            add(bookmarkItem)
            addSeparator()
            add(startItem)
            add(stopItem)
            add(clearItem)
            copyItem.addActionListener(actionHandler)
            showEntireItem.addActionListener(actionHandler)
            bookmarkItem.addActionListener(actionHandler)
            startItem.addActionListener(actionHandler)
            stopItem.addActionListener(actionHandler)
            clearItem.addActionListener(actionHandler)
        }

        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                val logMainUI = DarkUIUtil.getParentOfType(this@LogTable, LogMainUI::class.java)
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
                        updateBookmark(selectedNums())
                    }

                    startItem -> {
                        logMainUI.startAdbScan()
                    }

                    stopItem -> {
                        logMainUI.stopScan()
                    }

                    clearItem -> {
                        logMainUI.clearAdbLog()
                    }
                }
            }
        }
    }

    internal inner class MouseHandler : MouseAdapter() {
        private val popupMenu: JPopupMenu = PopUp()

        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                popupMenu.show(event.component, event.x, event.y)
                SwingUtilities.updateComponentTreeUI(popupMenu)
            } else {
                popupMenu.isVisible = false
            }

            super.mouseReleased(event)
        }

        override fun mouseClicked(event: MouseEvent) {
            if (SwingUtilities.isLeftMouseButton(event) && event.isDoubleClick) {
                // 双击第一列更新书签，双击第二列显示详细日志
                if (columnAtPoint(event.point) == COLUMN_NUM) {
                    updateBookmark(selectedNums())
                } else {
                    showSelected(selectedRows)
                }
            }
            super.mouseClicked(event)
        }
    }

    private fun selectedNums(): List<Int> {
        val selectedRows = selectedRows
        return if (selectedRows.isEmpty()) {
            emptyList()
        } else {
            selectedRows.map { tableModel.getItem(it).num }
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
                    selectedNums()
                )

                event.keyCode == KeyEvent.VK_PAGE_DOWN -> downPage()
                event.keyCode == KeyEvent.VK_PAGE_UP -> upPage()
                event.keyCode == KeyEvent.VK_DOWN -> downLine()
                event.keyCode == KeyEvent.VK_UP -> upLine()
                event.keyCode == KeyEvent.VK_ENTER -> showSelected(selectedRows)
                event.keyCode == KeyEvent.VK_DELETE -> deleteBookmark(selectedNums())
            }
            super.keyPressed(event)
        }
    }

    companion object {
        private const val TAG = "LogTable"
        private const val THRESHOLD = 10
    }
}
