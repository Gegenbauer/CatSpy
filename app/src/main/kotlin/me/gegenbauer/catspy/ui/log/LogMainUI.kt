package me.gegenbauer.catspy.ui.log

import com.github.weisj.darklaf.iconset.AllIcons
import com.github.weisj.darklaf.settings.ThemeSettings
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.theme.event.ThemeChangeEvent
import com.malinskiy.adam.request.device.Device
import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.context.*
import me.gegenbauer.catspy.data.model.log.FilterItem
import me.gegenbauer.catspy.data.model.log.LogLevel
import me.gegenbauer.catspy.data.model.log.LogcatLogItem
import me.gegenbauer.catspy.data.model.log.LogcatRealTimeFilter
import me.gegenbauer.catspy.data.repo.log.*
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.databinding.bind.withName
import me.gegenbauer.catspy.databinding.property.support.PROPERTY_TEXT
import me.gegenbauer.catspy.ddmlib.device.DeviceListListener
import me.gegenbauer.catspy.ddmlib.device.DeviceManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.task.PeriodicTask
import me.gegenbauer.catspy.task.TaskListener
import me.gegenbauer.catspy.task.TaskManager
import me.gegenbauer.catspy.ui.button.*
import me.gegenbauer.catspy.ui.combobox.*
import me.gegenbauer.catspy.ui.dialog.GoToDialog
import me.gegenbauer.catspy.ui.dialog.LogTableDialog
import me.gegenbauer.catspy.ui.icon.DayNightIcon
import me.gegenbauer.catspy.ui.menu.FileOpenPopupMenu
import me.gegenbauer.catspy.ui.panel.SplitLogPane
import me.gegenbauer.catspy.ui.panel.next
import me.gegenbauer.catspy.ui.state.EmptyStatePanel
import me.gegenbauer.catspy.ui.tab.OnTabChangeListener
import me.gegenbauer.catspy.utils.*
import me.gegenbauer.catspy.viewmodel.MainViewModel
import java.awt.*
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.text.JTextComponent

