package me.gegenbauer.catspy.log.ui.table

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.log.ui.dialog.LogViewDialog
import me.gegenbauer.catspy.log.ui.table.LogTableModel.Companion.COLUMN_NUM
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.*
import me.gegenbauer.catspy.view.table.RowNavigation
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.*
import javax.swing.*
import javax.swing.event.ListSelectionListener

class LogTable(
    val tableModel: LogTableModel,
    override val contexts: Contexts = Contexts.default
) : JTable(tableModel), Context, RowNavigation {

    var listSelectionHandler: ListSelectionListener? = null
        set(value) {
            field = value
            selectionModel.addListSelectionListener(value)
        }

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

        addMouseListener(MouseHandler())
        addKeyListener(TableKeyHandler())
    }

    override fun changeSelection(rowIndex: Int, columnIndex: Int, toggle: Boolean, extend: Boolean) {
        super.changeSelection(rowIndex, columnIndex, toggle, extend)
        scrollColumnToVisible(rowIndex, columnIndex)
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
                && rightX > visibleLeft) // otherwise cell is hidden on the left
        if (!isCellVisible) {
            scrollRectToVisible(cellRect)
        }
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        tableModel.setContexts(contexts)
    }

    override fun moveToRow(row: Int) {
        if (row !in 0 until tableModel.dataSize) {
            GLog.d(TAG, "[goToRow] invalid idx")
            return
        }
        val lastSelectedRow = selectedRow
        // clear section 会触发 onListSelectionChanged，而 onListSelectionChanged 中会重新设置 goToLast or goToFirst 导致循环调用
        selectionModel.removeListSelectionListener(listSelectionHandler)
        clearSelection()
        selectionModel.addListSelectionListener(listSelectionHandler)

        // 跳转到目标行所在页
        tableModel.gotoPage(row / tableModel.pageSize)

        // 计算目标行在当前页的位置
        val rowIdx = row % tableModel.pageSize

        setRowSelectionInterval(rowIdx, rowIdx)

        val targetRect = getCellRect(rowIdx, columnIndex.index, false)
        targetRect.x = visibleRect.x

        // 需要多滚动一行，不然还是无法看见选中行
        val isLastRow = selectedRow == tableModel.rowCount - 1
        val isFirstRow = selectedRow == 0
        if (tableModel.isFullLogTable().not() && isLastRow.not() && isFirstRow.not()) {
            targetRect.y -= if (lastSelectedRow > rowIdx) -targetRect.height else targetRect.height
        }
        scrollRectToVisible(targetRect)
    }

    override fun moveToLastRow() {
        val lastRow = tableModel.dataSize - 1
        if (lastRow < 0) {
            return
        }
        moveToRow(lastRow)
    }

    override fun processKeyBinding(ks: KeyStroke, e: KeyEvent, condition: Int, pressed: Boolean): Boolean {
        if (disabledKeys.contains(e.keyEventInfo)) {
            return false
        }
        return super.processKeyBinding(ks, e, condition, pressed)
    }

    override fun moveToFirstRow() {
        if (tableModel.dataSize <= 0) {
            return
        }
        moveToRow(0)
    }

    fun isLastRowSelected(): Boolean {
        return selectedRow == tableModel.rowCount - 1 && tableModel.currentPage == tableModel.pageCount - 1
    }

    private fun updateRowHeight() {
        setRowHeight(getFontMetrics(font).height)
    }

    override fun moveToNextSeveralRows() {
        if (selectedRow < 0) {
            return
        }
        moveToRow((selectedRow + BATCH_ROW_COUNT_TO_SCROLL).coerceAtMost(tableModel.dataSize - 1))
    }

    override fun moveToPreviousSeveralRows() {
        if (selectedRow < 0) {
            return
        }
        moveToRow((selectedRow - BATCH_ROW_COUNT_TO_SCROLL).coerceAtLeast(0))
    }

    override fun moveToNextRow() {
        if (selectedRow < 0) {
            return
        }
        moveToRow((selectedRow + 1).coerceAtMost(tableModel.dataSize - 1))
    }

    override fun moveToPreviousRow() {
        if (selectedRow < 0) {
            return
        }
        moveToRow((selectedRow - 1).coerceAtLeast(0))
    }

    private fun showSelected(rows: IntArray) {
        if (rows.isEmpty()) {
            return
        }
        val displayContent = if (rows.size > 1) {
            getRowsContent(rows)
        } else {
            getRowsContent(
                ((rows[0] - 2).coerceAtLeast(0)..(rows[0] + 3)
                    .coerceAtMost(rowCount - 1)).toList()
            )
        }
        val caretPos = getRowsContent(arrayListOf(rows[0])).length
        val frame = findFrameFromParent<JFrame>()
        val logViewDialog = LogViewDialog(frame, displayContent.trim(), caretPos)
        logViewDialog.setLocationRelativeTo(frame)
        logViewDialog.isVisible = true
    }

    private fun getRowsContent(rows: List<Int>): String {
        return rows.joinToString("\n") { this.tableModel.getItemInCurrentPage(it).logLine }
    }

    //TODO add render effect for this
    private fun getRowsContent(rows: IntArray): String {
        return rows.joinToString("\n") { this.tableModel.getItemInCurrentPage(it).logLine }
    }

    private fun updateBookmark(rows: List<Int>) {
        val context = contexts.getContext(LogTabPanel::class.java) ?: return
        val bookmarkManager = ServiceManager.getContextService(context, BookmarkManager::class.java)
        if (bookmarkManager.checkNewRow(rows)) {
            rows.forEach(bookmarkManager::addBookmark)
        } else {
            rows.forEach(bookmarkManager::updateBookmark)
        }
    }

    private fun deleteBookmark(rows: List<Int>) {
        val context = contexts.getContext(LogTabPanel::class.java) ?: return
        val bookmarkManager = ServiceManager.getContextService(context, BookmarkManager::class.java)
        rows.forEach(bookmarkManager::removeBookmark)
    }

    internal inner class PopUp : JPopupMenu() {
        var copyItem: JMenuItem = JMenuItem(STRINGS.ui.copy)
        var showEntireItem = JMenuItem(STRINGS.ui.showEntireLine)
        var bookmarkItem = JMenuItem(STRINGS.ui.bookmark)
        var startItem = JMenuItem(STRINGS.ui.start)
        var stopItem = JMenuItem(STRINGS.ui.stop)
        var clearItem = JMenuItem(STRINGS.ui.clear)

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
                val logTabPanel = DarkUIUtil.getParentOfType(this@LogTable, LogTabPanel::class.java)
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
                        logTabPanel.startLogcat()
                    }

                    stopItem -> {
                        logTabPanel.stopScan()
                    }

                    clearItem -> {
                        logTabPanel.clearAdbLog()
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
            selectedRows.map { tableModel.getItemInCurrentPage(it).num }
        }
    }

    @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY", "UNNECESSARY_SAFE_CALL")
    override fun updateUI() {
        super.updateUI()
        updateRowHeight()
        columns?.forEach { it.configureColumn(this) }
    }

    internal inner class TableKeyHandler : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            when(event.keyEventInfo) {
                KEY_UPDATE_BOOKMARK -> updateBookmark(selectedNums())
                KEY_PAGE_DOWN -> moveToNextSeveralRows()
                KEY_PAGE_UP -> moveToPreviousSeveralRows()
                KEY_NEXT_ROW -> moveToNextRow()
                KEY_PREVIOUS_ROW -> moveToPreviousRow()
                KEY_SHOW_LOGS_IN_DIALOG -> showSelected(selectedRows)
                KEY_DELETE_BOOKMARK -> deleteBookmark(selectedNums())
                KEY_LAST_ROW -> moveToLastRow()
                KEY_FIRST_ROW -> moveToFirstRow()
            }
            super.keyPressed(event)
        }
    }

    companion object {
        private const val TAG = "LogTable"
        private const val THRESHOLD = 10
        private const val BATCH_ROW_COUNT_TO_SCROLL = 100
        private val KEY_UPDATE_BOOKMARK = Key.C_B
        private val KEY_DELETE_BOOKMARK = Key.DELETE
        private val KEY_PREVIOUS_ROW = Key.UP
        private val KEY_NEXT_ROW = Key.DOWN
        private val KEY_PAGE_UP = Key.PAGE_UP
        private val KEY_PAGE_DOWN = Key.PAGE_DOWN
        private val KEY_SHOW_LOGS_IN_DIALOG = Key.ENTER
        private val KEY_LAST_ROW = Key.C_END
        private val KEY_FIRST_ROW = Key.C_HOME
        private val disabledKeys = listOf(
            KEY_PREVIOUS_ROW, KEY_NEXT_ROW,
            KEY_SHOW_LOGS_IN_DIALOG, KEY_PAGE_UP,
            KEY_PAGE_DOWN, KEY_LAST_ROW, KEY_FIRST_ROW
        )
    }
}
