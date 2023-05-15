package me.gegenbauer.catspy.ui

import com.github.weisj.darklaf.iconset.AllIcons
import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import com.github.weisj.darklaf.settings.ThemeSettings
import com.github.weisj.darklaf.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.command.LogCmdManager
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.databinding.bind.withName
import me.gegenbauer.catspy.databinding.property.support.DefaultDocumentListener
import me.gegenbauer.catspy.databinding.property.support.PROPERTY_TEXT
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.task.*
import me.gegenbauer.catspy.ui.button.*
import me.gegenbauer.catspy.ui.combobox.FilterComboBox
import me.gegenbauer.catspy.ui.combobox.darkComboBox
import me.gegenbauer.catspy.ui.combobox.filterComboBox
import me.gegenbauer.catspy.ui.container.WrapablePanel
import me.gegenbauer.catspy.ui.dialog.GoToDialog
import me.gegenbauer.catspy.ui.dialog.LogTableDialog
import me.gegenbauer.catspy.ui.icon.DayNightIcon
import me.gegenbauer.catspy.ui.log.*
import me.gegenbauer.catspy.ui.menu.FileMenu
import me.gegenbauer.catspy.ui.menu.HelpMenu
import me.gegenbauer.catspy.ui.menu.SettingsMenu
import me.gegenbauer.catspy.ui.menu.ViewMenu
import me.gegenbauer.catspy.ui.panel.SplitLogPane
import me.gegenbauer.catspy.ui.panel.next
import me.gegenbauer.catspy.ui.state.EmptyStatePanel
import me.gegenbauer.catspy.utils.*
import me.gegenbauer.catspy.viewmodel.MainViewModel
import java.awt.*
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.text.JTextComponent
import kotlin.system.exitProcess

class MainUI(title: String) : JFrame(title), TaskListener {
    companion object {
        private const val TAG = "MainUI"
    }

    //region filterPanel
    private val filterPanel = JPanel()

    //region filterLeftPanel
    private val filterLeftPanel = JPanel() withName "filterLeftPanel"

    //region logPanel
    private val logPanel = JPanel()

    //region showLogPanel
    private val showLogPanel = JPanel()

    private val showLogTogglePanel = JPanel(GridLayout(1, 1))
    val showLogToggle = ColorToggleButton(STRINGS.ui.log, STRINGS.toolTip.logToggle)

    val showLogCombo = filterComboBox(tooltip = STRINGS.toolTip.logCombo) withName STRINGS.ui.log
    //endregion