class LogMainUI(override val contexts: Contexts = Contexts.default) : JPanel(), Context, TaskListener,
    LogObservable.Observer<LogcatLogItem>, Disposable, OnTabChangeListener {

    //region scoped service
    private val bookmarkManager = ServiceManager.getContextService(this, BookmarkManager::class.java)
    //endregion

    //region task
    private val taskManager = TaskManager()
    private val updateLogUITask = PeriodicTask(500, "updateLogUITask")
    //endregion

    //region log data
    private val logProvider = LogcatLogProvider()
    private val fullLogcatRepository = FullLogcatRepository(updateLogUITask)
    private val filteredLogcatRepository = FilteredLogcatRepository(taskManager, updateLogUITask, bookmarkManager)
    //endregion

    //region filterPanel
    private val filterPanel = JPanel()
    //region logPanel
    private val logPanel = JPanel()

    val showLogToggle = ColorToggleButton(STRINGS.ui.log, STRINGS.toolTip.logToggle)
    val showLogCombo = filterComboBox(tooltip = STRINGS.toolTip.logCombo) withName STRINGS.ui.log
    // TODO 布局需要优化
    val showTagToggle = ColorToggleButton(STRINGS.ui.tag, STRINGS.toolTip.tagToggle)
    val showTagCombo = filterComboBox(tooltip = STRINGS.toolTip.tagCombo) withName STRINGS.ui.tag

    val showPidToggle = ColorToggleButton(STRINGS.ui.pid, STRINGS.toolTip.pidToggle)
    val showPidCombo = filterComboBox(tooltip = STRINGS.toolTip.pidCombo) withName STRINGS.ui.pid

    val showTidToggle = ColorToggleButton(STRINGS.ui.tid, STRINGS.toolTip.tidToggle)
    val showTidCombo = filterComboBox(tooltip = STRINGS.toolTip.tidCombo) withName STRINGS.ui.tid

    val logLevelToggle = ColorToggleButton(STRINGS.ui.logLevel)
    val logLevelCombo = readOnlyComboBox() withName STRINGS.ui.logLevel

    val boldLogToggle = ColorToggleButton(STRINGS.ui.bold, STRINGS.toolTip.boldToggle)
    val boldLogCombo = filterComboBox(tooltip = STRINGS.toolTip.boldCombo)

    val matchCaseToggle = ColorToggleButton("Aa", STRINGS.toolTip.caseToggle)
    //endregion

    //region toolBarPanel
    private val toolBarPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

    //region logToolBar
    private val logToolBar = JPanel(FlowLayout(FlowLayout.LEFT)) withName "logToolBar"

    val startBtn = StatefulButton(loadThemedIcon("start.svg"), STRINGS.ui.start, STRINGS.toolTip.startBtn)
    val pauseToggle = StatefulToggleButton(
        loadIcon("pause_off.png"),
        DayNightIcon(loadIcon("pause_on.png"), loadIcon("pause_on_dark.png")),
        STRINGS.ui.pause,
        tooltip = STRINGS.toolTip.pauseBtn
    )
    val stopBtn = StatefulButton(loadIcon("stop.png"), STRINGS.ui.stop, STRINGS.toolTip.stopBtn)
    val saveBtn = StatefulButton(loadIcon("save.svg"), STRINGS.ui.save, STRINGS.toolTip.saveBtn)
    val deviceCombo = darkComboBox(STRINGS.toolTip.devicesCombo)
    private val deviceStatus = JLabel("None", JLabel.LEFT) // TODO 整理设备连接状态相关的代码
    val adbConnectBtn = StatefulButton(loadIcon("connect.png"), STRINGS.ui.connect, STRINGS.toolTip.connectBtn)
    val adbDisconnectBtn =
        StatefulButton(loadIcon("disconnect.png"), STRINGS.ui.disconnect, STRINGS.toolTip.disconnectBtn)
    val adbRefreshBtn = StatefulButton(loadIcon("refresh.png"), STRINGS.ui.refresh, STRINGS.toolTip.refreshBtn)
    val rotateLogPanelBtn =
        StatefulButton(loadThemedIcon("rotate.svg"), STRINGS.ui.rotation, STRINGS.toolTip.rotationBtn)
    val clearViewsBtn = StatefulButton(loadIcon("clear.png"), STRINGS.ui.clearViews, STRINGS.toolTip.clearBtn)
    //endregion
    //endregion

    //region searchPanel
    internal val searchPanel by lazy { SearchPanel() }
    //endregion
    //endregion

    //region splitLogPane
    private val splitLogWithEmptyStatePanel = EmptyStatePanel()
    private val fullTableModel = LogTableModel(fullLogcatRepository)
    val filteredTableModel = LogTableModel(filteredLogcatRepository)
    val splitLogPane = SplitLogPane(fullTableModel, filteredTableModel).apply {
        onFocusGained = {
            searchPanel.setTargetView(it)
        }
    }
    //endregion

    //region statusBar
    private val statusBar = JPanel(BorderLayout())

    private val statusMethod = JLabel("")
    private val statusTF = StatusTextField(STRINGS.ui.none) applyTooltip STRINGS.toolTip.savedFileTf
    //endregion

    //region menu
    // TODO file open
    private val filePopupMenu = FileOpenPopupMenu().apply {
        onFileSelected = { file ->
            openFile(file.absolutePath, false)
        }
        onFileFollowSelected = { file ->
            startFileFollow(file.absolutePath)
        }
        onFileListSelected = { files ->
            var isFirst = true
            for (file in files) {
                if (isFirst) {
                    openFile(file.absolutePath, false)
                    isFirst = false
                } else {
                    openFile(file.absolutePath, true)
                }
            }
        }
        onFilesAppendSelected = { files ->
            for (file in files) {
                openFile(file.absolutePath, true)
            }
        }
    }
    //endregion

    //region events
    private val logPanelMouseListener = LogPanelMouseListener()
    private val keyHandler = KeyHandler()
    private val actionHandler = ActionHandler()
    private val mouseHandler = MouseHandler()
    private val statusChangeListener = StatusChangeListener()
    //endregion

    var customFont: Font = Font(
        UIConfManager.uiConf.logFontName,
        UIConfManager.uiConf.logFontStyle,
        UIConfManager.uiConf.logFontSize
    )
        set(value) {
            field = value
            splitLogPane.filteredLogPanel.customFont = value
            splitLogPane.fullLogPanel.customFont = value
        }

    private val themeChangeListener: (ThemeChangeEvent) -> Unit = {
        registerComboBoxEditorEvent()
    }

    private val devicesChangeListener = DeviceListListener {
        refreshDevices(it)
    }

    init {
        GlobalContextManager.register(this)

        logProvider.addObserver(fullLogcatRepository)
        logProvider.addObserver(filteredLogcatRepository)

        createUI()

        registerEvent()

        observeViewModelValue()

        MainViewModel.bind(this)

        ThemeManager.registerThemeUpdateListener(themeChangeListener)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        filteredTableModel.setContexts(contexts)
        fullTableModel.setContexts(contexts)
        filePopupMenu.setContexts(contexts)

        ServiceManager.getContextService(DeviceManager::class.java).registerDeviceListener(devicesChangeListener)
    }

    private fun observeViewModelValue() {
        MainViewModel.pauseAll.addObserver {
            taskManager.updatePauseState(it == true)
        }
        MainViewModel.filterMatchCaseEnabled.addObserver { updateLogFilter() }
        MainViewModel.logLevel.addObserver { updateLogFilter() }
        MainViewModel.logFilterEnabled.addObserver { updateLogFilter() }
        MainViewModel.tagFilterEnabled.addObserver { updateLogFilter() }
        MainViewModel.pidFilterEnabled.addObserver { updateLogFilter() }
        MainViewModel.tidFilterEnabled.addObserver { updateLogFilter() }
        MainViewModel.logLevelFilterEnabled.addObserver { updateLogFilter() }
        MainViewModel.searchMatchCase.addObserver { filteredTableModel.searchMatchCase = it == true }
    }

    private fun refreshDevices(devices: List<Device>) {
        MainViewModel.connectedDevices.updateValue((devices.map { it.serial }).toHistoryItemList())
        MainViewModel.currentDevice.updateValue(devices.firstOrNull()?.serial)
    }

    // TODO 关闭 tab 时调用
    private fun saveConfigOnDestroy() {
        UIConfManager.uiConf.logFontSize = customFont.size
        UIConfManager.uiConf.logFontName = customFont.name
        UIConfManager.uiConf.logFontStyle = customFont.style
        UIConfManager.saveUI()
    }

    private fun createUI() {
        boldLogCombo.enabledTfTooltip = false

        deviceStatus.isEnabled = false
        deviceCombo.setWidth(150)

        deviceStatus.horizontalAlignment = JLabel.CENTER

        val p = PREFERRED
        logPanel.layout = TableLayout(
            doubleArrayOf(p, FILL, p, 0.15, p, 0.15, p, 0.15, p, 0.10, p, 0.15, p),
            doubleArrayOf(p)
        )
        logPanel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        logPanel.add(showLogToggle, "0, 0")
        logPanel.add(showLogCombo, "1, 0")
        logPanel.add(showTagToggle, "2, 0")
        logPanel.add(showTagCombo, "3, 0")
        logPanel.add(showPidToggle, "4, 0")
        logPanel.add(showPidCombo, "5, 0")
        logPanel.add(showTidToggle, "6, 0")
        logPanel.add(showTidCombo, "7, 0")
        logPanel.add(logLevelToggle, "8, 0")
        logPanel.add(logLevelCombo, "9, 0")
        logPanel.add(boldLogToggle, "10, 0")
        logPanel.add(boldLogCombo, "11, 0")
        logPanel.add(matchCaseToggle, "12, 0")

        filterPanel.layout = BorderLayout()
        filterPanel.add(logPanel, BorderLayout.CENTER)

        logToolBar.add(startBtn)
        logToolBar.addVSeparator2()
        logToolBar.add(pauseToggle)
        logToolBar.add(stopBtn)
        logToolBar.add(saveBtn)
        logToolBar.addVSeparator2()
        logToolBar.add(deviceCombo)
        logToolBar.add(deviceStatus)
        logToolBar.add(adbConnectBtn)
        logToolBar.add(adbDisconnectBtn)
        logToolBar.add(adbRefreshBtn)
        logToolBar.addVSeparator2()
        logToolBar.add(rotateLogPanelBtn)
        logToolBar.addVSeparator2()
        logToolBar.add(clearViewsBtn)

        toolBarPanel.layout = BorderLayout()
        toolBarPanel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        toolBarPanel.add(logToolBar, BorderLayout.CENTER)

        filterPanel.add(toolBarPanel, BorderLayout.NORTH)
        filterPanel.add(searchPanel, BorderLayout.SOUTH)

        layout = BorderLayout()

        splitLogPane.fullLogPanel.updateTableBar()
        splitLogPane.filteredLogPanel.updateTableBar()

        splitLogPane.isOneTouchExpandable = false

        statusBar.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        statusMethod.isOpaque = true
        statusMethod.background = Color.DARK_GRAY
        statusTF.isEditable = false
        statusTF.border = BorderFactory.createEmptyBorder()

        statusBar.add(statusMethod, BorderLayout.WEST)
        statusBar.add(statusTF, BorderLayout.CENTER)

        deviceStatus.text = STRINGS.ui.connected
        setDeviceComboColor(true)

        customFont = UIConfManager.uiConf.getLogFont()
        GLog.d(TAG, "[createUI] log font: $customFont")
        splitLogPane.filteredLogPanel.customFont = customFont
        splitLogPane.fullLogPanel.customFont = customFont

        searchPanel.searchMatchCaseToggle.isSelected = UIConfManager.uiConf.searchMatchCaseEnabled
        filteredTableModel.searchMatchCase = UIConfManager.uiConf.searchMatchCaseEnabled
        fullTableModel.searchMatchCase = UIConfManager.uiConf.searchMatchCaseEnabled

        splitLogWithEmptyStatePanel.setContent(splitLogPane)
        splitLogWithEmptyStatePanel.action = {
            SwingUtilities.updateComponentTreeUI(filePopupMenu)
            filePopupMenu.show(it, it.width / 2, it.height / 2)
        }

        add(filterPanel, BorderLayout.NORTH)
        add(splitLogWithEmptyStatePanel, BorderLayout.CENTER)
        add(statusBar, BorderLayout.SOUTH)
    }

    private fun registerEvent() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { event ->
            when {
                event.keyCode == KeyEvent.VK_PAGE_DOWN && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> {
                    splitLogPane.filteredLogPanel.goToLast()
                    splitLogPane.fullLogPanel.goToLast()
                }

                event.keyCode == KeyEvent.VK_PAGE_UP && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> {
                    splitLogPane.filteredLogPanel.goToFirst()
                    splitLogPane.fullLogPanel.goToFirst()
                }

                event.keyCode == KeyEvent.VK_L && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> {
                    deviceCombo.requestFocus()
                }

                event.keyCode == KeyEvent.VK_R && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> {
                    reconnectAdb()
                }

                event.keyCode == KeyEvent.VK_G && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> {
                    val goToDialog = GoToDialog(findFrameFromParent())
                    goToDialog.setLocationRelativeTo(this@LogMainUI)
                    goToDialog.isVisible = true
                    if (goToDialog.line != -1) {
                        GLog.d(TAG, "[KeyEventDispatcher] Cancel Goto Line ${goToDialog.line}")
                        goToLine(goToDialog.line)
                    } else {
                        GLog.d(TAG, "[KeyEventDispatcher] Cancel Goto Line")
                    }
                }
            }

            false
        }

        // fix bug: event registered for editor in ComboBox will be removed when ComboBoxUI changed
        registerComboBoxEditorEvent()

        logToolBar.addMouseListener(logPanelMouseListener)
        logToolBar.addMouseListener(mouseHandler)
        startBtn.addActionListener(actionHandler)
        startBtn.addMouseListener(mouseHandler)
        stopBtn.addActionListener(actionHandler)
        stopBtn.addMouseListener(mouseHandler)
        clearViewsBtn.addActionListener(actionHandler)
        rotateLogPanelBtn.addActionListener(actionHandler)
        clearViewsBtn.addMouseListener(mouseHandler)
        saveBtn.addActionListener(actionHandler)
        saveBtn.addMouseListener(mouseHandler)
        adbConnectBtn.addActionListener(actionHandler)
        adbRefreshBtn.addActionListener(actionHandler)
        adbDisconnectBtn.addActionListener(actionHandler)
        filterPanel.addMouseListener(mouseHandler)
        toolBarPanel.addMouseListener(mouseHandler)
        statusMethod.addPropertyChangeListener(statusChangeListener)
        statusTF.document.addDocumentListener(statusChangeListener)

        fullTableModel.addLogTableModelListener { event ->
            splitLogWithEmptyStatePanel.contentVisible = (event.source as LogTableModel).rowCount > 0
        }
        splitLogPane.fullLogPanel.table.selectionModel.addListSelectionListener {
            fullLogcatRepository.selectedRow = it.firstIndex
        }
        splitLogPane.filteredLogPanel.table.selectionModel.addListSelectionListener {
            filteredLogcatRepository.selectedRow = it.firstIndex
        }
        logProvider.addObserver(this)
    }

    // TODO register after ui change
    fun registerComboBoxEditorEvent() {
        showLogCombo.editorComponent.addKeyListener(keyHandler)
        showLogCombo.editorComponent.addMouseListener(mouseHandler)
        boldLogCombo.editorComponent.addKeyListener(keyHandler)
        boldLogCombo.editorComponent.addMouseListener(mouseHandler)
        showTagCombo.editorComponent.addKeyListener(keyHandler)
        showTagCombo.editorComponent.addMouseListener(mouseHandler)
        showPidCombo.editorComponent.addKeyListener(keyHandler)
        showPidCombo.editorComponent.addMouseListener(mouseHandler)
        showTidCombo.editorComponent.addKeyListener(keyHandler)
        showTidCombo.editorComponent.addMouseListener(mouseHandler)
        deviceCombo.editorComponent.addKeyListener(keyHandler)
        deviceCombo.editorComponent.addMouseListener(mouseHandler)
        searchPanel.registerComboBoxEditorEvent()
    }

    inner class StatusChangeListener : PropertyChangeListener, DefaultDocumentListener() {
        private var method = ""
        override fun propertyChange(evt: PropertyChangeEvent) {
            if (evt.source == statusMethod && evt.propertyName == PROPERTY_TEXT) {
                method = evt.newValue.toString().trim()
            }
        }

        override fun insertUpdate(e: DocumentEvent) {
            updateTitleBar(method)
        }
    }

    // TODO update title
    private fun updateTitleBar(statusMethod: String) {
        val title = when (statusMethod) {
            STRINGS.ui.open, STRINGS.ui.follow, "${STRINGS.ui.follow} ${STRINGS.ui.stop}" -> {
                val path: Path = Paths.get(statusTF.text)
                path.fileName.toString()
            }

            STRINGS.ui.adb, STRINGS.ui.cmd, "${STRINGS.ui.adb} ${STRINGS.ui.stop}", "${STRINGS.ui.cmd} ${STRINGS.ui.stop}" -> {
                (MainViewModel.currentDevice.value ?: "").ifEmpty { STRINGS.ui.app }
            }

            else -> {
                STRINGS.ui.app
            }
        }
    }

    // TODO detach log 逻辑梳理
    fun detachLogPanel(logPanel: FullLogPanel) {
        if (logPanel.parent == splitLogPane) {
            logPanel.isWindowedMode = true
            splitLogPane.remove(logPanel)
            SwingUtilities.updateComponentTreeUI(splitLogPane)
        }
    }

    fun windowedModeLogPanel(logPanel: FullLogPanel) {
        detachLogPanel(logPanel)
        val logTableDialog = LogTableDialog(logPanel) {
            attachLogPanel(logPanel)
        }
        logTableDialog.isVisible = true
    }

    fun attachLogPanel(logPanel: FullLogPanel) {
        logPanel.isWindowedMode = false
        splitLogPane.resetWithCurrentRotation()
    }

    fun openFile(path: String, isAppend: Boolean) {
        GLog.d(TAG, "[openFile] Opening: $path, $isAppend")
        statusMethod.text = " ${STRINGS.ui.open} "
        logProvider.clear()
        updateLogFilter()

        if (updateLogUITask.isRunning().not()) {
            taskManager.exec(updateLogUITask)
        }
        logProvider.stopCollectLog()
        logProvider.startCollectLog(FileLogCollector(taskManager, path))

        if (isAppend) {
            statusTF.text += "| $path"
        } else {
            statusTF.text = path
        }

        return
    }

    fun startAdbScan() {
        statusMethod.text = " ${STRINGS.ui.adb} "
        updateLogFilter()

        if (updateLogUITask.isRunning().not()) {
            taskManager.exec(updateLogUITask)
        }
        logProvider.stopCollectLog()
        logProvider.startCollectLog(RealTimeLogCollector(taskManager, MainViewModel.currentDevice.value ?: ""))
        MainViewModel.pauseAll.updateValue(false)

        statusTF.text = logProvider.logTempFile?.absolutePath ?: ""
    }

    fun stopAll() {
        statusMethod.text = " ${STRINGS.ui.adb} ${STRINGS.ui.stop} "

        if (!logProvider.isCollecting()) {
            GLog.d(TAG, "stopAdbScan : not adb scanning mode")
            return
        }

        logProvider.stopCollectLog()
        taskManager.cancelAll()
    }

    private fun startFileFollow(filePath: String) {
        statusTF.text = filePath
        statusMethod.text = " ${STRINGS.ui.follow} "
        // TODO file follow
    }

    private inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                adbConnectBtn -> {
                    connect()
                }

                adbDisconnectBtn -> {
                    stopAll()
                    // disconnect
                }

                startBtn -> {
                    startAdbScan()
                }

                stopBtn -> {
                    stopScan()
                }

                rotateLogPanelBtn -> {
                    MainViewModel.rotation.value?.let {
                        MainViewModel.rotation.updateValue(it.next())
                    }
                }

                clearViewsBtn -> {
                    clearViews()
                }

                adbRefreshBtn -> {
                    //refreshDevices()
                }

                saveBtn -> {
                    if (logProvider.isCollecting()) {
                        // TODO Save As
                    } else {
                        // TODO Disable Save button
                        GLog.d(TAG, "SaveBtn : not adb scanning mode")
                    }
                }
            }
        }
    }

    private fun connect() {
        stopAll()
        // TODO connect
    }

    private fun stopScan() {
        stopAll()
    }

    private fun clearViews() {
        filteredLogcatRepository.clear()
        fullLogcatRepository.clear()
        repaint()
    }

    private class LogPanelMouseListener : MouseAdapter() {
        private val popupMenu: JPopupMenu = ButtonDisplayModeSelectMenu()

        override fun mouseReleased(e: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(e)) {
                // Update UI with current theme
                SwingUtilities.updateComponentTreeUI(popupMenu)
                popupMenu.show(e.component, e.x, e.y)
            } else {
                popupMenu.isVisible = false
            }
        }

        class ButtonDisplayModeSelectMenu : JPopupMenu() {
            private val itemIconText: JMenuItem =
                JMenuItem("IconText").apply { putClientProperty("ButtonDisplayMode", ButtonDisplayMode.ALL) }
            private val itemIcon: JMenuItem =
                JMenuItem("Icon").apply { putClientProperty("ButtonDisplayMode", ButtonDisplayMode.ICON) }
            private val itemText: JMenuItem =
                JMenuItem("Text").apply { putClientProperty("ButtonDisplayMode", ButtonDisplayMode.TEXT) }
            private val actionHandler = ActionHandler()

            init {
                add(itemIconText)
                add(itemIcon)
                add(itemText)
                itemIconText.addActionListener(actionHandler)
                itemIcon.addActionListener(actionHandler)
                itemText.addActionListener(actionHandler)
            }

            private inner class ActionHandler : ActionListener {
                override fun actionPerformed(event: ActionEvent) {
                    MainViewModel.buttonDisplayMode.updateValue(
                        (event.source as JComponent).getClientProperty("ButtonDisplayMode") as ButtonDisplayMode
                    )
                }
            }
        }
    }

    private inner class PopUpCombobox(private val combo: HistoryComboBox<String>) : JPopupMenu() {
        private val selectAllItem: JMenuItem = JMenuItem("Select All")
        private val copyItem: JMenuItem = JMenuItem("Copy")
        private val pasteItem: JMenuItem = JMenuItem("Paste")
        private val reconnectItem: JMenuItem = JMenuItem("Reconnect " + deviceCombo.selectedItem?.toString())
        private val actionHandler = ActionHandler()

        init {
            add(selectAllItem)
            add(copyItem)
            add(pasteItem)
            add(reconnectItem)
            selectAllItem.addActionListener(actionHandler)
            copyItem.addActionListener(actionHandler)
            pasteItem.addActionListener(actionHandler)
            reconnectItem.addActionListener(actionHandler)
        }

        inner class ActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                when (event.source) {
                    selectAllItem -> {
                        combo.editor?.selectAll()
                    }

                    copyItem -> {
                        val editorCom = combo.editor?.editorComponent as JTextComponent
                        val stringSelection = StringSelection(editorCom.selectedText)
                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(stringSelection, null)
                    }

                    pasteItem -> {
                        val editorCom = combo.editor?.editorComponent as JTextComponent
                        editorCom.paste()
                    }

                    reconnectItem -> {
                        reconnectAdb()
                    }
                }
            }
        }
    }

    private inner class PopUpFilterCombobox(private val combo: FilterComboBox) : JPopupMenu() {
        private val selectAllItem = JMenuItem("Select All")
        private val copyItem = JMenuItem("Copy").apply {
            icon = loadDarklafThemedIcon("menu/copy.svg")
        }
        private val pasteItem = JMenuItem("Paste").apply {
            icon = loadDarklafThemedIcon("menu/paste.svg")
        }
        private val actionHandler = ActionHandler()

        init {
            selectAllItem.addActionListener(actionHandler)
            copyItem.addActionListener(actionHandler)
            pasteItem.addActionListener(actionHandler)

            add(selectAllItem)
            add(copyItem)
            add(pasteItem)
        }

        inner class ActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                when (event.source) {
                    selectAllItem -> {
                        combo.editor.selectAll()
                    }

                    copyItem -> {
                        val editorCom = combo.editorComponent
                        val stringSelection = StringSelection(editorCom.selectedText)
                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(stringSelection, null)
                    }

                    pasteItem -> {
                        val editorCom = combo.editorComponent
                        editorCom.paste()
                    }
                }
            }
        }
    }

    private inner class MouseHandler : MouseAdapter() {
        private var popupMenu: JPopupMenu? = null

        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                when (event.source) {
                    deviceCombo.editorComponent -> {
                        popupMenu = PopUpCombobox(deviceCombo)
                        popupMenu?.show(event.component, event.x, event.y)
                    }

                    showLogCombo.editorComponent, boldLogCombo.editorComponent, showTagCombo.editorComponent, showPidCombo.editorComponent, showTidCombo.editorComponent -> {
                        popupMenu = PopUpFilterCombobox((event.source as JComponent).parent as FilterComboBox)
                        popupMenu?.show(event.component, event.x, event.y)
                    }

                    else -> {
                        val compo = event.source as JComponent
                        val transformedEvent = MouseEvent(
                            compo.parent,
                            event.id,
                            event.`when`,
                            event.modifiersEx,
                            event.x + compo.x,
                            event.y + compo.y,
                            event.clickCount,
                            event.isPopupTrigger
                        )

                        compo.parent.dispatchEvent(transformedEvent)
                    }
                }
            } else {
                popupMenu?.isVisible = false
            }

            super.mouseReleased(event)
        }
    }


    fun reconnectAdb() {
        GLog.d(TAG, "Reconnect ADB")
        stopScan()
        Thread.sleep(200)

        if (deviceCombo.selectedItem!!.toString().isNotBlank()) {
            connect()
            Thread.sleep(200)
        }

        Thread {
            Thread.sleep(200)
            clearViews()
            Thread.sleep(200)
            startAdbScan()
        }.start()
    }

    fun startAdbLog() {
        Thread {
            startAdbScan()
        }.start()
    }

    fun stopAdbLog() {
        stopScan()
    }

    fun clearAdbLog() {
        Thread {
            clearViews()
        }.start()
    }

    fun getTextShowLogCombo(): String {
        if (showLogCombo.selectedItem == null) {
            return ""
        }
        return showLogCombo.selectedItem!!.toString()
    }

    fun setTextShowLogCombo(text: String) {
        showLogCombo.selectedItem = text
        showLogCombo.updateTooltip()
    }

    fun applyShowLogCombo() {
        resetComboItem(MainViewModel.logFilterHistory, MainViewModel.logFilterCurrentContent.value ?: "")
    }

    fun applyShowLogComboEditor() {
        val editorCom = showLogCombo.editorComponent
        val text = editorCom.text
        setTextShowLogCombo(text)
        applyShowLogCombo()
    }

    private fun setDeviceComboColor(isConnected: Boolean) {
        if (isConnected) {
            if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                deviceCombo.editorComponent.foreground = Color(0x7070C0)
            } else {
                deviceCombo.editorComponent.foreground = Color.BLUE
            }
        } else {
            if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                deviceCombo.editorComponent.foreground = Color(0xC07070)
            } else {
                deviceCombo.editorComponent.foreground = Color.RED
            }
        }
    }

    private inner class KeyHandler : KeyAdapter() {
        override fun keyReleased(event: KeyEvent) {
            if (KeyEvent.VK_ENTER == event.keyCode && event.isControlDown) {
                when (event.source) {
                    in listOf(
                        showLogCombo.editorComponent,
                        boldLogCombo.editorComponent,
                        showTagCombo.editorComponent,
                        showPidCombo.editorComponent,
                        showTidCombo.editorComponent,
                        boldLogCombo.editorComponent,
                    ) -> {
                        updateComboBox()
                        updateLogFilter()
                    }

                    deviceCombo.editorComponent -> {
                        reconnectAdb()
                    }

                }
            }
            super.keyReleased(event)
        }
    }

    private fun updateComboBox() {
        resetComboItem(MainViewModel.logFilterHistory, MainViewModel.logFilterCurrentContent.value ?: "")
        resetComboItem(MainViewModel.tagFilterHistory, MainViewModel.tagFilterCurrentContent.value ?: "")
        resetComboItem(MainViewModel.pidFilterHistory, MainViewModel.pidFilterCurrentContent.value ?: "")
        resetComboItem(MainViewModel.tidFilterHistory, MainViewModel.tidFilterCurrentContent.value ?: "")
        resetComboItem(MainViewModel.boldHistory, MainViewModel.boldCurrentContent.value ?: "")
    }

    private fun updateLogFilter() {
        LogcatRealTimeFilter(
            if (MainViewModel.logFilterEnabled.getValueNonNull()) showLogCombo.filterItem else FilterItem.emptyItem,
            if (MainViewModel.tagFilterEnabled.getValueNonNull()) showTagCombo.filterItem else FilterItem.emptyItem,
            if (MainViewModel.pidFilterEnabled.getValueNonNull()) showPidCombo.filterItem else FilterItem.emptyItem,
            if (MainViewModel.tidFilterEnabled.getValueNonNull()) showTidCombo.filterItem else FilterItem.emptyItem,
            if (MainViewModel.logLevelFilterEnabled.getValueNonNull()) MainViewModel.logLevel.getValueNonNull() else LogLevel.VERBOSE,
            MainViewModel.filterMatchCaseEnabled.getValueNonNull()
        ).apply {
            filteredLogcatRepository.logFilter = this
            fullLogcatRepository.logFilter = this
        }
        filteredTableModel.highlightFilterItem = boldLogCombo.filterItem
    }

    private fun updateSearchFilter() {
        filteredTableModel.searchFilterItem = searchPanel.searchCombo.filterItem
    }

    fun resetComboItem(viewModelProperty: ObservableViewModelProperty<List<HistoryItem<String>>>, item: String) {
        if (item.isBlank()) return
        val list = viewModelProperty.value
        list ?: return
        val historyItem = HistoryItem(item)
        if (list.contains(historyItem)) {
            viewModelProperty.updateValue(ArrayList(list).apply {
                remove(historyItem)
                add(historyItem)
                sortWith(HistoryItem.comparator)
            })
            return
        }
        viewModelProperty.updateValue(ArrayList(list).apply {
            add(historyItem)
            sortWith(HistoryItem.comparator)
        })
        return
    }

    private fun goToLine(line: Int) {
        GLog.d(TAG, "Line : $line")
        if (line < 0) {
            return
        }
        var num = 0
        for (idx in 0 until filteredTableModel.rowCount) {
            num = filteredTableModel.getValueAt(idx, 0).toString().trim().toInt()
            if (line <= num) {
                splitLogPane.filteredLogPanel.goToRow(idx, 0)
                break
            }
        }

        if (line != num) {
            for (idx in 0 until fullTableModel.rowCount) {
                num = fullTableModel.getValueAt(idx, 0).toString().trim().toInt()
                if (line <= num) {
                    splitLogPane.fullLogPanel.goToRow(idx, 0)
                    break
                }
            }
        }
    }

    private inner class StatusTextField(text: String?) : JTextField(text) {
        private var prevText = ""
        override fun getToolTipText(event: MouseEvent): String? {
            val textTrimmed = text.trim()
            if (prevText != textTrimmed && textTrimmed.isNotEmpty()) {
                prevText = textTrimmed
                val splitData = textTrimmed.split("|")

                var tooltip = "<html>"
                for (item in splitData) {
                    val itemTrimmed = item.trim()
                    if (itemTrimmed.isNotEmpty()) {
                        tooltip += "$itemTrimmed<br>"
                    }
                }
                tooltip += "</html>"
                toolTipText = tooltip
            }
            return super.getToolTipText(event)
        }
    }

    inner class SearchPanel : JPanel() {
        val closeBtn: JButton = GButton("X") applyTooltip STRINGS.toolTip.searchCloseBtn
        val searchCombo: FilterComboBox = filterComboBox() applyTooltip STRINGS.toolTip.searchCombo
        val searchMatchCaseToggle: ColorToggleButton =
            ColorToggleButton("Aa") applyTooltip STRINGS.toolTip.searchCaseToggle
        var isInternalTargetView = true  // true : filter view, false : full view

        private var targetLabel: JLabel = if (isInternalTargetView) {
            JLabel("${STRINGS.ui.filter} ${STRINGS.ui.log}")
        } else {
            JLabel("${STRINGS.ui.full} ${STRINGS.ui.log}")
        } applyTooltip STRINGS.toolTip.searchTargetLabel
        private val upBtn: JButton =
            GButton(AllIcons.Arrow.Thick.Up.get()) applyTooltip STRINGS.toolTip.searchPrevBtn //△ ▲ ▽ ▼
        private val downBtn: JButton =
            GButton(AllIcons.Arrow.Thick.Down.get()) applyTooltip STRINGS.toolTip.searchNextBtn
        private val contentPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
        private val statusPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 2))

        private val searchActionHandler = SearchActionHandler()
        private val searchKeyHandler = SearchKeyHandler()
        private val searchPopupMenuHandler = SearchPopupMenuHandler()

        init {
            configureUI()
            registerEvent()
        }

        private fun configureUI() {
            searchCombo.enabledTfTooltip = false
            searchCombo.isEditable = true

            searchMatchCaseToggle.margin = Insets(0, 0, 0, 0)
            searchMatchCaseToggle.background = background
            searchMatchCaseToggle.border = BorderFactory.createEmptyBorder()

            upBtn.margin = Insets(0, 7, 0, 7)
            upBtn.background = background
            upBtn.border = BorderFactory.createEmptyBorder()

            downBtn.margin = Insets(0, 7, 0, 7)
            downBtn.background = background
            downBtn.border = BorderFactory.createEmptyBorder()

            closeBtn.margin = Insets(0, 0, 0, 0)
            closeBtn.background = background
            closeBtn.border = BorderFactory.createEmptyBorder()

            contentPanel.add(searchCombo)
            contentPanel.add(searchMatchCaseToggle)
            contentPanel.add(upBtn)
            contentPanel.add(downBtn)

            statusPanel.add(targetLabel)
            statusPanel.add(closeBtn)

            layout = BorderLayout()
            add(contentPanel, BorderLayout.WEST)
            add(statusPanel, BorderLayout.EAST)
        }

        private fun registerEvent() {
            registerComboBoxEditorEvent()
            searchCombo.addPopupMenuListener(searchPopupMenuHandler)
            searchMatchCaseToggle.addItemListener(SearchItemHandler())
            upBtn.addActionListener(searchActionHandler)
            downBtn.addActionListener(searchActionHandler)
            closeBtn.addActionListener(searchActionHandler)
        }

        fun registerComboBoxEditorEvent() {
            searchCombo.editorComponent.addKeyListener(searchKeyHandler)
        }

        override fun setVisible(aFlag: Boolean) {
            super.setVisible(aFlag)

            if (aFlag) {
                searchCombo.requestFocus()
                searchCombo.editor.selectAll()
            }
        }

        fun setTargetView(aFlag: Boolean) {
            isInternalTargetView = aFlag
            if (isInternalTargetView) {
                targetLabel.text = "${STRINGS.ui.filter} ${STRINGS.ui.log}"
            } else {
                targetLabel.text = "${STRINGS.ui.full} ${STRINGS.ui.log}"
            }
        }

        fun moveToNext() {
            if (isInternalTargetView) {
                filteredTableModel.moveToNextSearch()
            } else {
                fullTableModel.moveToNextSearch()
            }
        }

        fun moveToPrev() {
            if (isInternalTargetView) {
                filteredTableModel.moveToPrevSearch()
            } else {
                fullTableModel.moveToPrevSearch()
            }
        }

        private inner class SearchActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                when (event.source) {
                    upBtn -> {
                        moveToPrev()
                    }

                    downBtn -> {
                        moveToNext()
                    }

                    closeBtn -> {
                        contentPanel.isVisible = false
                    }
                }
            }
        }

        private inner class SearchKeyHandler : KeyAdapter() {
            override fun keyReleased(event: KeyEvent) {
                if (KeyEvent.VK_ENTER == event.keyCode) {
                    when (event.source) {
                        searchCombo.editorComponent -> {
                            resetComboItem(MainViewModel.searchHistory, MainViewModel.searchCurrentContent.value ?: "")
                            updateSearchFilter()
                            if (KeyEvent.SHIFT_DOWN_MASK == event.modifiersEx) {
                                moveToPrev()
                            } else {
                                moveToNext()
                            }
                        }
                    }
                }
                super.keyReleased(event)
            }
        }

        private inner class SearchPopupMenuHandler : PopupMenuListener {
            private var isCanceled = false
            override fun popupMenuWillBecomeInvisible(event: PopupMenuEvent) {
                if (isCanceled) {
                    isCanceled = false
                    return
                }
                when (event.source) {
                    searchCombo -> {
                        if (searchCombo.selectedIndex < 0) {
                            return
                        }
                        resetComboItem(MainViewModel.searchHistory, MainViewModel.searchCurrentContent.value ?: "")
                    }
                }
            }

            override fun popupMenuCanceled(event: PopupMenuEvent) {
                isCanceled = true
            }

            override fun popupMenuWillBecomeVisible(event: PopupMenuEvent) {
                isCanceled = false
            }
        }

        private inner class SearchItemHandler : ItemListener {
            override fun itemStateChanged(event: ItemEvent) {
                when (event.source) {
                    searchMatchCaseToggle -> {
                        filteredTableModel.searchMatchCase = searchMatchCaseToggle.isSelected
                        UIConfManager.uiConf.searchMatchCaseEnabled = searchMatchCaseToggle.isSelected
                    }
                }
            }
        }
    }

    fun showSearchResultTooltip(isNext: Boolean, result: String) {
        val targetPanel = if (searchPanel.isInternalTargetView) {
            splitLogPane.filteredLogPanel
        } else {
            splitLogPane.fullLogPanel
        } applyTooltip result

        if (isNext) {
            ToolTipManager.sharedInstance()
                .mouseMoved(MouseEvent(targetPanel, 0, 0, 0, targetPanel.width / 3, targetPanel.height - 50, 0, false))
        } else {
            ToolTipManager.sharedInstance()
                .mouseMoved(MouseEvent(targetPanel, 0, 0, 0, targetPanel.width / 3, 0, 0, false))
        }

        AppScope.launch(Dispatchers.UI) {
            delay(1000)
            targetPanel.toolTipText = ""
        }
    }

    override fun onError(error: Throwable) {
        JOptionPane.showMessageDialog(
            this,
            error,
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }

    override fun dispose() {
        ServiceManager.dispose(this)
        logProvider.stopCollectLog()
        logProvider.destroy()
        taskManager.cancelAll()
    }

    override fun onTabFocusChanged(focused: Boolean) {
        taskManager.updatePauseState(!focused)
    }

    companion object {
        private const val TAG = "LogMainUI"
    }
}
