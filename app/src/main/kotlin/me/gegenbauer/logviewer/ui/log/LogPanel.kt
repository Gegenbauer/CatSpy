package me.gegenbauer.logviewer.ui.log

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.command.CmdManager
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.manager.*
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.button.ColorToggleButton
import me.gegenbauer.logviewer.ui.button.GButton
import me.gegenbauer.logviewer.ui.button.TableBarButton
import me.gegenbauer.logviewer.ui.container.WrapablePanel
import me.gegenbauer.logviewer.ui.panel.VStatusPanel
import me.gegenbauer.logviewer.ui.popup.PopUpLogPanel
import me.gegenbauer.logviewer.utils.applyTooltip
import me.gegenbauer.logviewer.utils.loadIcon
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.plaf.basic.BasicScrollBarUI


class LogPanel constructor(
    val mainUI: MainUI,
    tableModel: LogTableModel,
    var basePanel: LogPanel?,
    val focusHandler: FocusListener
) : JPanel() {
    private val ctrlMainPanel: WrapablePanel = WrapablePanel()
    private val firstBtn = GButton(loadIcon<DerivableImageIcon>("top.png")) applyTooltip STRINGS.toolTip.viewFirstBtn
    private val lastBtn = GButton(loadIcon<DerivableImageIcon>("bottom.png")) applyTooltip STRINGS.toolTip.viewLastBtn
    private val tagBtn = ColorToggleButton(STRINGS.ui.tag) applyTooltip STRINGS.toolTip.viewTagToggle
    private val pidBtn = ColorToggleButton(STRINGS.ui.pid) applyTooltip STRINGS.toolTip.viewPidToggle
    private val tidBtn = ColorToggleButton(STRINGS.ui.tid) applyTooltip STRINGS.toolTip.viewTidToggle
    private val windowedModeBtn = GButton(STRINGS.ui.windowedMode) applyTooltip STRINGS.toolTip.viewWindowedModeBtn
    private val bookmarksBtn = ColorToggleButton(STRINGS.ui.bookmarks) applyTooltip STRINGS.toolTip.viewBookmarksToggle
    private val fullBtn = ColorToggleButton(STRINGS.ui.full) applyTooltip STRINGS.toolTip.viewFullToggle

    private val table = LogTable(tableModel)
    private val scrollPane = JScrollPane(table)
    private val vStatusPanel = VStatusPanel(table)
    private var selectedRow = -1
    private val adjustmentHandler = AdjustmentHandler()
    private val listSelectionHandler = ListSelectionHandler()
    private val tableModelHandler = TableModelHandler()
    private val actionHandler = ActionHandler()
    private val bookmarkHandler = BookmarkHandler()
    private val componentHandler = ComponentHandler()

    private var oldLogVPos = -1
    private var oldLogHPos = -1
    private var isCreatingUI = true

    var isWindowedMode = false
        set(value) {
            field = value
            windowedModeBtn.isEnabled = !value
        }

    init {
        layout = BorderLayout()
        firstBtn.margin = Insets(2, 3, 1, 3)
        firstBtn.addActionListener(actionHandler)
        lastBtn.margin = Insets(2, 3, 1, 3)
        lastBtn.addActionListener(actionHandler)
        tagBtn.margin = Insets(0, 3, 0, 3)
        tagBtn.addActionListener(actionHandler)
        pidBtn.margin = Insets(0, 3, 0, 3)
        pidBtn.addActionListener(actionHandler)
        tidBtn.margin = Insets(0, 3, 0, 3)
        tidBtn.addActionListener(actionHandler)
        windowedModeBtn.margin = Insets(0, 3, 0, 3)
        windowedModeBtn.addActionListener(actionHandler)
        bookmarksBtn.margin = Insets(0, 3, 0, 3)
        bookmarksBtn.addActionListener(actionHandler)
        fullBtn.margin = Insets(0, 3, 0, 3)
        fullBtn.addActionListener(actionHandler)
        updateTableBar(null)
        tableModel.addLogTableModelListener(tableModelHandler)
        table.addFocusListener(this.focusHandler)
        table.columnSelectionAllowed = true
        table.selectionModel.addListSelectionListener(listSelectionHandler)
        BookmarkManager.addBookmarkEventListener(bookmarkHandler)
        scrollPane.verticalScrollBar.setUI(BasicScrollBarUI())
        scrollPane.horizontalScrollBar.setUI(BasicScrollBarUI())
        scrollPane.verticalScrollBar.unitIncrement = 20

        scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentHandler)
        scrollPane.horizontalScrollBar.addAdjustmentListener(adjustmentHandler)
        scrollPane.addMouseListener(MouseHandler())

        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        scrollPane.isOpaque = false
        scrollPane.viewport.isOpaque = false

        val ctrlPanel = JPanel()
        ctrlPanel.layout = BoxLayout(ctrlPanel, BoxLayout.Y_AXIS)
        ctrlPanel.add(ctrlMainPanel)

        add(ctrlPanel, BorderLayout.NORTH)
        add(vStatusPanel, BorderLayout.WEST)
        add(scrollPane, BorderLayout.CENTER)

        addComponentListener(componentHandler)

        isCreatingUI = false
    }

    private fun addVSeparator(panel: JPanel) {
        val separator1 = JSeparator(SwingConstants.VERTICAL)
        separator1.preferredSize = Dimension(separator1.preferredSize.width, 10)

        panel.add(Box.createHorizontalStrut(2))
        panel.add(separator1)
        panel.add(Box.createHorizontalStrut(2))
    }

    private fun updateTableBarFilters(customArray: ArrayList<CustomListManager.CustomElement>?) {
        val filtersBtn = TableBarButton(STRINGS.ui.filters)
        filtersBtn.icon = loadIcon("filterscmds.png")
        filtersBtn.toolTipText = STRINGS.toolTip.addFilterBtn
        filtersBtn.margin = Insets(0, 3, 0, 3)
        filtersBtn.addActionListener {
            mainUI.filtersManager.showDialog()
        }
        ctrlMainPanel.add(filtersBtn)

        val icon = loadIcon<DerivableImageIcon>("filterscmdsitem.png")
        if (customArray != null) {
            for (item in customArray) {
                if (!item.tableBar) {
                    continue
                }
                val button = TableBarButton(item.title)
                button.icon = icon
                button.value = item.value
                button.toolTipText =
                    "<html>${item.title} : <b>\"${item.value}\"</b><br><br>* Append : Ctrl + Click</html>"
                button.margin = Insets(0, 3, 0, 3)
                button.addActionListener { event: ActionEvent ->
                    if ((ActionEvent.CTRL_MASK and event.modifiers) != 0) {
                        val filterText = mainUI.getTextShowLogCombo()
                        if (filterText.isEmpty()) {
                            mainUI.setTextShowLogCombo((event.source as TableBarButton).value)
                        } else {
                            if (filterText.substring(filterText.length - 1) == "|") {
                                mainUI.setTextShowLogCombo(filterText + (event.source as TableBarButton).value)
                            } else {
                                mainUI.setTextShowLogCombo(filterText + "|" + (event.source as TableBarButton).value)
                            }
                        }
                    } else {
                        mainUI.setTextShowLogCombo((event.source as TableBarButton).value)
                    }
                    mainUI.applyShowLogCombo()
                }
                ctrlMainPanel.add(button)
            }
        }
    }

    private fun updateTableBarCommands(customArray: ArrayList<CustomListManager.CustomElement>?) {
        val cmdsBtn = TableBarButton(STRINGS.ui.commands)
        cmdsBtn.icon = loadIcon("filterscmds.png")
        cmdsBtn.toolTipText = STRINGS.toolTip.addCmdBtn
        cmdsBtn.margin = Insets(0, 3, 0, 3)
        cmdsBtn.addActionListener {
            mainUI.cmdManager.showDialog()
        }
        ctrlMainPanel.add(cmdsBtn)

        val icon = loadIcon<DerivableImageIcon>("filterscmdsitem.png")
        if (customArray != null) {
            for (item in customArray) {
                if (!item.tableBar) {
                    continue
                }
                val button = TableBarButton(item.title)
                button.icon = icon
                button.value = item.value
                button.toolTipText = "${item.title} : ${item.value}"
                button.margin = Insets(0, 3, 0, 3)
                button.addActionListener { event: ActionEvent ->
                    val cmd = CmdManager.replaceAdbCmdWithTargetDevice((event.source as TableBarButton).value)

                    if (cmd.isNotEmpty()) {
                        val runtime = Runtime.getRuntime()
                        runtime.exec(cmd)
                    }
                }
                ctrlMainPanel.add(button)
            }
        }
    }

    fun updateTableBar(customArray: ArrayList<CustomListManager.CustomElement>?) {
        ctrlMainPanel.removeAll()
        ctrlMainPanel.add(firstBtn)
        ctrlMainPanel.add(lastBtn)
        ctrlMainPanel.add(pidBtn)
        ctrlMainPanel.add(tidBtn)
        ctrlMainPanel.add(tagBtn)

        if (basePanel != null) {
            ctrlMainPanel.add(fullBtn)
            ctrlMainPanel.add(bookmarksBtn)
        }
        if (basePanel == null) {
            ctrlMainPanel.add(windowedModeBtn)
        }

        addVSeparator(ctrlMainPanel)
        if (basePanel != null) {
            updateTableBarFilters(customArray)
        } else {
            updateTableBarCommands(customArray)
        }
        ctrlMainPanel.updateUI()
    }

    var customFont: Font = Font(MainUI.DEFAULT_FONT_NAME, Font.PLAIN, 12)
        set(value) {
            field = value
            table.font = value
            table.rowHeight = value.size + 4

            repaint()
        }

    override fun repaint() {
        val bg = if (basePanel != null) {
            ColorManager.filterTableColor.logBG
        } else {
            ColorManager.fullTableColor.logBG
        }

        if (bg != background) {
            background = bg
        }

        super.repaint()
    }

    fun goToRow(idx: Int, column: Int) {
        if (idx < 0 || idx >= table.rowCount) {
            GLog.d(TAG, "goToRow : invalid idx")
            return
        }
        table.setRowSelectionInterval(idx, idx)
        val viewRect: Rectangle
        if (column < 0) {
            viewRect = table.getCellRect(idx, 0, true)
            viewRect.x = table.visibleRect.x
        } else {
            viewRect = table.getCellRect(idx, column, true)
        }
        table.scrollRectToVisible(viewRect)
    }

    fun goToRowByNum(num: Int, column: Int) {
        val firstNum = table.getValueAt(0, 0).toString().trim().toInt()
        var idx = num - firstNum
        if (idx < 0) {
            idx = 0
        }

        goToRow(idx, column)
    }

    fun setGoToLast(value: Boolean) {
        table.tableModel.goToLast = value
    }

    fun getGoToLast(): Boolean {
        return table.tableModel.goToLast
    }

    fun goToFirst() {
        setGoToLast(false)
        goToRow(0, -1)
        updateTableUI()
        return
    }

    fun goToLast() {
        if (table.rowCount > 0) {
            goToRow(table.rowCount - 1, -1)
            setGoToLast(true)
            updateTableUI()
        }

        return
    }

    fun updateTableUI() {
        table.updateUI()
    }

    fun getSelectedLine(): Int {
        return table.getValueAt(table.selectedRow, 0).toString().trim().toInt()
    }

    fun getSelectedRow(): Int {
        return table.selectedRow
    }

    internal inner class AdjustmentHandler : AdjustmentListener {
        override fun adjustmentValueChanged(event: AdjustmentEvent) {
            if (event.source == scrollPane.verticalScrollBar) {
                val vPos = scrollPane.verticalScrollBar.value
                if (vPos != oldLogVPos) {
                    if (vPos < oldLogVPos && getGoToLast()) {
                        setGoToLast(false)
                    } else if (vPos > oldLogVPos
                        && !getGoToLast()
                        && (vPos + scrollPane.verticalScrollBar.size.height) == scrollPane.verticalScrollBar.maximum
                    ) {
                        setGoToLast(true)
                    }
                    oldLogVPos = vPos
                    vStatusPanel.repaint()
                }
            } else if (event.source == scrollPane.horizontalScrollBar) {
                val hPos = scrollPane.horizontalScrollBar.value
                if (hPos != oldLogHPos) {
                    oldLogHPos = hPos
                }
            }

        }
    }

    internal inner class TableModelHandler : LogTableModelListener {
        @Synchronized
        override fun tableChanged(event: LogTableModelEvent) {
            if (event.dataChange == LogTableModelEvent.EVENT_CLEARED) {
                oldLogVPos = -1
            } else {
                AppScope.launch(Dispatchers.UI) {
                    tableChangedInternal(event)
                }
            }
        }

        private fun tableChangedInternal(event: LogTableModelEvent) {
            updateTableUI()
            table.updateColumnWidth(this@LogPanel.width, scrollPane.verticalScrollBar.width)
            if (event.dataChange == LogTableModelEvent.EVENT_CHANGED) {
                if (getGoToLast() && table.rowCount > 0) {
                    val viewRect = table.getCellRect(table.rowCount - 1, 0, true)
                    viewRect.x = table.visibleRect.x
                    table.scrollRectToVisible(viewRect)
                } else {
                    if (event.removedCount > 0 && table.selectedRow > 0) {
                        var idx = table.selectedRow - event.removedCount
                        if (idx < 0) {
                            idx = 0
                        }

                        val selectedLine = table.getValueAt(idx, 0).toString().trim().toInt()

                        if (selectedLine >= 0) {
                            table.setRowSelectionInterval(idx, idx)
                            val viewRect: Rectangle = table.getCellRect(idx, 0, true)
                            table.scrollRectToVisible(viewRect)
                            table.scrollRectToVisible(viewRect) // sometimes not work
                        }
                    }
                }
            } else if (event.dataChange == LogTableModelEvent.EVENT_FILTERED) {
                if (basePanel != null) {
                    val selectedLine = mainUI.getMarkLine()
                    if (selectedLine >= 0) {
                        var num = 0
                        for (idx in 0 until table.rowCount) {
                            num = table.getValueAt(idx, 0).toString().trim().toInt()
                            if (selectedLine <= num) {
                                GLog.d(TAG, "tableChanged Tid = ${Thread.currentThread().id}, num = $num, selectedLine = $selectedLine")
                                table.setRowSelectionInterval(idx, idx)
                                val viewRect: Rectangle = table.getCellRect(idx, 0, true)
                                GLog.d(TAG, "tableChanged Tid = ${Thread.currentThread().id}, viewRect = $viewRect, rowCount = ${table.rowCount}, idx = $idx")
                                table.scrollRectToVisible(viewRect)
                                table.scrollRectToVisible(viewRect) // sometimes not work
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    internal inner class ListSelectionHandler : ListSelectionListener {
        override fun valueChanged(event: ListSelectionEvent) {
            val basePanel = basePanel
            if (basePanel != null) {
                val value = table.tableModel.getValueAt(table.selectedRow, 0)
                val selectedRow = value.toString().trim().toInt()

                val baseValue = basePanel.table.tableModel.getValueAt(basePanel.table.selectedRow, 0)
                val baseSelectedRow = baseValue.toString().trim().toInt()

                if (selectedRow != baseSelectedRow) {
                    setGoToLast(false)
                    basePanel.setGoToLast(false)
                    basePanel.goToRowByNum(selectedRow, -1)
                    table.tableModel.selectionChanged = true

                    if (table.selectedRow == table.rowCount - 1) {
                        setGoToLast(true)
                    }
                }
            } else {
                if (table.selectedRow == table.rowCount - 1) {
                    setGoToLast(true)
                }
            }

            return
        }
    }

    internal inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                firstBtn -> {
                    goToFirst()
                }

                lastBtn -> {
                    goToLast()
                }

                windowedModeBtn -> {
                    mainUI.windowedModeLogPanel(this@LogPanel)
                }

                tagBtn -> {
                    val selected = tagBtn.model.isSelected
                    table.tableModel.boldTag = selected
                    table.repaint()
                }

                pidBtn -> {
                    val selected = pidBtn.model.isSelected
                    table.tableModel.boldPid = selected
                    table.repaint()
                }

                tidBtn -> {
                    val selected = tidBtn.model.isSelected
                    table.tableModel.boldTid = selected
                    table.repaint()
                }

                bookmarksBtn -> {
                    val selected = bookmarksBtn.model.isSelected
                    if (selected) {
                        fullBtn.model.isSelected = false
                    }
                    table.tableModel.bookmarkMode = selected
                    table.repaint()
                }

                fullBtn -> {
                    val selected = fullBtn.model.isSelected
                    if (selected) {
                        bookmarksBtn.model.isSelected = false
                    }
                    table.tableModel.fullMode = selected
                    table.repaint()
                }
            }
        }
    }

    internal inner class BookmarkHandler : BookmarkEventListener {
        override fun bookmarkChanged(event: BookmarkEvent) {
            vStatusPanel.repaint()
            if (table.tableModel.bookmarkMode) {
                table.tableModel.bookmarkMode = true
            }
            table.repaint()
        }
    }

    internal inner class ComponentHandler : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
            table.updateColumnWidth(e.component.width, scrollPane.verticalScrollBar.width)
            super.componentResized(e)
        }
    }

    internal inner class MouseHandler : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            super.mousePressed(event)
        }

        private var popupMenu: JPopupMenu? = null
        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                popupMenu = PopUpLogPanel(mainUI)
                popupMenu?.show(event.component, event.x, event.y)
            } else {
                popupMenu?.isVisible = false
            }

            super.mouseReleased(event)
        }

        override fun mouseDragged(e: MouseEvent) {
            GLog.d(TAG, "mouseDragged")
            super.mouseDragged(e)
        }
    }

    companion object {
        private const val TAG = "LogPanel"
    }
}
