package me.gegenbauer.logviewer.ui.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.ui.panel.VStatusPanel
import me.gegenbauer.logviewer.command.CmdManager
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.manager.*
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.button.ColorToggleButton
import me.gegenbauer.logviewer.ui.button.TableBarButton
import me.gegenbauer.logviewer.ui.container.WrapablePanel
import me.gegenbauer.logviewer.ui.popup.PopUpLogPanel
import me.gegenbauer.logviewer.utils.currentPlatform
import me.gegenbauer.logviewer.utils.getImageFile
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.event.*
import java.io.File
import java.net.URI
import java.util.*
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
    private val ctrlMainPanel: WrapablePanel
    private var firstBtn: JButton
    private var lastBtn: JButton
    private var tagBtn: ColorToggleButton
    private var pidBtn: ColorToggleButton
    private var tidBtn: ColorToggleButton
    private var windowedModeBtn: JButton
    private var bookmarksBtn: ColorToggleButton
    private var fullBtn: ColorToggleButton

    private val scrollPane: JScrollPane
    private val vStatusPanel: VStatusPanel
    private val table: LogTable
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
        ctrlMainPanel = WrapablePanel()
        firstBtn = JButton("")
        firstBtn.icon = ImageIcon(getImageFile("top.png"))
        firstBtn.toolTipText = STRINGS.toolTip.viewFirstBtn
        firstBtn.margin = Insets(2, 3, 1, 3)

        firstBtn.addActionListener(actionHandler)
        lastBtn = JButton("")
        lastBtn.icon = ImageIcon(getImageFile("bottom.png"))
        lastBtn.toolTipText = STRINGS.toolTip.viewLastBtn
        lastBtn.margin = Insets(2, 3, 1, 3)
        lastBtn.addActionListener(actionHandler)
        tagBtn = ColorToggleButton(STRINGS.ui.tag)
        tagBtn.toolTipText = STRINGS.toolTip.viewTagToggle
        tagBtn.margin = Insets(0, 3, 0, 3)
        tagBtn.addActionListener(actionHandler)
        pidBtn = ColorToggleButton(STRINGS.ui.pid)
        pidBtn.toolTipText = STRINGS.toolTip.viewPidToggle
        pidBtn.margin = Insets(0, 3, 0, 3)
        pidBtn.addActionListener(actionHandler)
        tidBtn = ColorToggleButton(STRINGS.ui.tid)
        tidBtn.toolTipText = STRINGS.toolTip.viewTidToggle
        tidBtn.margin = Insets(0, 3, 0, 3)
        tidBtn.addActionListener(actionHandler)
        windowedModeBtn = JButton(STRINGS.ui.windowedMode)
        windowedModeBtn.toolTipText = STRINGS.toolTip.viewWindowedModeBtn
        windowedModeBtn.margin = Insets(0, 3, 0, 3)
        windowedModeBtn.addActionListener(actionHandler)
        bookmarksBtn = ColorToggleButton(STRINGS.ui.bookmarks)
        bookmarksBtn.toolTipText = STRINGS.toolTip.viewBookmarksToggle
        bookmarksBtn.margin = Insets(0, 3, 0, 3)
        bookmarksBtn.addActionListener(actionHandler)
        fullBtn = ColorToggleButton(STRINGS.ui.full)
        fullBtn.toolTipText = STRINGS.toolTip.viewFullToggle
        fullBtn.margin = Insets(0, 3, 0, 3)
        fullBtn.addActionListener(actionHandler)

        updateTableBar(null)

        tableModel.addLogTableModelListener(tableModelHandler)
        table = LogTable(tableModel)
        table.addFocusListener(this.focusHandler)

        table.columnSelectionAllowed = true
        table.selectionModel.addListSelectionListener(listSelectionHandler)
        scrollPane = JScrollPane(table)

        vStatusPanel = VStatusPanel(table)

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

        transferHandler = TableTransferHandler()
        addComponentListener(componentHandler)

        isCreatingUI = false
    }

    private fun addVSeparator(panel: JPanel) {
        val separator1 = JSeparator(SwingConstants.VERTICAL)
        separator1.preferredSize = Dimension(separator1.preferredSize.width, 10)
        if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
            separator1.foreground = Color.GRAY
            separator1.background = Color.GRAY
        } else {
            separator1.foreground = Color.DARK_GRAY
            separator1.background = Color.DARK_GRAY
        }

        panel.add(Box.createHorizontalStrut(2))
        panel.add(separator1)
        panel.add(Box.createHorizontalStrut(2))
    }

    private fun updateTableBarFilters(customArray: ArrayList<CustomListManager.CustomElement>?) {
        val filtersBtn = TableBarButton(STRINGS.ui.filters)
        filtersBtn.icon = ImageIcon(getImageFile("filterscmds.png"))
        filtersBtn.toolTipText = STRINGS.toolTip.addFilterBtn
        filtersBtn.margin = Insets(0, 3, 0, 3)
        filtersBtn.addActionListener {
            mainUI.filtersManager.showDialog()
        }
        ctrlMainPanel.add(filtersBtn)

        val icon = ImageIcon(getImageFile("filterscmdsitem.png"))
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
        cmdsBtn.icon = ImageIcon(getImageFile("filterscmds.png"))
        cmdsBtn.toolTipText = STRINGS.toolTip.addCmdBtn
        cmdsBtn.margin = Insets(0, 3, 0, 3)
        cmdsBtn.addActionListener {
            mainUI.cmdManager.showDialog()
        }
        ctrlMainPanel.add(cmdsBtn)

        val icon = ImageIcon(getImageFile("filterscmdsitem.png"))
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

    internal inner class TableTransferHandler : TransferHandler() {
        override fun canImport(info: TransferSupport): Boolean {
            if (info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return true
            }

            if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return true
            }

            return false
        }

        override fun importData(info: TransferSupport): Boolean {
            GLog.d(TAG, "importData")
            if (!info.isDrop) {
                return false
            }

            val fileList: MutableList<File> = mutableListOf()

            if (info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val data: String
                try {
                    data = info.transferable.getTransferData(DataFlavor.stringFlavor) as String
                    val splitData = data.split("\n")

                    for (item in splitData) {
                        if (item.isNotEmpty()) {
                            GLog.d(TAG, "importData item = $item")
                            fileList.add(File(URI(item.trim())))
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    return false
                }
            }

            if (fileList.size == 0 && info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                val listFile: Any
                try {
                    listFile = info.transferable.getTransferData(DataFlavor.javaFileListFlavor)
                    if (listFile is List<*>) {
                        val iterator = listFile.iterator()
                        while (iterator.hasNext()) {
                            fileList.add(iterator.next() as File)
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    return false
                }
            }

            if (fileList.size > 0) {
                val os = currentPlatform
                GLog.d(TAG, "os = $os, drop = ${info.dropAction}, source drop = ${info.sourceDropActions}, user drop = ${info.userDropAction}")
                val action = os.getFileDropAction(info)

                var value = 1
                if (action == COPY) {
                    val options = arrayOf<Any>(
                        STRINGS.ui.append,
                        STRINGS.ui.open,
                        STRINGS.ui.cancel
                    )
                    value = JOptionPane.showOptionDialog(
                        mainUI, STRINGS.ui.msgSelectOpenMode,
                        "",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options,
                        options[0]
                    )
                }

                when (value) {
                    0 -> {
                        for (file in fileList) {
                            mainUI.openFile(file.absolutePath, true)
                        }
                    }

                    1 -> {
                        var isFirst = true
                        for (file in fileList) {
                            if (isFirst) {
                                mainUI.openFile(file.absolutePath, false)
                                isFirst = false
                            } else {
                                mainUI.openFile(file.absolutePath, true)
                            }
                        }
                    }

                    else -> {
                        GLog.d(TAG, "select cancel")
                    }
                }
            }
            return true
        }
    }

    internal inner class ComponentHandler : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
            if (e != null) {
                table.updateColumnWidth(e.component.width, scrollPane.verticalScrollBar.width)
            }
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