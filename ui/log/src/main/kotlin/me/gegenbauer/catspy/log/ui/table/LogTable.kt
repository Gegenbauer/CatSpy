package me.gegenbauer.catspy.log.ui.table

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.CPU
import me.gegenbauer.catspy.configuration.GSettings
import me.gegenbauer.catspy.configuration.SettingsChangeListener
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.log.ui.table.LogTableModel.Companion.COLUMN_NUM
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.ui.*
import me.gegenbauer.catspy.view.table.RowNavigation
import me.gegenbauer.catspy.view.table.lastPage
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Rectangle
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu
import javax.swing.JTable
import javax.swing.JViewport
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.ListSelectionListener
import javax.swing.text.JTextComponent

class LogTable(
    val tableModel: LogTableModel,
    override val contexts: Contexts = Contexts.default
) : JTable(tableModel), Context, RowNavigation, SettingsChangeListener {

    var listSelectionHandler: ListSelectionListener? = null
        set(value) {
            field = value
            selectionModel.addListSelectionListener(value)
        }

    private val popupMenu: JPopupMenu = PopUp()
    private var logPopupActionsProvider: () -> List<Pair<String, () -> Unit>> = { emptyList() }
    private var logDetailPopupActionsProvider: () -> List<LogDetailDialog.PopupAction> = { emptyList() }

    private val logConf: LogConfiguration
        get() = contexts.getContext(LogConfiguration::class.java) ?: LogConfiguration.default
    private val scope = MainScope()

    private val logMetadataObserver = Observer<LogMetadata> {
        scope.launch {
            withContext(Dispatchers.CPU) {
                logConf.filterCreatedAfterMetadataChanged.compareAndSet(true, true)
            }
            updateConfigure()
        }
    }

    init {
        setShowGrid(false)
        autoscrolls = false
        intercellSpacing = Dimension(0, 0)
        getTableHeader().resizingAllowed = true
        getTableHeader().reorderingAllowed = false
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        setRowSelectionAllowed(true)
        setAutoResizeMode(AUTO_RESIZE_OFF)
        columnSelectionAllowed = false

        addMouseListener(MouseHandler())
        addKeyListener(TableKeyHandler())

        updateConfigure()

        addMouseListener(RowSelectionHandler(this, tableModel))
    }

    override fun getFont(): Font {
        return SettingsManager.settings.logSettings.font.nativeFont
    }

    override fun onSettingsChanged(oldSettings: GSettings, newSettings: GSettings) {
        if (oldSettings.logSettings.font != newSettings.logSettings.font) {
            updateConfigure()
        }
    }

    private fun updateConfigure() {
        updateRowHeight()
        logConf.configureColumn(this)
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
        logConf.addObserver(logMetadataObserver)
        SettingsManager.addSettingsChangeListener(this)
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
            setInternalSelectedRows(listOf(rowIdx))
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
        val selectedRow = getInternalSelectedRow()
        return selectedRow == tableModel.rowCount - 1 && tableModel.currentPage == tableModel.pageCount - 1
    }
    
    private fun updateRowHeight() {
        setRowHeight(getFontMetrics(font).height + 4)
    }

    override fun moveToNextSeveralRows() {
        val selectedRow = getInternalSelectedRow()
        if (selectedRow < 0) {
            return
        }
        moveRowToCenter((selectedRow + BATCH_ROW_COUNT_TO_SCROLL).coerceAtMost(tableModel.dataSize - 1), true)
    }

    override fun moveToPreviousSeveralRows() {
        val selectedRow = getInternalSelectedRow()
        if (selectedRow < 0) {
            return
        }
        moveRowToCenter((selectedRow - BATCH_ROW_COUNT_TO_SCROLL).coerceAtLeast(0), true)
    }

    override fun moveToNextRow() {
        val selectedRow = getInternalSelectedRow()
        if (selectedRow < 0) {
            return
        }
        moveRowToCenter((selectedRow + 1).coerceAtMost(tableModel.dataSize - 1), true)
    }

    override fun moveToPreviousRow() {
        val selectedRow = getInternalSelectedRow()
        if (selectedRow < 0) {
            return
        }
        moveRowToCenter((selectedRow - 1).coerceAtLeast(0), true)
    }

    override fun scrollToEnd() {
        if (tableModel.dataSize <= 0) {
            return
        }
        tableModel.gotoPage(tableModel.lastPage)
        val targetRect = getCellRect(rowCount - 1, 0, true)
        targetRect.x = visibleRect.x
        targetRect.y += targetRect.height
        scrollRectToVisible(targetRect)
    }

    private fun showSelected(rows: List<Int>) {
        if (rows.isEmpty()) {
            return
        }

        suspend fun buildRendererComponent(rows: List<Int>): JTextComponent {
            return logConf.buildDetailRendererComponent(this@LogTable, rows.toList())
        }

        fun realShowSelected(rows: List<Int>) {
            val displayedRows = if (rows.size > 1) {
                rows.toList()
            } else {
                ((rows[0] - 2).coerceAtLeast(0)..(rows[0] + 3)
                    .coerceAtMost(rowCount - 1)).toList()
            }.sorted()
            scope.launch {
                val component = buildRendererComponent(displayedRows)
                showComponentInDialog(this@LogTable, component, logDetailPopupActionsProvider.invoke())
            }
        }

        if (rows.size > MAX_LOG_COUNT_SHOWS_IN_DIALOG) {
            showExceedMaxLogCount(rows.size, MAX_LOG_COUNT_SHOWS_IN_DIALOG) {
                realShowSelected(rows.take(MAX_LOG_COUNT_SHOWS_IN_DIALOG))
            }
        } else {
            realShowSelected(rows)
        }
    }

    private fun getInternalSelectedRow(): Int {
        return tableModel.selectedLogRows.firstOrNull() ?: -1
    }

    private fun setInternalSelectedRows(rows: List<Int>) {
        tableModel.selectedLogRows.clear()
        tableModel.selectedLogRows.addAll(rows)
        repaint()
    }

    private fun showComponentInDialog(
        logTable: LogTable,
        textComponent: JTextComponent,
        popupActions: List<LogDetailDialog.PopupAction>
    ) {
        val frame = logTable.contexts.getContext(JFrame::class.java) ?: return
        val logViewDialog = LogDetailDialog(frame, textComponent, popupActions, logConf.logMetaData)
        logViewDialog.setLocationRelativeTo(frame)
        logViewDialog.isVisible = true
    }

    fun setLogDetailPopupActionsProvider(actionsProvider: () -> List<LogDetailDialog.PopupAction>) {
        logDetailPopupActionsProvider = actionsProvider
    }

    private fun showExceedMaxLogCount(selected: Int, threshold: Int, afterShow: () -> Unit) {
        val frame = findFrameFromParent<JFrame>()
        val result = JOptionPane.showConfirmDialog(
            frame,
            STRINGS.ui.selectedTooMuchLogWarningMessage.get(selected, threshold),
            STRINGS.ui.selectedTooMuchLogWarningTitle,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        if (result == JOptionPane.OK_OPTION) {
            afterShow()
        }
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

        suspend fun collectRowsContent(rows: List<Int>): String {
            return withContext(Dispatchers.CPU) {
                rows.joinToString("\n") { tableModel.getItemInCurrentPage(it).toString() }
            }
        }

        fun realCopyRow(rows: List<Int>) {
            scope.launch {
                val content = collectRowsContent(rows.toList())
                toolkit.systemClipboard.setContents(
                    StringSelection(content),
                    null
                )
            }
        }

        val rows = tableModel.selectedLogRows.toList().sorted()
        if (rows.size > MAX_LOG_COUNT_CAN_BE_COPIED) {
            showExceedMaxLogCount(rows.size, MAX_LOG_COUNT_CAN_BE_COPIED) {
                realCopyRow(rows.take(MAX_LOG_COUNT_CAN_BE_COPIED))
            }
            return
        } else {
            realCopyRow(rows)
        }
    }

    /**
     * Set the log right-click menu
     * @param popupItemsProvider Menu items, Pair<String, () -> Unit>,
     * where String is the menu item name and () -> Unit is the callback for clicking the menu item
     */
    fun setTablePopupActionProvider(popupItemsProvider: () -> List<Pair<String, () -> Unit>>) {
        logPopupActionsProvider = popupItemsProvider
    }

    internal inner class PopUp : JPopupMenu() {
        private val copyItem: JMenuItem = JMenuItem(STRINGS.ui.copy)
        private val showEntireItem = JMenuItem(STRINGS.ui.showEntireLine)
        private val bookmarkItem = JMenuItem(STRINGS.ui.bookmark)

        private val actionHandler = ActionHandler()
        private val customPopupActions: MutableList<Pair<String, () -> Unit>> = mutableListOf()

        init {
            copyItem.addActionListener(actionHandler)
            showEntireItem.addActionListener(actionHandler)
            bookmarkItem.addActionListener(actionHandler)
        }

        override fun show(invoker: Component?, x: Int, y: Int) {
            prepareCustomPopupItems()
            super.show(invoker, x, y)
        }

        override fun setVisible(b: Boolean) {
            prepareCustomPopupItems()
            super.setVisible(b)
        }

        private fun prepareCustomPopupItems() {
            removeAll()
            add(copyItem)
            add(showEntireItem)
            add(bookmarkItem)
            val customPopupActions = logPopupActionsProvider.invoke()
            this.customPopupActions.clear()
            this.customPopupActions.addAll(customPopupActions)
            if (customPopupActions.isNotEmpty()) {
                addSeparator()
            }
            customPopupActions.forEach {
                val item = JMenuItem(it.first)
                item.addActionListener(actionHandler)
                add(item)
            }
        }

        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                val action = (event.source as JMenuItem).text
                when {
                    event.source == copyItem -> {
                        copySelectedRows()
                    }

                    event.source == showEntireItem -> {
                        showSelected(tableModel.selectedLogRows.toList())
                    }

                    event.source == bookmarkItem -> {
                        updateBookmark(selectedNumbers())
                    }

                    (action in customPopupActions.map { it.first }) -> {
                        customPopupActions.find { it.first == action }?.second?.invoke()
                    }
                }
            }
        }
    }

    fun onReceiveMouseReleaseEvent(event: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(event)) {
            popupMenu.show(event.component, event.x, event.y)
            SwingUtilities.updateComponentTreeUI(popupMenu)
        } else {
            popupMenu.isVisible = false
        }
    }

    internal inner class MouseHandler : MouseAdapter() {

        override fun mouseReleased(event: MouseEvent) {
            onReceiveMouseReleaseEvent(event)
            super.mouseReleased(event)
        }

        override fun mouseClicked(event: MouseEvent) {
            if (SwingUtilities.isLeftMouseButton(event) && event.isDoubleClick) {
                // 双击第一列更新书签，双击第二列显示详细日志
                if (columnAtPoint(event.point) == COLUMN_NUM) {
                    updateBookmark(selectedNumbers())
                } else {
                    showSelected(tableModel.selectedLogRows.toList())
                }
            }
            super.mouseClicked(event)
        }
    }

    private fun selectedNumbers(): List<Int> {
        val selectedRows = tableModel.selectedLogRows
        return if (selectedRows.isEmpty()) {
            emptyList()
        } else {
            selectedRows.map { tableModel.getItemInCurrentPage(it).num }
        }
    }

    override fun updateUI() {
        super.updateUI()
        updateRowHeight()
        if (tableModel != null) {
            updateConfigure()
        }
    }

    override fun destroy() {
        super.destroy()
        SettingsManager.removeSettingsChangeListener(this)
        logConf.removeObserver(logMetadataObserver)
        scope.cancel()
    }

    internal inner class TableKeyHandler : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            when (event.keyEventInfo) {
                KEY_UPDATE_BOOKMARK -> updateBookmark(selectedNumbers())
                KEY_PAGE_DOWN -> moveToNextSeveralRows()
                KEY_PAGE_UP -> moveToPreviousSeveralRows()
                KEY_NEXT_ROW -> moveToNextRow()
                KEY_PREVIOUS_ROW -> moveToPreviousRow()
                KEY_SHOW_LOGS_IN_DIALOG -> showSelected(tableModel.selectedLogRows.toList())
                KEY_DELETE_BOOKMARK -> deleteBookmark(selectedNumbers())
                KEY_LAST_ROW -> moveToLastRow()
                KEY_FIRST_ROW -> moveToFirstRow()
                KEY_COPY -> copySelectedRows()
            }
            super.keyPressed(event)
        }
    }

    private class RowSelectionHandler(
        private val table: JTable,
        private val model: ILogTableModel
    ) : MouseAdapter() {
        private var startRow = -1
        private var shiftSelectStartRow = -1

        override fun mousePressed(e: MouseEvent) {
            val row = table.rowAtPoint(e.point)
            if (row != -1) {
                if (e.isLeftClick.not()) {
                    return
                }
                startRow = row
                if (shiftSelectStartRow == -1) {
                    shiftSelectStartRow = row
                }
                if (e.isControlDown) {
                    if (model.selectedLogRows.contains(row)) {
                        model.selectedLogRows.remove(row)
                    } else {
                        model.selectedLogRows.add(row)
                    }
                    updateSelection()
                } else if (e.isShiftDown) {
                    val minRow = minOf(shiftSelectStartRow, row)
                    val maxRow = maxOf(shiftSelectStartRow, row)

                    model.selectedLogRows.clear()
                    for (i in minRow..maxRow) {
                        model.selectedLogRows.add(i)
                    }
                    updateSelection()
                } else {
                    startRow = -1
                    shiftSelectStartRow = row
                    model.selectedLogRows.clear()
                    model.selectedLogRows.add(row)
                    updateSelection()
                }
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            startRow = -1
        }

        private fun updateSelection() {
            table.repaint()
        }
    }

    companion object {
        private const val THRESHOLD = 10
        private const val BATCH_ROW_COUNT_TO_SCROLL = 100
        private const val MAX_LOG_COUNT_SHOWS_IN_DIALOG = 500
        private const val MAX_LOG_COUNT_CAN_BE_COPIED = 500
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
