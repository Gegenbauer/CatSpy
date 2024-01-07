package me.gegenbauer.catspy.log.ui.table

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.ui.panel.BaseLogMainPanel
import me.gegenbauer.catspy.log.ui.dialog.LogViewDialog
import me.gegenbauer.catspy.log.ui.table.LogTableModel.Companion.COLUMN_NUM
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.Key
import me.gegenbauer.catspy.utils.findFrameFromParent
import me.gegenbauer.catspy.utils.isDoubleClick
import me.gegenbauer.catspy.utils.keyEventInfo
import me.gegenbauer.catspy.view.table.RowNavigation
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.datatransfer.StringSelection
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
        setTableHeader(null)
        setAutoResizeMode(AUTO_RESIZE_OFF)
        autoscrolls = false
        dragEnabled = true
        dropMode = DropMode.INSERT
        intercellSpacing = Dimension(0, 0)
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
        setRowSelectionAllowed(true)
        columnSelectionAllowed = false

        val columns = if (tableModel.deviceMode) {
            deviceLogColumns
        } else {
            fileLogColumns
        }
        columns.forEach { it.configureColumn(this) }

        updateRowHeight()

        addMouseListener(MouseHandler())
        addKeyListener(TableKeyHandler())

        selectionModel.addListSelectionListener {
            selectedRows.takeIf { it.isNotEmpty() }?.let {
                tableModel.selectedLogRows = it.toList()
                repaint()
            }
        }
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
        tableModel.setParent(this)
    }

    override fun moveRowToCenter(rowIndex: Int, setSelected: Boolean) {
        if (rowIndex !in 0 until tableModel.dataSize) {
            return
        }

        if (parent !is JViewport) {
            return
        }

        // clear section 会触发 onListSelectionChanged，而 onListSelectionChanged 中会重新设置 goToLast or goToFirst 导致循环调用
        selectionModel.removeListSelectionListener(listSelectionHandler)
        clearSelection()
        selectionModel.addListSelectionListener(listSelectionHandler)

        // position target page of target row
        tableModel.gotoPage(rowIndex / tableModel.pageSize)

        // 计算目标行在当前页的位置
        val rowIdx = rowIndex % tableModel.pageSize

        if (setSelected) {
            setRowSelectionInterval(rowIdx, rowIdx)
        }

        val viewPort = parent as JViewport
        val rect = getCellRect(rowIdx, 0, true)
        val viewRect = viewPort.viewRect
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y)

        var centerX = (viewRect.width - rect.width) / 2
        var centerY = (viewRect.height - rect.height) / 2
        if (rect.x < centerX) {
            centerX = -centerX
        }
        if (rect.y < centerY) {
            centerY = -centerY
        }
        rect.translate(centerX, centerY)
        viewPort.scrollRectToVisible(rect)
    }

    override fun moveToLastRow() {
        val lastRow = tableModel.dataSize - 1
        if (lastRow < 0) {
            return
        }
        moveRowToCenter(lastRow, false)
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
        moveRowToCenter(0, false)
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
        moveRowToCenter((selectedRow + BATCH_ROW_COUNT_TO_SCROLL).coerceAtMost(tableModel.dataSize - 1), true)
    }

    override fun moveToPreviousSeveralRows() {
        if (selectedRow < 0) {
            return
        }
        moveRowToCenter((selectedRow - BATCH_ROW_COUNT_TO_SCROLL).coerceAtLeast(0), true)
    }

    override fun moveToNextRow() {
        if (selectedRow < 0) {
            return
        }
        moveRowToCenter((selectedRow + 1).coerceAtMost(tableModel.dataSize - 1), true)
    }

    override fun moveToPreviousRow() {
        if (selectedRow < 0) {
            return
        }
        moveRowToCenter((selectedRow - 1).coerceAtLeast(0), true)
    }

    override fun scrollToEnd() {
        if (rowCount <= 0) {
            return
        }
        val targetRect = getCellRect(rowCount - 1, 0, true)
        targetRect.x = visibleRect.x
        targetRect.y += targetRect.height
        scrollRectToVisible(targetRect)
    }

    private fun showSelected(rows: IntArray) {
        if (rows.isEmpty()) {
            return
        }
        val displayContent = if (rows.size > 1) {
            getRenderedContent(rows.toList())
        } else {
            getRenderedContent(
                ((rows[0] - 2).coerceAtLeast(0)..(rows[0] + 3)
                    .coerceAtMost(rowCount - 1)).toList()
            )
        }
        val frame = findFrameFromParent<JFrame>()
        val logViewDialog = LogViewDialog(frame, displayContent.trim())
        logViewDialog.setParent(this)
        logViewDialog.setLocationRelativeTo(frame)
        logViewDialog.isVisible = true
    }

    private fun getRowsContent(rows: List<Int>): String {
        return rows.joinToString("\n") { tableModel.getItemInCurrentPage(it).toLogLine() }
    }

    private fun updateBookmark(rows: List<Int>) {
        val context = contexts.getContext(BaseLogMainPanel::class.java) ?: return
        val bookmarkManager = ServiceManager.getContextService(context, BookmarkManager::class.java)
        if (bookmarkManager.checkNewRow(rows)) {
            rows.forEach(bookmarkManager::addBookmark)
        } else {
            rows.forEach(bookmarkManager::updateBookmark)
        }
    }

    private fun deleteBookmark(rows: List<Int>) {
        val context = contexts.getContext(BaseLogMainPanel::class.java) ?: return
        val bookmarkManager = ServiceManager.getContextService(context, BookmarkManager::class.java)
        rows.forEach(bookmarkManager::removeBookmark)
    }

    private fun copySelectedRows() {
        toolkit.systemClipboard.setContents(
            StringSelection(getRowsContent(selectedRows.toList())),
            null
        )
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
                val logTabPanel = DarkUIUtil.getParentOfType(this@LogTable, BaseLogMainPanel::class.java)
                when (event.source) {
                    copyItem -> {
                        copySelectedRows()
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
                        logTabPanel.stopAll()
                    }

                    clearItem -> {
                        logTabPanel.clearAllLogs()
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
        if (tableModel != null) {
            val columns = if (tableModel.deviceMode) {
                deviceLogColumns
            } else {
                fileLogColumns
            }
            columns.forEach { it.configureColumn(this) }
        }
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
                KEY_COPY -> copySelectedRows()
            }
            super.keyPressed(event)
        }
    }

    companion object {
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
        private val KEY_COPY = Key.C_C
        private val disabledKeys = listOf(
            KEY_PREVIOUS_ROW, KEY_NEXT_ROW,
            KEY_SHOW_LOGS_IN_DIALOG, KEY_PAGE_UP,
            KEY_PAGE_DOWN, KEY_LAST_ROW, KEY_FIRST_ROW,
            KEY_COPY
        )
    }
}