    //region itemFilterPanel
    private val itemFilterPanel = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0))

    private val showTagPanel = JPanel()
    private val showTagTogglePanel = JPanel(GridLayout(1, 1))
    val showTagToggle = ColorToggleButton(STRINGS.ui.tag, STRINGS.toolTip.tagToggle)
    val showTagCombo = filterComboBox(useColorTag = false, tooltip = STRINGS.toolTip.tagCombo) withName STRINGS.ui.tag

    private val showPidPanel = JPanel()
    private val showPidTogglePanel = JPanel(GridLayout(1, 1))
    val showPidToggle = ColorToggleButton(STRINGS.ui.pid, STRINGS.toolTip.pidToggle)
    val showPidCombo = filterComboBox(useColorTag = false, tooltip = STRINGS.toolTip.pidCombo) withName STRINGS.ui.pid

    private val showTidPanel = JPanel()
    private val showTidTogglePanel = JPanel(GridLayout(1, 1))
    val showTidToggle = ColorToggleButton(STRINGS.ui.tid, STRINGS.toolTip.tidToggle)
    val showTidCombo = filterComboBox(useColorTag = false, tooltip = STRINGS.toolTip.tidCombo) withName STRINGS.ui.tid

    private val boldLogPanel = JPanel()
    private val boldLogTogglePanel = JPanel(GridLayout(1, 1))
    val boldLogToggle = ColorToggleButton(STRINGS.ui.bold, STRINGS.toolTip.boldToggle)
    val boldLogCombo = filterComboBox(useColorTag = false, tooltip = STRINGS.toolTip.boldCombo)

    private val matchCaseTogglePanel = JPanel(GridLayout(1, 1))
    val matchCaseToggle = ColorToggleButton("Aa", STRINGS.toolTip.caseToggle)
    //endregion
    //endregion
    //endregion

    //region toolBarPanel
    private val toolBarPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

    //region logToolBar
    private val logToolBar = WrapablePanel() withName "logToolBar"

    val startBtn = StatefulButton(loadThemedIcon("start.svg"), STRINGS.ui.start, STRINGS.toolTip.startBtn)
    val retryAdbToggle = StatefulToggleButton(
        loadIcon("retry_off.png"),
        DayNightIcon(loadIcon("retry_on.png"), loadIcon("retry_on_dark.png")),
        STRINGS.ui.retryAdb,
        tooltip = STRINGS.toolTip.retryAdbToggle
    )
    val pauseToggle = StatefulToggleButton(
        loadIcon("pause_off.png"),
        DayNightIcon(loadIcon("pause_on.png"), loadIcon("pause_on_dark.png")),
        STRINGS.ui.pause,
        tooltip = STRINGS.toolTip.pauseBtn
    )
    val stopBtn = StatefulButton(loadIcon("stop.png"), STRINGS.ui.stop, STRINGS.toolTip.stopBtn)
    val saveBtn = StatefulButton(loadIcon("save.svg"), STRINGS.ui.save, STRINGS.toolTip.saveBtn)
    val logCmdCombo = darkComboBox(STRINGS.toolTip.logCmdCombo)
    val deviceCombo = darkComboBox(STRINGS.toolTip.devicesCombo)
    private val deviceStatus = JLabel("None", JLabel.LEFT) // TODO 整理设备连接状态相关的代码
    val adbConnectBtn = StatefulButton(loadIcon("connect.png"), STRINGS.ui.connect, STRINGS.toolTip.connectBtn)
    val adbDisconnectBtn =
        StatefulButton(loadIcon("disconnect.png"), STRINGS.ui.disconnect, STRINGS.toolTip.disconnectBtn)
    val adbRefreshBtn = StatefulButton(loadIcon("refresh.png"), STRINGS.ui.refresh, STRINGS.toolTip.refreshBtn)
    val clearViewsBtn = StatefulButton(loadIcon("clear.png"), STRINGS.ui.clearViews, STRINGS.toolTip.clearBtn)
    //endregion

    //region scrollBackPanel
    private val scrollBackPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))

    val scrollBackLabel = StatefulLabel(loadIcon("scrollback.png"), STRINGS.ui.scrollBackLines)
    val scrollBackTF =
        JTextField(UIConfManager.uiConf.logScrollBackCount.toString()) applyTooltip STRINGS.toolTip.scrollBackTf
    val scrollBackSplitFileToggle = StatefulToggleButton(
        loadIcon("splitfile_off.png"),
        DayNightIcon(loadIcon("splitfile_on.png"), loadIcon("splitfile_on_dark.png")),
        STRINGS.ui.splitFile,
        loadIcon("toggle_on_warn.png"),
        tooltip = STRINGS.toolTip.scrollBackSplitChk
    )
    val scrollBackApplyBtn = StatefulButton(loadIcon("apply.png"), STRINGS.ui.apply, STRINGS.toolTip.scrollBackApplyBtn)
    val scrollBackKeepToggle = StatefulToggleButton(
        loadIcon("keeplog_off.png"),
        DayNightIcon(loadIcon("keeplog_on.png"), loadIcon("keeplog_on_dark.png")),
        STRINGS.ui.keep,
        loadIcon("toggle_on_warn.png"),
        tooltip = STRINGS.toolTip.scrollBackKeepToggle
    )
    //endregion
    //endregion

    //region searchPanel
    internal val searchPanel by lazy { SearchPanel() }
    //endregion
    //endregion

    //region splitLogPane
    private val splitLogWithEmptyStatePanel = EmptyStatePanel()

    private val fullTableModel = LogTableModel(this, null)
    val filteredTableModel = LogTableModel(this, fullTableModel)
    val splitLogPane = SplitLogPane(this, fullTableModel, filteredTableModel).apply {
        onFocusGained = {
            searchPanel.setTargetView(it)
        }
    }
    //endregion

    //region statusBar
    private val statusBar = JPanel(BorderLayout())

    private val statusMethod = JLabel("")
    private val statusTF = StatusTextField(STRINGS.ui.none) applyTooltip STRINGS.toolTip.savedFileTf

    private val followPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))

    private val followLabel = JLabel(" ${STRINGS.ui.follow} ")
    private val startFollowBtn = GButton(STRINGS.ui.start) applyTooltip STRINGS.toolTip.startFollowBtn
    private val pauseFollowToggle = ColorToggleButton(STRINGS.ui.pause)
    private val stopFollowBtn = GButton(STRINGS.ui.stop) applyTooltip STRINGS.toolTip.stopFollowBtn
    //endregion

    //region menu
    private val fileMenu = FileMenu().apply {
        onFileSelected = { file ->
            openFile(file.absolutePath, false)
        }
        onFileFollowSelected = { file ->
            setFollowLogFile(file.absolutePath)
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
        onExit = ::exit
    }
    val viewMenu = ViewMenu().apply {
        onItemRotationClicked = {
            MainViewModel.rotation.value?.let {
                MainViewModel.rotation.updateValue(it.next())
            }
        }
    }
    val settingsMenu = SettingsMenu().apply {
        onLogLevelChangedListener = {
            filteredTableModel.filterLevel = it
            MainViewModel.logLevel.updateValue(it.logName)
        }
    }
    private val helpMenu = HelpMenu()
    private val menuBar = JMenuBar().apply {
        add(fileMenu)
        add(viewMenu)
        add(settingsMenu)
        add(this@MainUI.helpMenu)
    }
    //endregion

    //region events
    private val logPanelMouseListener = LogPanelMouseListener()
    private val keyHandler = KeyHandler()
    private val itemHandler = ItemHandler()
    private val actionHandler = ActionHandler()
    private val popupMenuHandler = PopupMenuHandler()
    private val mouseHandler = MouseHandler()
    private val statusChangeListener = StatusChangeListener()

    //endregion
    private val taskManager = TaskManager()
    private val getDeviceTask = GetDeviceTask()
    private val getDeviceTaskPeriodicTask = PeriodicTask(1000) { taskManager.exec(getDeviceTask) }
    private var selectedLine = 0

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

    init {
        LogCmdManager.setMainUI(this)
        configureWindow()

        LogCmdManager.addEventListener(AdbHandler())

        createUI()

        registerEvent()

        MainViewModel.bind(this)

        taskManager.exec(getDeviceTaskPeriodicTask)
        getDeviceTask.addListener(this)
    }

    override fun onFinalResult(task: Task, data: Any) {
        super.onFinalResult(task, data)
        if (task is GetDeviceTask) {
            MainViewModel.connectedDevices.updateValue(data as ArrayList<String>)
        }
    }

    private fun configureWindow() {
        iconImage = loadIconWithRealSize<DerivableImageIcon>("logo.png").image
        defaultCloseOperation = EXIT_ON_CLOSE

        UIConfManager.uiConf.run {
            extendedState = if (frameX == 0 || frameY == 0 || frameWidth == 0 || frameHeight == 0) {
                MAXIMIZED_BOTH
            } else {
                frameExtendedState
            }
            if (frameX != 0 && frameY != 0) {
                setLocation(frameX, frameY)
            }
            if (frameWidth != 0 && frameHeight != 0) {
                setSize(frameWidth, frameHeight)
            }
        }
    }

    private fun exit() {
        saveConfigOnDestroy()
        filteredTableModel.stopScan()
        fullTableModel.stopScan()
        LogCmdManager.stop()
        taskManager.cancelAll()
        exitProcess(0)
    }

    private fun saveConfigOnDestroy() {
        UIConfManager.uiConf.frameX = location.x
        UIConfManager.uiConf.frameY = location.y
        UIConfManager.uiConf.frameWidth = size.width
        UIConfManager.uiConf.frameHeight = size.height
        UIConfManager.uiConf.frameExtendedState = extendedState
        UIConfManager.uiConf.adbDevice = LogCmdManager.targetDevice
        UIConfManager.uiConf.adbLogCommand = logCmdCombo.editor.item.toString()
        UIConfManager.uiConf.logFontSize = customFont.size
        UIConfManager.uiConf.logFontName = customFont.name
        UIConfManager.uiConf.logFontStyle = customFont.style
        UIConfManager.saveUI()
    }

    private fun createUI() {
        jMenuBar = menuBar

        boldLogCombo.enabledTfTooltip = false
        showLogTogglePanel.add(showLogToggle)
        boldLogTogglePanel.add(boldLogToggle)
        showTagTogglePanel.add(showTagToggle)
        showPidTogglePanel.add(showPidToggle)
        showTidTogglePanel.add(showTidToggle)

        deviceStatus.isEnabled = false
        val deviceComboPanel = JPanel(BorderLayout())
        deviceComboPanel.add(deviceCombo, BorderLayout.CENTER)
        deviceComboPanel.add(deviceStatus, BorderLayout.LINE_END)

        matchCaseTogglePanel.add(matchCaseToggle)

        showLogPanel.layout = BorderLayout()
        showLogPanel.add(showLogTogglePanel, BorderLayout.WEST)
        showLogPanel.add(showLogCombo, BorderLayout.CENTER)

        boldLogPanel.layout = BorderLayout()
        boldLogPanel.add(boldLogTogglePanel, BorderLayout.WEST)
        boldLogPanel.add(boldLogCombo, BorderLayout.CENTER)

        showTagPanel.layout = BorderLayout()
        showTagPanel.add(showTagTogglePanel, BorderLayout.WEST)
        showTagPanel.add(showTagCombo, BorderLayout.CENTER)

        showPidPanel.layout = BorderLayout()
        showPidPanel.add(showPidTogglePanel, BorderLayout.WEST)
        showPidPanel.add(showPidCombo, BorderLayout.CENTER)

        showTidPanel.layout = BorderLayout()
        showTidPanel.add(showTidTogglePanel, BorderLayout.WEST)
        showTidPanel.add(showTidCombo, BorderLayout.CENTER)

        logCmdCombo.minimumSize = Dimension(200, 15)
        deviceCombo.minimumSize = Dimension(200, 15)
        deviceStatus.minimumSize = Dimension(100, 15)
        deviceStatus.border = BorderFactory.createEmptyBorder(3, 0, 3, 0)
        deviceStatus.horizontalAlignment = JLabel.CENTER

        scrollBackTF.preferredSize = Dimension(80, scrollBackTF.preferredSize.height)

        itemFilterPanel.add(showTagPanel)
        itemFilterPanel.add(showPidPanel)
        itemFilterPanel.add(showTidPanel)
        itemFilterPanel.add(boldLogPanel)
        itemFilterPanel.add(matchCaseTogglePanel)

        logPanel.layout = BorderLayout()
        logPanel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        logPanel.add(showLogPanel, BorderLayout.CENTER)
        logPanel.add(itemFilterPanel, BorderLayout.EAST)

        filterLeftPanel.layout = BorderLayout()
        filterLeftPanel.add(logPanel, BorderLayout.NORTH)

        filterPanel.layout = BorderLayout()
        filterPanel.add(filterLeftPanel, BorderLayout.CENTER)

        logToolBar.add(startBtn)
        logToolBar.add(retryAdbToggle)
        logToolBar.addVSeparator2()
        logToolBar.add(pauseToggle)
        logToolBar.add(stopBtn)
        logToolBar.add(saveBtn)
        logToolBar.addVSeparator2()
        logToolBar.add(logCmdCombo)
        logToolBar.addVSeparator2()
        logToolBar.add(deviceComboPanel)
        logToolBar.add(adbConnectBtn)
        logToolBar.add(adbDisconnectBtn)
        logToolBar.add(adbRefreshBtn)
        logToolBar.addVSeparator2()
        logToolBar.add(clearViewsBtn)

        scrollBackPanel.addVSeparator2()
        scrollBackPanel.add(scrollBackLabel)
        scrollBackPanel.add(scrollBackTF)
        scrollBackPanel.add(scrollBackSplitFileToggle)
        scrollBackPanel.add(scrollBackApplyBtn)
        scrollBackPanel.add(scrollBackKeepToggle)

        toolBarPanel.layout = BorderLayout()
        toolBarPanel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        toolBarPanel.add(logToolBar, BorderLayout.CENTER)
        toolBarPanel.add(scrollBackPanel, BorderLayout.EAST)

        filterPanel.add(toolBarPanel, BorderLayout.NORTH)
        filterPanel.add(searchPanel, BorderLayout.SOUTH)

        layout = BorderLayout()

        splitLogPane.fullLogPanel.updateTableBar(ArrayList(UIConfManager.uiConf.commands))
        splitLogPane.filteredLogPanel.updateTableBar(ArrayList(UIConfManager.uiConf.filters))

        splitLogPane.isOneTouchExpandable = false

        statusBar.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        statusMethod.isOpaque = true
        statusMethod.background = Color.DARK_GRAY
        statusTF.isEditable = false
        statusTF.border = BorderFactory.createEmptyBorder()

        followPanel.border = BorderFactory.createEmptyBorder(0, 3, 0, 3)
        followLabel.border = BorderFactory.createDashedBorder(null, 1.0f, 2.0f)
        followPanel.add(followLabel)
        followPanel.add(startFollowBtn)
        followPanel.add(pauseFollowToggle)
        followPanel.add(stopFollowBtn)

        enabledFollowBtn(false)

        statusBar.add(statusMethod, BorderLayout.WEST)
        statusBar.add(statusTF, BorderLayout.CENTER)
        statusBar.add(followPanel, BorderLayout.EAST)

        showLogCombo.updateTooltip()
        showTagCombo.updateTooltip()
        boldLogCombo.updateTooltip()
        searchPanel.searchCombo.updateTooltip()

        updateLogCmdCombo()

        val targetDevice = UIConfManager.uiConf.adbDevice
        deviceCombo.insertItemAt(targetDevice, 0)
        deviceCombo.selectedIndex = 0

        deviceStatus.text = STRINGS.ui.connected
        setDeviceComboColor(true)

        customFont = UIConfManager.uiConf.getLogFont()
        GLog.d(TAG, "[createUI] log font: $customFont")
        splitLogPane.filteredLogPanel.customFont = customFont
        splitLogPane.fullLogPanel.customFont = customFont

        filteredTableModel.filterLevel = getLevelFromName(MainViewModel.logLevel.value ?: LogLevel.WARN.logName)

        if (showLogToggle.isSelected) {
            filteredTableModel.filterLog = showLogCombo.selectedItem?.toString() ?: ""
        }
        if (boldLogToggle.isSelected) {
            filteredTableModel.filterHighlightLog = boldLogCombo.selectedItem?.toString() ?: ""
        }
        if (searchPanel.isVisible) {
            filteredTableModel.filterSearchLog = searchPanel.searchCombo.selectedItem?.toString() ?: ""
        }
        if (showTagToggle.isSelected) {
            filteredTableModel.filterTag = showTagCombo.selectedItem?.toString() ?: ""
        }
        if (showPidToggle.isSelected) {
            filteredTableModel.filterPid = showPidCombo.selectedItem?.toString() ?: ""
        }
        if (showTidToggle.isSelected) {
            filteredTableModel.filterTid = showTidCombo.selectedItem?.toString() ?: ""
        }

        viewMenu.itemFull.state = UIConfManager.uiConf.logFullViewEnabled
        if (!viewMenu.itemFull.state) {
            windowedModeLogPanel(splitLogPane.fullLogPanel)
        }

        filteredTableModel.scrollback = UIConfManager.uiConf.logScrollBackCount
        scrollBackSplitFileToggle.isSelected = UIConfManager.uiConf.logScrollBackSplitFileEnabled
        filteredTableModel.scrollBackSplitFile = UIConfManager.uiConf.logScrollBackSplitFileEnabled
        filteredTableModel.matchCase = UIConfManager.uiConf.filterMatchCaseEnabled

        searchPanel.searchMatchCaseToggle.isSelected = UIConfManager.uiConf.searchMatchCaseEnabled
        filteredTableModel.searchMatchCase = UIConfManager.uiConf.searchMatchCaseEnabled

        splitLogWithEmptyStatePanel.setContent(splitLogPane)
        splitLogWithEmptyStatePanel.action = fileMenu::onClickFileOpen

        add(filterPanel, BorderLayout.NORTH)
        add(splitLogWithEmptyStatePanel, BorderLayout.CENTER)
        add(statusBar, BorderLayout.SOUTH)

        registerSearchStroke()
    }

    private fun registerEvent() {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                exit()
            }
        })

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { event ->
            if (event.keyCode == KeyEvent.VK_PAGE_DOWN && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0) {
                splitLogPane.filteredLogPanel.goToLast()
                splitLogPane.fullLogPanel.goToLast()
            } else if (event.keyCode == KeyEvent.VK_PAGE_UP && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0) {
                splitLogPane.filteredLogPanel.goToFirst()
                splitLogPane.fullLogPanel.goToFirst()
            } else if (event.keyCode == KeyEvent.VK_L && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0) {
                deviceCombo.requestFocus()
            } else if (event.keyCode == KeyEvent.VK_R && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0) {
                reconnectAdb()
            } else if (event.keyCode == KeyEvent.VK_G && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0) {
                val goToDialog = GoToDialog(this@MainUI)
                goToDialog.setLocationRelativeTo(this@MainUI)
                goToDialog.isVisible = true
                if (goToDialog.line != -1) {
                    GLog.d(TAG, "[KeyEventDispatcher] Cancel Goto Line ${goToDialog.line}")
                    goToLine(goToDialog.line)
                } else {
                    GLog.d(TAG, "[KeyEventDispatcher] Cancel Goto Line")
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
        pauseToggle.addItemListener(itemHandler)
        stopBtn.addActionListener(actionHandler)
        stopBtn.addMouseListener(mouseHandler)
        clearViewsBtn.addActionListener(actionHandler)
        clearViewsBtn.addMouseListener(mouseHandler)
        saveBtn.addActionListener(actionHandler)
        saveBtn.addMouseListener(mouseHandler)
        showLogCombo.addPopupMenuListener(popupMenuHandler)
        showLogToggle.addItemListener(itemHandler)
        boldLogToggle.addItemListener(itemHandler)
        showTagToggle.addItemListener(itemHandler)
        showPidToggle.addItemListener(itemHandler)
        showTidToggle.addItemListener(itemHandler)
        logCmdCombo.addPopupMenuListener(popupMenuHandler)
        adbConnectBtn.addActionListener(actionHandler)
        adbRefreshBtn.addActionListener(actionHandler)
        adbDisconnectBtn.addActionListener(actionHandler)
        matchCaseToggle.addItemListener(itemHandler)
        scrollBackApplyBtn.addActionListener(actionHandler)
        scrollBackKeepToggle.addItemListener(itemHandler)
        scrollBackTF.addKeyListener(keyHandler)
        scrollBackSplitFileToggle.addItemListener(itemHandler)
        filterPanel.addMouseListener(mouseHandler)
        toolBarPanel.addMouseListener(mouseHandler)
        statusMethod.addPropertyChangeListener(statusChangeListener)
        statusTF.document.addDocumentListener(statusChangeListener)
        startFollowBtn.addActionListener(actionHandler)
        startFollowBtn.addMouseListener(mouseHandler)
        pauseFollowToggle.addItemListener(itemHandler)
        stopFollowBtn.addActionListener(actionHandler)
        stopFollowBtn.addMouseListener(mouseHandler)

        fullTableModel.addLogTableModelListener { event ->
            if (event.dataChange == LogTableModelEvent.EVENT_CHANGED) {
                splitLogWithEmptyStatePanel.contentVisible = true
            } else if (event.dataChange == LogTableModelEvent.EVENT_CLEARED) {
                splitLogWithEmptyStatePanel.contentVisible = false
            }
        }
        filteredTableModel.addLogTableModelListener { event ->
            if (event.dataChange == LogTableModelEvent.EVENT_CHANGED) {
                splitLogWithEmptyStatePanel.contentVisible = true
            } else if (event.dataChange == LogTableModelEvent.EVENT_CLEARED) {
                splitLogWithEmptyStatePanel.contentVisible = false
            }
        }
    }

    fun registerComboBoxEditorEvent() {
        showLogCombo.editor.editorComponent.addKeyListener(keyHandler)
        showLogCombo.editor.editorComponent.addMouseListener(mouseHandler)
        boldLogCombo.editor.editorComponent.addKeyListener(keyHandler)
        boldLogCombo.editor.editorComponent.addMouseListener(mouseHandler)
        showTagCombo.editor.editorComponent.addKeyListener(keyHandler)
        showTagCombo.editor.editorComponent.addMouseListener(mouseHandler)
        showPidCombo.editor.editorComponent.addKeyListener(keyHandler)
        showPidCombo.editor.editorComponent.addMouseListener(mouseHandler)
        showTidCombo.editor.editorComponent.addKeyListener(keyHandler)
        showTidCombo.editor.editorComponent.addMouseListener(mouseHandler)
        logCmdCombo.editor.editorComponent.addKeyListener(keyHandler)
        logCmdCombo.editor.editorComponent.addMouseListener(mouseHandler)
        deviceCombo.editor.editorComponent.addKeyListener(keyHandler)
        deviceCombo.editor.editorComponent.addMouseListener(mouseHandler)
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

    private fun updateTitleBar(statusMethod: String) {
        title = when (statusMethod) {
            STRINGS.ui.open, STRINGS.ui.follow, "${STRINGS.ui.follow} ${STRINGS.ui.stop}" -> {
                val path: Path = Paths.get(statusTF.text)
                path.fileName.toString()
            }

            STRINGS.ui.adb, STRINGS.ui.cmd, "${STRINGS.ui.adb} ${STRINGS.ui.stop}", "${STRINGS.ui.cmd} ${STRINGS.ui.stop}" -> {
                LogCmdManager.targetDevice.ifEmpty { STRINGS.ui.app }
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
            viewMenu.itemRotation.isEnabled = false
            splitLogPane.remove(logPanel)
            SwingUtilities.updateComponentTreeUI(splitLogPane)
        }
    }

    fun windowedModeLogPanel(logPanel: FullLogPanel) {
        detachLogPanel(logPanel)
        if (viewMenu.itemFull.state) {
            val logTableDialog = LogTableDialog(this@MainUI, logPanel)
            logTableDialog.isVisible = true
        }
    }

    fun attachLogPanel(logPanel: FullLogPanel) {
        logPanel.isWindowedMode = false
        viewMenu.itemRotation.isEnabled = true
        splitLogPane.resetWithCurrentRotation()
    }

    fun openFile(path: String, isAppend: Boolean) {
        GLog.d(TAG, "[openFile] Opening: $path, $isAppend")
        statusMethod.text = " ${STRINGS.ui.open} "
        filteredTableModel.stopScan()
        filteredTableModel.stopFollow()

        if (isAppend) {
            statusTF.text += "| $path"
        } else {
            statusTF.text = path
        }
        val file = File(path)
        fullTableModel.loadFromFile(file, isAppend)
        filteredTableModel.loadFromFile(file, isAppend)

        enabledFollowBtn(true)

        repaint()

        return
    }

    fun setSaveLogFile() {
        val dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HH.mm.ss")
        var device = deviceCombo.selectedItem?.toString() ?: ""
        device = device.substringBefore(":")
        if (LogCmdManager.prefix.isEmpty()) {
            LogCmdManager.prefix = STRINGS.ui.app
        }

        val filePath =
            "${LogCmdManager.logSavePath}/${LogCmdManager.prefix}_${device}_${dtf.format(LocalDateTime.now())}.txt"
        var file = File(filePath)
        var idx = 1
        var filePathSaved = filePath
        while (file.isFile) {
            filePathSaved = "${filePath}-$idx.txt"
            file = File(filePathSaved)
            idx++
        }

        statusTF.text = filePathSaved
    }

    fun startAdbScan(reconnect: Boolean) {
        if (LogCmdManager.getType() == LogCmdManager.TYPE_CMD) {
            statusMethod.text = " ${STRINGS.ui.cmd} "
        } else {
            statusMethod.text = " ${STRINGS.ui.adb} "
        }

        filteredTableModel.stopScan()
        filteredTableModel.stopFollow()
        pauseToggle.isSelected = false
        setSaveLogFile()
        if (reconnect) {
            LogCmdManager.targetDevice = deviceCombo.selectedItem?.toString() ?: ""
        }
        filteredTableModel.startScan()

        enabledFollowBtn(false)
    }

    fun stopAdbScan() {
        if (LogCmdManager.getType() == LogCmdManager.TYPE_CMD) {
            statusMethod.text = " ${STRINGS.ui.cmd} ${STRINGS.ui.stop} "
        } else {
            statusMethod.text = " ${STRINGS.ui.adb} ${STRINGS.ui.stop} "
        }

        if (!filteredTableModel.isScanning()) {
            GLog.d(TAG, "stopAdbScan : not adb scanning mode")
            return
        }
        filteredTableModel.stopScan()

        enabledFollowBtn(true)
    }

    fun isRestartAdbLogcat(): Boolean {
        return MainViewModel.retryAdb.value ?: false
    }

    fun restartAdbLogcat() {
        GLog.d(TAG, "[restartAdbLogcat]")
        LogCmdManager.targetDevice = deviceCombo.selectedItem!!.toString()
    }

    fun pauseAdbScan(pause: Boolean) {
        if (!filteredTableModel.isScanning()) {
            GLog.d(TAG, "[pauseAdbScan] not adb scanning mode")
            return
        }
        filteredTableModel.pauseScan(pause)
    }

    fun setFollowLogFile(filePath: String) {
        statusTF.text = filePath
    }

    fun startFileFollow(filePath: String = "") {
        statusMethod.text = " ${STRINGS.ui.follow} "
        filteredTableModel.stopScan()
        filteredTableModel.stopFollow()
        pauseFollowToggle.isSelected = false
        filteredTableModel.startFollow(if (filePath.isEmpty()) null else File(filePath))

        enabledFollowBtn(true)
    }

    fun stopFileFollow() {
        if (!filteredTableModel.isFollowing()) {
            GLog.d(TAG, "[stopAdbScan] not file follow mode")
            return
        }
        statusMethod.text = " ${STRINGS.ui.follow} ${STRINGS.ui.stop} "
        filteredTableModel.stopFollow()
        enabledFollowBtn(true)
    }

    fun pauseFileFollow(pause: Boolean) {
        if (!filteredTableModel.isFollowing()) {
            GLog.d(TAG, "[pauseFileFollow] not file follow mode")
            return
        }
        filteredTableModel.pauseFollow(pause)
    }

    private fun enabledFollowBtn(enabled: Boolean) {
        followLabel.isEnabled = enabled
        startFollowBtn.isEnabled = enabled
        pauseFollowToggle.isEnabled = enabled
        stopFollowBtn.isEnabled = enabled
    }

    internal inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                adbConnectBtn -> {
                    connect()
                }

                adbDisconnectBtn -> {
                    stopAdbScan()
                    LogCmdManager.disconnect()
                }

                scrollBackApplyBtn -> {
                    applyScrollBack()
                }

                startBtn -> {
                    startAdbScan(true)
                }

                stopBtn -> {
                    stopScan()
                }

                clearViewsBtn -> {
                    clearViews()
                }

                saveBtn -> {
                    if (filteredTableModel.isScanning()) {
                        // TODO Save As
                    } else {
                        // TODO Disable Save button
                        GLog.d(TAG, "SaveBtn : not adb scanning mode")
                    }
                }

                startFollowBtn -> {
                    startFileFollow()
                }

                stopFollowBtn -> {
                    stopFileFollow()
                }
            }
        }
    }

    private fun applyScrollBack() {
        try {
            filteredTableModel.scrollback = scrollBackTF.text.toString().trim().toInt()
        } catch (e: java.lang.NumberFormatException) {
            filteredTableModel.scrollback = 0
            scrollBackTF.text = "0"
        }
        filteredTableModel.scrollBackSplitFile = scrollBackSplitFileToggle.isSelected
        UIConfManager.uiConf.logScrollBackCount = scrollBackTF.text.toInt()
        UIConfManager.uiConf.logScrollBackSplitFileEnabled = scrollBackSplitFileToggle.isSelected
    }

    private fun connect() {
        stopAdbScan()
        LogCmdManager.targetDevice = deviceCombo.selectedItem!!.toString()
        LogCmdManager.connect()
    }

    private fun stopScan() {
        stopAdbScan()
        LogCmdManager.stop()
    }

    private fun clearViews() {
        filteredTableModel.clearItems()
        repaint()
    }

    internal inner class ButtonDisplayModeSelectMenu : JPopupMenu() {
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

        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                MainViewModel.buttonDisplayMode.updateValue(
                    (event.source as JComponent).getClientProperty("ButtonDisplayMode") as ButtonDisplayMode
                )
            }
        }
    }

    internal inner class LogPanelMouseListener : MouseAdapter() {
        private val popupMenu: JPopupMenu = ButtonDisplayModeSelectMenu()

        override fun mouseReleased(e: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(e)) {
                popupMenu.show(e.component, e.x, e.y)
            } else {
                popupMenu.isVisible = false
            }
        }
    }

    internal inner class PopUpCombobox(private val combo: JComboBox<String>) : JPopupMenu() {
        private val selectAllItem: JMenuItem = JMenuItem("Select All")
        private val copyItem: JMenuItem = JMenuItem("Copy")
        private val pasteItem: JMenuItem = JMenuItem("Paste")
        private val reconnectItem: JMenuItem = JMenuItem("Reconnect " + deviceCombo.selectedItem?.toString())
        private val actionHandler = ActionHandler()

        init {
            selectAllItem.addActionListener(actionHandler)
            add(selectAllItem)
            copyItem.addActionListener(actionHandler)
            add(copyItem)
            pasteItem.addActionListener(actionHandler)
            add(pasteItem)
            reconnectItem.addActionListener(actionHandler)
            add(reconnectItem)
        }

        internal inner class ActionHandler : ActionListener {
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

    internal inner class PopUpFilterCombobox(private val combo: FilterComboBox) : JPopupMenu() {
        private val selectAllItem = JMenuItem("Select All")
        private val copyItem = JMenuItem("Copy").apply {
            icon = loadDarklafThemedIcon("menu/copy.svg")
        }
        private val pasteItem = JMenuItem("Paste").apply {
            icon = loadDarklafThemedIcon("menu/paste.svg")
        }
        private val removeColorTagsItem = JMenuItem("Remove All Color Tags")
        private val removeOneColorTagItem = JMenuItem("Remove Color Tag")
        private val addColorTagItems: ArrayList<JMenuItem> = arrayListOf()
        private val actionHandler = ActionHandler()

        init {
            selectAllItem.addActionListener(actionHandler)
            copyItem.addActionListener(actionHandler)
            pasteItem.addActionListener(actionHandler)
            removeColorTagsItem.addActionListener(actionHandler)
            removeOneColorTagItem.addActionListener(actionHandler)

            updateMenuItems()
            ThemeManager.registerThemeUpdateListener { updateMenuItems() }
        }

        private fun updateMenuItems() {
            removeAll()
            add(selectAllItem)
            add(copyItem)
            add(pasteItem)
            add(removeColorTagsItem)
            if (this.combo.useColorTag) {
                add(removeOneColorTagItem)
                addColorTagItems.clear()
                val colorTagIndexes = 1..9
                colorTagIndexes.map {
                    JMenuItem("Add Color Tag : #$it").apply {
                        isOpaque = true
                        foreground = ColorScheme.filteredFGs[it]
                        background = ColorScheme.filteredBGs[it]
                        addActionListener(actionHandler)
                    }
                }.forEach {
                    addColorTagItems.add(it)
                    add(it)
                }
            }
        }

        internal inner class ActionHandler : ActionListener {
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

                    removeColorTagsItem -> {
                        combo.removeAllColorTags()
                        if (combo == showLogCombo) {
                            applyShowLogComboEditor()
                        }
                    }

                    removeOneColorTagItem -> {
                        combo.removeColorTag()
                        if (combo == showLogCombo) {
                            applyShowLogComboEditor()
                        }
                    }

                    else -> {
                        val item = event.source as JMenuItem
                        if (addColorTagItems.contains(item)) {
                            val textSplit = item.text.split(":")
                            combo.addColorTag(textSplit[1].trim())
                            if (combo == showLogCombo) {
                                applyShowLogComboEditor()
                            }
                        }
                    }
                }
            }
        }
    }

    internal inner class MouseHandler : MouseAdapter() {
        private var popupMenu: JPopupMenu? = null

        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                when (event.source) {
                    deviceCombo.editor.editorComponent -> {
                        popupMenu = PopUpCombobox(deviceCombo)
                        popupMenu?.show(event.component, event.x, event.y)
                    }

                    showLogCombo.editor.editorComponent, boldLogCombo.editor.editorComponent, showTagCombo.editor.editorComponent, showPidCombo.editor.editorComponent, showTidCombo.editor.editorComponent -> {
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
            run {
                Thread.sleep(200)
                clearViews()
                Thread.sleep(200)
                startAdbScan(true)
            }
        }.start()
    }

    fun startAdbLog() {
        Thread {
            run {
                startAdbScan(true)
            }
        }.start()
    }

    fun stopAdbLog() {
        stopScan()
    }

    fun clearAdbLog() {
        Thread {
            run {
                clearViews()
            }
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

    fun getTextSearchCombo(): String {
        if (searchPanel.searchCombo.selectedItem == null) {
            return ""
        }
        return searchPanel.searchCombo.selectedItem!!.toString()
    }

    fun setTextSearchCombo(text: String) {
        searchPanel.searchCombo.selectedItem = text
        filteredTableModel.filterSearchLog = searchPanel.searchCombo.selectedItem!!.toString()
        searchPanel.isVisible = true
        viewMenu.itemSearch.state = searchPanel.isVisible
    }

    fun applyShowLogCombo() {
        resetComboItem(MainViewModel.logFilterHistory, MainViewModel.logFilterCurrentContent.value ?: "")
        filteredTableModel.filterLog = MainViewModel.logFilterCurrentContent.value ?: ""
    }

    fun applyShowLogComboEditor() {
        val editorCom = showLogCombo.editorComponent
        val text = editorCom.text
        setTextShowLogCombo(text)
        applyShowLogCombo()
    }

    fun setDeviceComboColor(isConnected: Boolean) {
        if (isConnected) {
            if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                deviceCombo.editor.editorComponent.foreground = Color(0x7070C0)
            } else {
                deviceCombo.editor.editorComponent.foreground = Color.BLUE
            }
        } else {
            if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                deviceCombo.editor.editorComponent.foreground = Color(0xC07070)
            } else {
                deviceCombo.editor.editorComponent.foreground = Color.RED
            }
        }
    }

    fun updateLogCmdCombo() {
        logCmdCombo.toolTipText = "\"${LogCmdManager.logCmd}\"\n\n${STRINGS.toolTip.logCmdCombo}"

        if (LogCmdManager.logCmd == logCmdCombo.editor.item.toString()) {
            if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                logCmdCombo.editor.editorComponent.foreground = Color(0x7070C0)
            } else {
                logCmdCombo.editor.editorComponent.foreground = Color.BLUE
            }
        } else {
            if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                logCmdCombo.editor.editorComponent.foreground = Color(0xC07070)
            } else {
                logCmdCombo.editor.editorComponent.foreground = Color.RED
            }
        }
    }

    internal inner class KeyHandler : KeyAdapter() {
        override fun keyReleased(event: KeyEvent) {
            if (KeyEvent.VK_ENTER != event.keyCode && event.source == logCmdCombo.editor.editorComponent) {
                updateLogCmdCombo()
            }

            if (KeyEvent.VK_ENTER == event.keyCode) {
                when {
                    event.source == showLogCombo.editor.editorComponent && showLogToggle.isSelected -> {
                        resetComboItem(
                            MainViewModel.logFilterHistory,
                            MainViewModel.logFilterCurrentContent.value ?: ""
                        )
                        filteredTableModel.filterLog = MainViewModel.logFilterCurrentContent.value ?: ""
                    }

                    event.source == boldLogCombo.editor.editorComponent && boldLogToggle.isSelected -> {
                        resetComboItem(MainViewModel.boldHistory, MainViewModel.boldCurrentContent.value ?: "")
                        filteredTableModel.filterHighlightLog = MainViewModel.boldCurrentContent.value ?: ""
                    }

                    event.source == showTagCombo.editor.editorComponent && showTagToggle.isSelected -> {
                        resetComboItem(
                            MainViewModel.tagFilterHistory,
                            MainViewModel.tagFilterCurrentContent.value ?: ""
                        )
                        filteredTableModel.filterTag = MainViewModel.tagFilterCurrentContent.value ?: ""
                    }

                    event.source == showPidCombo.editor.editorComponent && showPidToggle.isSelected -> {
                        resetComboItem(
                            MainViewModel.pidFilterHistory,
                            MainViewModel.pidFilterCurrentContent.value ?: ""
                        )
                        filteredTableModel.filterPid = MainViewModel.pidFilterCurrentContent.value ?: ""
                    }

                    event.source == showTidCombo.editor.editorComponent && showTidToggle.isSelected -> {
                        resetComboItem(
                            MainViewModel.tidFilterHistory,
                            MainViewModel.tidFilterCurrentContent.value ?: ""
                        )
                        filteredTableModel.filterTid = MainViewModel.tidFilterCurrentContent.value ?: ""
                    }

                    event.source == logCmdCombo.editor.editorComponent -> {
                        if (LogCmdManager.logCmd == logCmdCombo.editor.item.toString()) {
                            reconnectAdb()
                        } else {
                            resetComboItem(
                                MainViewModel.logCmdHistory,
                                MainViewModel.logCmdCurrentContent.value ?: ""
                            )
                            LogCmdManager.logCmd = logCmdCombo.editor.item.toString()
                            updateLogCmdCombo()
                        }
                    }

                    event.source == deviceCombo.editor.editorComponent -> {
                        reconnectAdb()
                    }

                    event.source == scrollBackTF -> {
                        applyScrollBack()
                    }
                }
            } else if (settingsMenu.filterIncremental) {
                when {
                    event.source == showLogCombo.editor.editorComponent && showLogToggle.isSelected -> {
                        val item = showLogCombo.editor.item.toString()
                        filteredTableModel.filterLog = item
                    }

                    event.source == boldLogCombo.editor.editorComponent && boldLogToggle.isSelected -> {
                        val item = boldLogCombo.editor.item.toString()
                        filteredTableModel.filterHighlightLog = item
                    }

                    event.source == showTagCombo.editor.editorComponent && showTagToggle.isSelected -> {
                        val item = showTagCombo.editor.item.toString()
                        filteredTableModel.filterTag = item
                    }

                    event.source == showPidCombo.editor.editorComponent && showPidToggle.isSelected -> {
                        val item = showPidCombo.editor.item.toString()
                        filteredTableModel.filterPid = item
                    }

                    event.source == showTidCombo.editor.editorComponent && showTidToggle.isSelected -> {
                        val item = showTidCombo.editor.item.toString()
                        filteredTableModel.filterTid = item
                    }
                }
            }
            super.keyReleased(event)
        }
    }

    internal inner class ItemHandler : ItemListener {
        override fun itemStateChanged(event: ItemEvent) {
            when (event.source) {
                showLogToggle -> {
                    if (showLogToggle.isSelected && showLogCombo.selectedItem != null) {
                        filteredTableModel.filterLog = showLogCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterLog = ""
                    }
                    UIConfManager.uiConf.logFilterEnabled = showLogToggle.isSelected
                }

                boldLogToggle -> {
                    if (boldLogToggle.isSelected && boldLogCombo.selectedItem != null) {
                        filteredTableModel.filterHighlightLog = boldLogCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterHighlightLog = ""
                    }
                    UIConfManager.uiConf.boldEnabled = boldLogToggle.isSelected
                }

                showTagToggle -> {
                    if (showTagToggle.isSelected && showTagCombo.selectedItem != null) {
                        filteredTableModel.filterTag = showTagCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterTag = ""
                    }
                    UIConfManager.uiConf.tagFilterEnabled = showTagToggle.isSelected
                }

                showPidToggle -> {
                    if (showPidToggle.isSelected && showPidCombo.selectedItem != null) {
                        filteredTableModel.filterPid = showPidCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterPid = ""
                    }
                    UIConfManager.uiConf.pidFilterEnabled = showPidToggle.isSelected
                }

                showTidToggle -> {
                    if (showTidToggle.isSelected && showTidCombo.selectedItem != null) {
                        filteredTableModel.filterTid = showTidCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterTid = ""
                    }
                    UIConfManager.uiConf.tidFilterEnabled = showTidToggle.isSelected
                }

                matchCaseToggle -> {
                    filteredTableModel.matchCase = matchCaseToggle.isSelected
                    UIConfManager.uiConf.filterMatchCaseEnabled = matchCaseToggle.isSelected
                }

                scrollBackKeepToggle -> {
                    filteredTableModel.scrollBackKeep = scrollBackKeepToggle.isSelected
                }

                pauseToggle -> {
                    pauseAdbScan(pauseToggle.isSelected)
                }

                pauseFollowToggle -> {
                    pauseFileFollow(pauseFollowToggle.isSelected)
                }
            }
        }
    }

    internal inner class AdbHandler : LogCmdManager.AdbEventListener {
        override fun changedStatus(event: LogCmdManager.AdbEvent) {
            when (event.cmd) {
                LogCmdManager.CMD_CONNECT -> {

                }

                LogCmdManager.CMD_DISCONNECT -> {

                }
            }
        }
    }

    internal inner class PopupMenuHandler : PopupMenuListener {
        private var isCanceled = false
        override fun popupMenuWillBecomeInvisible(event: PopupMenuEvent) {
            if (isCanceled) {
                isCanceled = false
                return
            }
            when (event.source) {
                showLogCombo -> {
                    if (showLogCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = showLogCombo
                    val item = combo.selectedItem!!.toString()
                    if (combo.editor.item.toString() != item) {
                        return
                    }
                    resetComboItem(MainViewModel.logFilterHistory, MainViewModel.logFilterCurrentContent.value ?: "")
                    filteredTableModel.filterLog = MainViewModel.logFilterCurrentContent.value ?: ""
                    combo.updateTooltip()
                }

                boldLogCombo -> {
                    if (boldLogCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = boldLogCombo
                    resetComboItem(MainViewModel.boldHistory, MainViewModel.boldCurrentContent.value ?: "")
                    filteredTableModel.filterHighlightLog = MainViewModel.boldCurrentContent.value ?: ""
                    combo.updateTooltip()
                }

                showTagCombo -> {
                    if (showTagCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = showTagCombo
                    resetComboItem(MainViewModel.tagFilterHistory, MainViewModel.tagFilterCurrentContent.value ?: "")
                    filteredTableModel.filterTag = MainViewModel.tagFilterCurrentContent.value ?: ""
                    combo.updateTooltip()
                }

                showPidCombo -> {
                    if (showPidCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = showPidCombo
                    resetComboItem(MainViewModel.pidFilterHistory, MainViewModel.pidFilterCurrentContent.value ?: "")
                    filteredTableModel.filterPid = MainViewModel.pidFilterCurrentContent.value ?: ""
                    combo.updateTooltip()
                }

                showTidCombo -> {
                    if (showTidCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = showTidCombo
                    resetComboItem(MainViewModel.tidFilterHistory, MainViewModel.tidFilterCurrentContent.value ?: "")
                    filteredTableModel.filterTid = MainViewModel.tidFilterCurrentContent.value ?: ""
                    combo.updateTooltip()
                }

                logCmdCombo -> {
                    if (logCmdCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = logCmdCombo
                    resetComboItem(MainViewModel.logCmdHistory, MainViewModel.logCmdCurrentContent.value ?: "")
                    val item = combo.selectedItem!!.toString()
                    LogCmdManager.logCmd = item
                    updateLogCmdCombo()
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

    fun <T> resetComboItem(viewModelProperty: ObservableViewModelProperty<List<T>>, item: T) {
        val list = viewModelProperty.value
        list ?: return
        if (list.contains(item)) {
            return
        }
        viewModelProperty.updateValue(ArrayList(list).apply {
            add(0, item)
        })
        return
    }

    fun goToLine(line: Int) {
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

    fun markLine() {
        selectedLine = splitLogPane.filteredLogPanel.getSelectedLine()
    }

    fun getMarkLine(): Int {
        return selectedLine
    }

    internal inner class StatusTextField(text: String?) : JTextField(text) {
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
        val searchCombo: FilterComboBox = filterComboBox(useColorTag = false) applyTooltip STRINGS.toolTip.searchCombo
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
            searchCombo.editor.editorComponent.addKeyListener(searchKeyHandler)
        }

        override fun setVisible(aFlag: Boolean) {
            super.setVisible(aFlag)

            if (aFlag) {
                searchCombo.requestFocus()
                searchCombo.editor.selectAll()

                filteredTableModel.filterSearchLog = searchCombo.selectedItem?.toString() ?: ""
            } else {
                filteredTableModel.filterSearchLog = ""
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

        internal inner class SearchActionHandler : ActionListener {
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
                        viewMenu.itemSearch.state = contentPanel.isVisible
                    }
                }
            }
        }

        internal inner class SearchKeyHandler : KeyAdapter() {
            override fun keyReleased(event: KeyEvent) {
                if (KeyEvent.VK_ENTER == event.keyCode) {
                    when (event.source) {
                        searchCombo.editor.editorComponent -> {
                            resetComboItem(MainViewModel.searchHistory, MainViewModel.searchCurrentContent.value ?: "")
                            filteredTableModel.filterSearchLog = MainViewModel.searchCurrentContent.value ?: ""
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

        internal inner class SearchPopupMenuHandler : PopupMenuListener {
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
                        filteredTableModel.filterSearchLog = MainViewModel.searchCurrentContent.value ?: ""
                        searchCombo.updateTooltip()
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

        internal inner class SearchItemHandler : ItemListener {
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

    private fun registerSearchStroke() {
        var stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
        var actionMapKey = javaClass.name + ":SEARCH_CLOSING"
        var action: Action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                MainViewModel.searchPanelVisible.updateValue(false)
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)
        actionMapKey = javaClass.name + ":SEARCH_OPENING"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                MainViewModel.searchPanelVisible.updateValue(true)
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)
        actionMapKey = javaClass.name + ":SEARCH_MOVE_PREV"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                if (searchPanel.isVisible) {
                    searchPanel.moveToPrev()
                }
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0)
        actionMapKey = javaClass.name + ":SEARCH_MOVE_NEXT"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                if (searchPanel.isVisible) {
                    searchPanel.moveToNext()
                }
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)
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
}



