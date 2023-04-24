package me.gegenbauer.logviewer.ui

import com.github.weisj.darklaf.components.OverlayScrollPane
import com.github.weisj.darklaf.iconset.AllIcons
import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.command.CmdManager
import me.gegenbauer.logviewer.command.LogCmdManager
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.logviewer.databinding.bind.withName
import me.gegenbauer.logviewer.databinding.property.support.DefaultDocumentListener
import me.gegenbauer.logviewer.file.Log
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.manager.FiltersManager
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.resource.strings.app
import me.gegenbauer.logviewer.ui.button.*
import me.gegenbauer.logviewer.ui.combobox.FilterComboBox
import me.gegenbauer.logviewer.ui.combobox.getFilterComboBox
import me.gegenbauer.logviewer.ui.container.WrapablePanel
import me.gegenbauer.logviewer.ui.dialog.GoToDialog
import me.gegenbauer.logviewer.ui.dialog.LogTableDialog
import me.gegenbauer.logviewer.ui.icon.DayNightIcon
import me.gegenbauer.logviewer.ui.log.LogPanel
import me.gegenbauer.logviewer.ui.log.LogTableModel
import me.gegenbauer.logviewer.ui.log.getLevelFromName
import me.gegenbauer.logviewer.ui.menu.FileMenu
import me.gegenbauer.logviewer.ui.menu.HelpMenu
import me.gegenbauer.logviewer.ui.menu.SettingsMenu
import me.gegenbauer.logviewer.ui.menu.ViewMenu
import me.gegenbauer.logviewer.ui.panel.SplitLogPane
import me.gegenbauer.logviewer.utils.applyTooltip
import me.gegenbauer.logviewer.utils.getEnum
import me.gegenbauer.logviewer.utils.loadIcon
import me.gegenbauer.logviewer.utils.loadIconWithRealSize
import me.gegenbauer.logviewer.viewmodel.MainViewModel
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
import javax.swing.plaf.basic.BasicScrollBarUI
import javax.swing.text.JTextComponent
import kotlin.system.exitProcess

class MainUI(title: String) : JFrame(title) {
    companion object {
        private const val TAG = "MainUI"

        const val DEFAULT_FONT_NAME = "DialogInput"

        const val CROSS_PLATFORM_LAF = "Cross Platform"
        const val SYSTEM_LAF = "System"
        const val FLAT_LIGHT_LAF = "Flat Light"
        const val FLAT_DARK_LAF = "Flat Dark"

        var IsCreatingUI = true
    }

    private val fullTableModel = LogTableModel(this, null)
    val filteredTableModel = LogTableModel(this, fullTableModel)

    private val fileMenu = FileMenu().apply {
        onFileSelected = { file ->
            openFile(file.absolutePath, false)
        }
        onFileFollowSelected = { file ->
            setFollowLogFile(file.absolutePath)
            startFileFollow()
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

    val splitLogPane = SplitLogPane(this, fullTableModel, filteredTableModel).apply {
        onFocusGained = {
            searchPanel.setTargetView(it)
        }
    }

    private val viewMenu = ViewMenu().apply {
        onItemFullClicked = {
            if (it) {
                attachLogPanel(splitLogPane.filteredLogPanel)
            } else {
                windowedModeLogPanel(splitLogPane.fullLogPanel)
            }

            UIConfManager.uiConf.logFullViewEnabled = itemFull.state
        }
        onItemSearchClicked = {
            searchPanel.isVisible = !searchPanel.isVisible
            itemSearch.state = searchPanel.isVisible
        }
        onItemRotationClicked = {
            UIConfManager.uiConf.rotation = splitLogPane.rotation.ordinal
            splitLogPane.rotate()
        }
    }
    private val settingsMenu = SettingsMenu().apply {
        onLogLevelChangedListener = {
            filteredTableModel.filterLevel = it
            UIConfManager.uiConf.logLevel = it.logName
        }
    }

    private val helpMenu = HelpMenu()

    private val menuBar = JMenuBar().apply {
        add(fileMenu)
        add(viewMenu)
        add(settingsMenu)
        add(this@MainUI.helpMenu)
    }
    private val filterPanel = JPanel()
    private val filterLeftPanel = JPanel()

    private val frameMouseListener = FrameMouseListener(this)
    private val keyHandler = KeyHandler()
    private val itemHandler = ItemHandler()
    private val actionHandler = ActionHandler()
    private val popupMenuHandler = PopupMenuHandler()
    private val mouseHandler = MouseHandler()
    private val statusChangeListener = StatusChangeListener()

    private val logToolBar = WrapablePanel()
    val startBtn = StatefulButton(loadIcon("start.svg", true), STRINGS.ui.start, STRINGS.toolTip.startBtn)
    val stopBtn = StatefulButton(loadIcon("stop.png"), STRINGS.ui.stop, STRINGS.toolTip.stopBtn)
    val saveBtn = StatefulButton(loadIcon("save.svg"), STRINGS.ui.save, STRINGS.toolTip.saveBtn)
    val clearViewsBtn = StatefulButton(loadIcon("clear.png"), STRINGS.ui.clearViews, STRINGS.toolTip.clearBtn)
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

    private val logPanel = JPanel()
    private val showLogPanel = JPanel()
    val matchCaseToggle = ColorToggleButton("Aa", STRINGS.toolTip.caseToggle)
    private val matchCaseTogglePanel = JPanel(GridLayout(1, 1))
    val showLogCombo = getFilterComboBox(tooltip = STRINGS.toolTip.logCombo) withName STRINGS.ui.log
    val showLogToggle = ColorToggleButton(STRINGS.ui.log, STRINGS.toolTip.logToggle)
    private val showLogTogglePanel = JPanel(GridLayout(1, 1))

    val showPidCombo = getFilterComboBox(useColorTag = false, tooltip = STRINGS.toolTip.pidCombo) withName STRINGS.ui.pid
    val showTagCombo = getFilterComboBox(useColorTag = false, tooltip = STRINGS.toolTip.tagCombo) withName STRINGS.ui.tag
    val showTidCombo = getFilterComboBox(useColorTag = false, tooltip = STRINGS.toolTip.tidCombo) withName STRINGS.ui.tid
    private var selectedLine = 0

    private val boldLogPanel = JPanel()
    val highlightLogCombo = getFilterComboBox(useColorTag = false, tooltip = STRINGS.toolTip.boldCombo)
    val boldLogToggle = ColorToggleButton(STRINGS.ui.bold, STRINGS.toolTip.boldToggle)
    private val boldLogTogglePanel = JPanel(GridLayout(1, 1))
    private val showTagPanel = JPanel()
    val showTagToggle = ColorToggleButton(STRINGS.ui.tag, STRINGS.toolTip.tagToggle)
    private val showTagTogglePanel = JPanel(GridLayout(1, 1))
    private val showPidPanel = JPanel()
    val showPidToggle = ColorToggleButton(STRINGS.ui.pid, STRINGS.toolTip.pidToggle)
    private val showPidTogglePanel = JPanel(GridLayout(1, 1))
    private val showTidPanel = JPanel()
    val showTidToggle = ColorToggleButton(STRINGS.ui.tid, STRINGS.toolTip.tidToggle)
    private val showTidTogglePanel = JPanel(GridLayout(1, 1))
    private val logCmdCombo = JComboBox<String>().apply { isEditable = true } applyTooltip STRINGS.toolTip.logCmdCombo
    val deviceCombo = JComboBox<String>().apply { isEditable = true } applyTooltip STRINGS.toolTip.devicesCombo
    private val deviceStatus = JLabel("None", JLabel.LEFT) // TODO 整理设备连接状态相关的代码
    val adbConnectBtn = StatefulButton(loadIcon("connect.png"), STRINGS.ui.connect, STRINGS.toolTip.connectBtn)
    val adbRefreshBtn = StatefulButton(loadIcon("refresh.png"), STRINGS.ui.refresh, STRINGS.toolTip.refreshBtn)
    val adbDisconnectBtn = StatefulButton(loadIcon("disconnect.png"), STRINGS.ui.disconnect, STRINGS.toolTip.disconnectBtn)
    val scrollBackLabel = StatefulLabel(loadIcon("scrollback.png"), STRINGS.ui.scrollBackLines)
    val scrollBackTF = JTextField(UIConfManager.uiConf.logScrollBackCount.toString()) applyTooltip STRINGS.toolTip.scrollBackTf
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
    private val statusBar = JPanel(BorderLayout())
    private val statusMethod = JLabel("")
    private val statusTF = StatusTextField(STRINGS.ui.none) applyTooltip STRINGS.toolTip.savedFileTf
    private val followLabel = JLabel(" ${STRINGS.ui.follow} ")
    private val startFollowBtn = JButton(STRINGS.ui.start) applyTooltip STRINGS.toolTip.startFollowBtn
    private val stopFollowBtn = JButton(STRINGS.ui.stop) applyTooltip STRINGS.toolTip.stopFollowBtn
    private val pauseFollowToggle = ColorToggleButton(STRINGS.ui.pause)
    private val toolBarPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

    val filtersManager = FiltersManager(this, splitLogPane.filteredLogPanel)
    val cmdManager = CmdManager(this, splitLogPane.fullLogPanel)

    var customFont: Font = Font(DEFAULT_FONT_NAME, Font.PLAIN, 12)
        set(value) {
            field = value
            if (!IsCreatingUI) {
                splitLogPane.filteredLogPanel.customFont = value
                splitLogPane.fullLogPanel.customFont = value
            }
        }

    var uiFontPercent = 100
    internal val searchPanel by lazy { SearchPanel() }

    init {
        LogCmdManager.setMainUI(this)

        configureWindow()

        if (UIConfManager.uiConf.rotation != 0) splitLogPane.rotate(getEnum(UIConfManager.uiConf.rotation))

        LogCmdManager.addEventListener(AdbHandler())

        createUI()

        registerEvent()

        MainViewModel.bind(this)

        if (LogCmdManager.getType() == LogCmdManager.TYPE_LOGCAT) {
            LogCmdManager.getDevices()
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
        exitProcess(0)
    }

    private fun saveConfigOnDestroy() {
        ConfigManager.loadConfig()

        UIConfManager.uiConf.frameX = location.x
        UIConfManager.uiConf.frameY = location.y
        UIConfManager.uiConf.frameWidth = size.width
        UIConfManager.uiConf.frameHeight = size.height
        UIConfManager.uiConf.frameExtendedState = extendedState
        UIConfManager.uiConf.lastDividerLocation = splitLogPane.lastDividerLocation
        UIConfManager.uiConf.dividerLocation = splitLogPane.dividerLocation
        UIConfManager.uiConf.logFilterHistory.clear()
        UIConfManager.uiConf.logFilterHistory.addAll(showLogCombo.getAllItems())
        UIConfManager.uiConf.tagFilterHistory.clear()
        UIConfManager.uiConf.tagFilterHistory.addAll(showTagCombo.getAllItems())
        UIConfManager.uiConf.highlightHistory.clear()
        UIConfManager.uiConf.highlightHistory.addAll(highlightLogCombo.getAllItems())
        UIConfManager.uiConf.searchHistory.clear()
        UIConfManager.uiConf.searchHistory.addAll(searchPanel.searchCombo.getAllItems())
        UIConfManager.uiConf.adbDevice = LogCmdManager.targetDevice
        UIConfManager.uiConf.adbLogCommand = logCmdCombo.editor.item.toString()
        UIConfManager.saveUI()

        ConfigManager.saveConfig()
    }

    private fun createUI() {
        jMenuBar = menuBar

        logToolBar.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)

        showLogTogglePanel.add(showLogToggle)
        showLogTogglePanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)

        highlightLogCombo.enabledTfTooltip = false
        boldLogTogglePanel.add(boldLogToggle)
        boldLogTogglePanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)

        showTagTogglePanel.add(showTagToggle)
        showTagTogglePanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)

        showPidTogglePanel.add(showPidToggle)
        showPidTogglePanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)

        showTidTogglePanel.add(showTidToggle)
        showTidTogglePanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)


        deviceStatus.isEnabled = false
        val deviceComboPanel = JPanel(BorderLayout())
        deviceComboPanel.add(deviceCombo, BorderLayout.CENTER)
        deviceComboPanel.add(deviceStatus, BorderLayout.LINE_END)

        matchCaseTogglePanel.add(matchCaseToggle)
        matchCaseTogglePanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)

        showLogPanel.layout = BorderLayout()
        showLogPanel.add(showLogTogglePanel, BorderLayout.WEST)
        showLogPanel.add(showLogCombo, BorderLayout.CENTER)

        boldLogPanel.layout = BorderLayout()
        boldLogPanel.add(boldLogTogglePanel, BorderLayout.WEST)
        boldLogPanel.add(highlightLogCombo, BorderLayout.CENTER)

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

        val itefilterPanel = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0))
        itefilterPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        itefilterPanel.add(showTagPanel)
        itefilterPanel.add(showPidPanel)
        itefilterPanel.add(showTidPanel)
        itefilterPanel.add(boldLogPanel)
        itefilterPanel.add(matchCaseTogglePanel)

        logPanel.layout = BorderLayout()
        logPanel.add(showLogPanel, BorderLayout.CENTER)
        logPanel.add(itefilterPanel, BorderLayout.EAST)

        filterLeftPanel.layout = BorderLayout()
        filterLeftPanel.add(logPanel, BorderLayout.NORTH)
        filterLeftPanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)

        filterPanel.layout = BorderLayout()
        filterPanel.add(filterLeftPanel, BorderLayout.CENTER)

        logToolBar.add(startBtn)
        logToolBar.add(retryAdbToggle)
        addVSeparator2(logToolBar)
        logToolBar.add(pauseToggle)
        logToolBar.add(stopBtn)
        logToolBar.add(saveBtn)

        addVSeparator(logToolBar)

        logToolBar.add(logCmdCombo)

        addVSeparator(logToolBar)

        logToolBar.add(deviceComboPanel)
        logToolBar.add(adbConnectBtn)
        logToolBar.add(adbDisconnectBtn)
        logToolBar.add(adbRefreshBtn)

        addVSeparator(logToolBar)

        logToolBar.add(clearViewsBtn)

        val scrollbackPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
        scrollbackPanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        addVSeparator(scrollbackPanel)
        scrollbackPanel.add(scrollBackLabel)
        scrollbackPanel.add(scrollBackTF)
        scrollbackPanel.add(scrollBackSplitFileToggle)
        scrollbackPanel.add(scrollBackApplyBtn)
        scrollbackPanel.add(scrollBackKeepToggle)

        toolBarPanel.layout = BorderLayout()
        toolBarPanel.add(logToolBar, BorderLayout.CENTER)
        toolBarPanel.add(scrollbackPanel, BorderLayout.EAST)

        filterPanel.add(toolBarPanel, BorderLayout.NORTH)
        filterPanel.add(searchPanel, BorderLayout.SOUTH)

        searchPanel.isVisible = false
        viewMenu.itemSearch.state = searchPanel.isVisible

        layout = BorderLayout()

        splitLogPane.fullLogPanel.updateTableBar(ArrayList(UIConfManager.uiConf.commands))
        splitLogPane.filteredLogPanel.updateTableBar(ArrayList(UIConfManager.uiConf.filters))

        if (UIConfManager.uiConf.dividerSize > 0) splitLogPane.dividerSize = UIConfManager.uiConf.dividerSize

        splitLogPane.isOneTouchExpandable = false

        statusBar.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        statusMethod.isOpaque = true
        statusMethod.background = Color.DARK_GRAY
        statusTF.isEditable = false
        statusTF.border = BorderFactory.createEmptyBorder()

        pauseFollowToggle.margin = Insets(pauseFollowToggle.margin.top, 0, pauseFollowToggle.margin.bottom, 0)

        val followPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
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

        highlightLogCombo.updateTooltip()

        searchPanel.searchCombo.updateTooltip()

        updateLogCmdCombo(true)

        val targetDevice = UIConfManager.uiConf.adbDevice
        deviceCombo.insertItemAt(targetDevice, 0)
        deviceCombo.selectedIndex = 0

        if (LogCmdManager.devices.contains(targetDevice)) {
            deviceStatus.text = STRINGS.ui.connected
            setDeviceComboColor(true)
        } else {
            deviceStatus.text = STRINGS.ui.notConnected
            setDeviceComboColor(false)
        }

        val fontName = UIConfManager.uiConf.logFontName.ifEmpty { DEFAULT_FONT_NAME }

        val fontSize = UIConfManager.uiConf.logFontSize.takeIf { it > 0 } ?: 12

        customFont = Font(fontName, Font.PLAIN, fontSize)
        splitLogPane.filteredLogPanel.customFont = customFont
        splitLogPane.fullLogPanel.customFont = customFont

        if (UIConfManager.uiConf.lastDividerLocation > 0) splitLogPane.lastDividerLocation =
            UIConfManager.uiConf.lastDividerLocation
        if (UIConfManager.uiConf.dividerLocation > 0) splitLogPane.dividerLocation =
            UIConfManager.uiConf.dividerLocation

        filteredTableModel.filterLevel = getLevelFromName(settingsMenu.logLevel)

        if (showLogToggle.isSelected && showLogCombo.selectedItem != null) {
            filteredTableModel.filterLog = showLogCombo.selectedItem!!.toString()
        } else {
            filteredTableModel.filterLog = ""
        }
        if (boldLogToggle.isSelected && highlightLogCombo.selectedItem != null) {
            filteredTableModel.filterHighlightLog = highlightLogCombo.selectedItem!!.toString()
        } else {
            filteredTableModel.filterHighlightLog = ""
        }
        if (searchPanel.isVisible && searchPanel.searchCombo.selectedItem != null) {
            filteredTableModel.filterSearchLog = searchPanel.searchCombo.selectedItem!!.toString()
        } else {
            filteredTableModel.filterSearchLog = ""
        }
        if (showTagToggle.isSelected && showTagCombo.selectedItem != null) {
            filteredTableModel.filterTag = showTagCombo.selectedItem!!.toString()
        } else {
            filteredTableModel.filterTag = ""
        }
        if (showPidToggle.isSelected && showPidCombo.selectedItem != null) {
            filteredTableModel.filterPid = showPidCombo.selectedItem!!.toString()
        } else {
            filteredTableModel.filterPid = ""
        }
        if (showTidToggle.isSelected && showTidCombo.selectedItem != null) {
            filteredTableModel.filterTid = showTidCombo.selectedItem!!.toString()
        } else {
            filteredTableModel.filterTid = ""
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

        add(filterPanel, BorderLayout.NORTH)
        add(splitLogPane, BorderLayout.CENTER)
        add(statusBar, BorderLayout.SOUTH)

        registerSearchStroke()

        IsCreatingUI = false
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
                    goToLine(goToDialog.line)
                } else {
                    GLog.d(TAG, "Cancel Goto Line")
                }
            }

            false
        }

        // fix bug: event registered for editor in ComboBox will be removed when ComboBoxUI changed
        registerComboBoxEditorEvent()

        logToolBar.addMouseListener(frameMouseListener)
        logToolBar.addMouseListener(mouseHandler)
        startBtn.addActionListener(actionHandler)
        startBtn.addMouseListener(mouseHandler)
        retryAdbToggle.addItemListener(itemHandler)
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
    }

    fun registerComboBoxEditorEvent() {
        showLogCombo.editor.editorComponent.addKeyListener(keyHandler)
        showLogCombo.editor.editorComponent.addMouseListener(mouseHandler)
        highlightLogCombo.editor.editorComponent.addKeyListener(keyHandler)
        highlightLogCombo.editor.editorComponent.addMouseListener(mouseHandler)
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
            if (evt.source == statusMethod && evt.propertyName == "text") {
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

    private fun addVSeparator(panel: JPanel) {
        val separator1 = JSeparator(SwingConstants.VERTICAL)
        separator1.preferredSize = Dimension(separator1.preferredSize.width, 20)
        val separator2 = JSeparator(SwingConstants.VERTICAL)
        separator2.preferredSize = Dimension(separator2.preferredSize.width, 20)
        panel.add(Box.createHorizontalStrut(5))
        panel.add(separator1)
        panel.add(separator2)
        panel.add(Box.createHorizontalStrut(5))
    }

    private fun addVSeparator2(panel: JPanel) {
        val separator1 = JSeparator(SwingConstants.VERTICAL)
        separator1.preferredSize = Dimension(separator1.preferredSize.width / 2, 20)
        panel.add(Box.createHorizontalStrut(2))
        panel.add(separator1)
        panel.add(Box.createHorizontalStrut(2))
    }

    fun windowedModeLogPanel(logPanel: LogPanel) {
        if (logPanel.parent == splitLogPane) {
            logPanel.isWindowedMode = true
            viewMenu.itemRotation.isEnabled = false
            splitLogPane.remove(logPanel)
            if (viewMenu.itemFull.state) {
                val logTableDialog = LogTableDialog(this@MainUI, logPanel)
                logTableDialog.isVisible = true
            }
        }
    }

    fun attachLogPanel(logPanel: LogPanel) {
        logPanel.isWindowedMode = false
        viewMenu.itemRotation.isEnabled = true
        splitLogPane.forceRotate()
    }

    fun openFile(path: String, isAppend: Boolean) {
        GLog.d(TAG, "Opening: $path, $isAppend")
        statusMethod.text = " ${STRINGS.ui.open} "
        filteredTableModel.stopScan()
        filteredTableModel.stopFollow()

        if (isAppend) {
            statusTF.text += "| $path"
        } else {
            statusTF.text = path
        }
        Log.file = File(path)
        fullTableModel.loadItems(isAppend)
        filteredTableModel.loadItems(isAppend)

        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x50, 0x50, 0x00)
        } else {
            statusMethod.background = Color(0xF0, 0xF0, 0x30)
        }
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

        Log.file = File(filePathSaved)
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
            LogCmdManager.targetDevice = deviceCombo.selectedItem!!.toString()
            LogCmdManager.startLogcat()
        }
        filteredTableModel.startScan()
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x00, 0x50, 0x00)
        } else {
            statusMethod.background = Color(0x90, 0xE0, 0x90)
        }

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
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x50, 0x50, 0x50)
        } else {
            statusMethod.background = Color.LIGHT_GRAY
        }

        enabledFollowBtn(true)
    }

    fun isRestartAdbLogcat(): Boolean {
        return retryAdbToggle.isSelected
    }

    fun restartAdbLogcat() {
        GLog.d(TAG, "Restart Adb Logcat")
        LogCmdManager.stop()
        LogCmdManager.targetDevice = deviceCombo.selectedItem!!.toString()
        LogCmdManager.startLogcat()
    }

    fun pauseAdbScan(pause: Boolean) {
        if (!filteredTableModel.isScanning()) {
            GLog.d(TAG, "pauseAdbScan : not adb scanning mode")
            return
        }
        filteredTableModel.pauseScan(pause)
    }

    fun setFollowLogFile(filePath: String) {
        Log.file = File(filePath)
        statusTF.text = filePath
    }

    fun startFileFollow() {
        statusMethod.text = " ${STRINGS.ui.follow} "
        filteredTableModel.stopScan()
        filteredTableModel.stopFollow()
        pauseFollowToggle.isSelected = false
        filteredTableModel.startFollow()

        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x00, 0x00, 0x50)
        } else {
            statusMethod.background = Color(0xA0, 0xA0, 0xF0)
        }

        enabledFollowBtn(true)
    }

    fun stopFileFollow() {
        if (!filteredTableModel.isFollowing()) {
            GLog.d(TAG, "stopAdbScan : not file follow mode")
            return
        }
        statusMethod.text = " ${STRINGS.ui.follow} ${STRINGS.ui.stop} "
        filteredTableModel.stopFollow()
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x50, 0x50, 0x50)
        } else {
            statusMethod.background = Color.LIGHT_GRAY
        }
        enabledFollowBtn(true)
    }

    fun pauseFileFollow(pause: Boolean) {
        if (!filteredTableModel.isFollowing()) {
            GLog.d(TAG, "pauseFileFollow : not file follow mode")
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
                    stopAdbScan()
                    LogCmdManager.targetDevice = deviceCombo.selectedItem!!.toString()
                    LogCmdManager.connect()
                }

                adbRefreshBtn -> {
                    LogCmdManager.getDevices()
                }

                adbDisconnectBtn -> {
                    stopAdbScan()
                    LogCmdManager.disconnect()
                }

                scrollBackApplyBtn -> {
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

                startBtn -> {
                    startAdbScan(true)
                }

                stopBtn -> {
                    stopAdbScan()
                    LogCmdManager.stop()
                }

                clearViewsBtn -> {
                    filteredTableModel.clearItems()
                    repaint()
                }

                saveBtn -> {
                    if (filteredTableModel.isScanning()) {
                        setSaveLogFile()
                    } else {
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

    internal inner class FramePopUp : JPopupMenu() {
        var itemIconText: JMenuItem = JMenuItem("IconText").apply { putClientProperty("ButtonDisplayMode", ButtonDisplayMode.ALL) }
        var itemIcon: JMenuItem = JMenuItem("Icon").apply { putClientProperty("ButtonDisplayMode", ButtonDisplayMode.ICON) }
        var itemText: JMenuItem = JMenuItem("Text").apply { putClientProperty("ButtonDisplayMode", ButtonDisplayMode.TEXT) }
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

    internal inner class FrameMouseListener(private val frame: JFrame) : MouseAdapter() {
        private var mouseDownCompCoords: Point? = null

        private var popupMenu: JPopupMenu? = null
        override fun mouseReleased(e: MouseEvent) {
            mouseDownCompCoords = null

            if (SwingUtilities.isRightMouseButton(e)) {
                popupMenu = FramePopUp()
                popupMenu?.show(e.component, e.x, e.y)
            } else {
                popupMenu?.isVisible = false
            }
        }

        override fun mousePressed(e: MouseEvent) {
            mouseDownCompCoords = e.point
        }

        override fun mouseDragged(e: MouseEvent) {
            val currCoords = e.locationOnScreen
            frame.setLocation(currCoords.x - mouseDownCompCoords!!.x, currCoords.y - mouseDownCompCoords!!.y)
        }
    }

    internal inner class PopUpCombobox(combo: JComboBox<String>?) : JPopupMenu() {
        val selectAllItem: JMenuItem = JMenuItem("Select All")
        val copyItem: JMenuItem = JMenuItem("Copy")
        val pasteItem: JMenuItem = JMenuItem("Paste")
        val reconnectItem: JMenuItem = JMenuItem("Reconnect " + deviceCombo.selectedItem?.toString())
        val combo: JComboBox<String>?
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
            this.combo = combo
        }

        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                when (event.source) {
                    selectAllItem -> {
                        combo?.editor?.selectAll()
                    }

                    copyItem -> {
                        val editorCom = combo?.editor?.editorComponent as JTextComponent
                        val stringSelection = StringSelection(editorCom.selectedText)
                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(stringSelection, null)
                    }

                    pasteItem -> {
                        val editorCom = combo?.editor?.editorComponent as JTextComponent
                        editorCom.paste()
                    }

                    reconnectItem -> {
                        reconnectAdb()
                    }
                }
            }
        }
    }

    internal inner class PopUpFilterCombobox(combo: FilterComboBox) : JPopupMenu() {
        var selectAllItem: JMenuItem
        var copyItem: JMenuItem
        var pasteItem: JMenuItem
        var removeColorTagsItem: JMenuItem
        lateinit var removeOneColorTagItem: JMenuItem
        lateinit var addColorTagItems: ArrayList<JMenuItem>
        var combo: FilterComboBox
        private val actionHandler = ActionHandler()

        init {
            this.combo = combo
            combo.background = Color.BLACK
            selectAllItem = JMenuItem("Select All")
            selectAllItem.addActionListener(actionHandler)
            add(selectAllItem)
            copyItem = JMenuItem("Copy")
            copyItem.addActionListener(actionHandler)
            add(copyItem)
            pasteItem = JMenuItem("Paste")
            pasteItem.addActionListener(actionHandler)
            add(pasteItem)
            removeColorTagsItem = JMenuItem("Remove All Color Tags")
            removeColorTagsItem.addActionListener(actionHandler)
            add(removeColorTagsItem)


            if (this.combo.useColorTag) {
                removeOneColorTagItem = JMenuItem("Remove Color Tag")
                removeOneColorTagItem.addActionListener(actionHandler)
                add(removeOneColorTagItem)
                addColorTagItems = arrayListOf()
                for (idx in 0..8) {
                    val num = idx + 1
                    val item = JMenuItem("Add Color Tag : #$num")
                    item.isOpaque = true
                    item.foreground = Color.decode(ColorManager.filterTableColor.strFilteredFGs[num])
                    item.background = Color.decode(ColorManager.filterTableColor.strFilteredBGs[num])
                    item.addActionListener(actionHandler)
                    addColorTagItems.add(item)
                    add(item)
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
                        val editorCom = combo.editor.editorComponent as JTextComponent
                        val stringSelection = StringSelection(editorCom.selectedText)
                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(stringSelection, null)
                    }

                    pasteItem -> {
                        val editorCom = combo.editor.editorComponent as JTextComponent
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
                            if (textSplit.size == 2) {
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
    }

    internal inner class MouseHandler : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            super.mouseClicked(event)
        }

        private var popupMenu: JPopupMenu? = null
        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                when (event.source) {
                    deviceCombo.editor.editorComponent -> {
                        popupMenu = PopUpCombobox(deviceCombo)
                        popupMenu?.show(event.component, event.x, event.y)
                    }

                    showLogCombo.editor.editorComponent, highlightLogCombo.editor.editorComponent, showTagCombo.editor.editorComponent, showPidCombo.editor.editorComponent, showTidCombo.editor.editorComponent -> {
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
        stopBtn.doClick()
        Thread.sleep(200)

        if (deviceCombo.selectedItem!!.toString().isNotBlank()) {
            adbConnectBtn.doClick()
            Thread.sleep(200)
        }

        Thread {
            run {
                Thread.sleep(200)
                clearViewsBtn.doClick()
                Thread.sleep(200)
                startBtn.doClick()
            }
        }.start()
    }

    fun startAdbLog() {
        Thread {
            run {
                startBtn.doClick()
            }
        }.start()
    }

    fun stopAdbLog() {
        stopBtn.doClick()
    }

    fun clearAdbLog() {
        Thread {
            run {
                clearViewsBtn.doClick()
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
        val editorCom = showLogCombo.editor.editorComponent as JTextComponent
        val text = editorCom.text
        setTextShowLogCombo(text)
        applyShowLogCombo()
    }

    fun setDeviceComboColor(isConnected: Boolean) {
        if (isConnected) {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                deviceCombo.editor.editorComponent.foreground = Color(0x7070C0)
            } else {
                deviceCombo.editor.editorComponent.foreground = Color.BLUE
            }
        } else {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                deviceCombo.editor.editorComponent.foreground = Color(0xC07070)
            } else {
                deviceCombo.editor.editorComponent.foreground = Color.RED
            }
        }
    }

    fun updateLogCmdCombo(isReload: Boolean) {
        if (isReload) {
            var logCmd: String?
            val currLogCmd = logCmdCombo.editor.item.toString()
            logCmdCombo.removeAllItems()
            for (i in 0 until LogCmdManager.LOG_CMD_MAX) {
                logCmd = ConfigManager.getItem("${ConfigManager.ITEM_ADB_LOG_CMD}_$i")
                if (logCmd.isNullOrBlank()) {
                    continue
                }

                logCmdCombo.addItem(logCmd)
            }
            logCmdCombo.selectedIndex = -1
            if (currLogCmd.isBlank()) {
                logCmdCombo.editor.item = LogCmdManager.logCmd
            } else {
                logCmdCombo.editor.item = currLogCmd
            }
        }

        logCmdCombo.toolTipText = "\"${LogCmdManager.logCmd}\"\n\n${STRINGS.toolTip.logCmdCombo}"

        if (LogCmdManager.logCmd == logCmdCombo.editor.item.toString()) {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                logCmdCombo.editor.editorComponent.foreground = Color(0x7070C0)
            } else {
                logCmdCombo.editor.editorComponent.foreground = Color.BLUE
            }
        } else {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                logCmdCombo.editor.editorComponent.foreground = Color(0xC07070)
            } else {
                logCmdCombo.editor.editorComponent.foreground = Color.RED
            }
        }
    }

    internal inner class KeyHandler : KeyAdapter() {
        override fun keyReleased(event: KeyEvent) {
            if (KeyEvent.VK_ENTER != event.keyCode && event.source == logCmdCombo.editor.editorComponent) {
                updateLogCmdCombo(false)
            }

            if (KeyEvent.VK_ENTER == event.keyCode) {
                when {
                    event.source == showLogCombo.editor.editorComponent && showLogToggle.isSelected -> {
                        resetComboItem(MainViewModel.logFilterHistory, MainViewModel.logFilterCurrentContent.value ?: "")
                        filteredTableModel.filterLog = MainViewModel.logFilterCurrentContent.value ?: ""
                    }

                    event.source == highlightLogCombo.editor.editorComponent && boldLogToggle.isSelected -> {
                        resetComboItem(MainViewModel.highlightHistory, MainViewModel.highlightCurrentContent.value ?: "")
                        filteredTableModel.filterHighlightLog = MainViewModel.highlightCurrentContent.value ?: ""
                    }

                    event.source == showTagCombo.editor.editorComponent && showTagToggle.isSelected -> {
                        resetComboItem(MainViewModel.tagFilterHistory, MainViewModel.tagFilterCurrentContent.value ?: "")
                        filteredTableModel.filterTag = MainViewModel.tagFilterCurrentContent.value ?: ""
                    }

                    event.source == showPidCombo.editor.editorComponent && showPidToggle.isSelected -> {
                        resetComboItem(MainViewModel.pidFilterHistory, MainViewModel.pidFilterCurrentContent.value ?: "")
                        filteredTableModel.filterPid = MainViewModel.pidFilterCurrentContent.value ?: ""
                    }

                    event.source == showTidCombo.editor.editorComponent && showTidToggle.isSelected -> {
                        resetComboItem(MainViewModel.tidFilterHistory, MainViewModel.tidFilterCurrentContent.value ?: "")
                        filteredTableModel.filterTid = MainViewModel.tidFilterCurrentContent.value ?: ""
                    }

                    event.source == logCmdCombo.editor.editorComponent -> {
                        if (LogCmdManager.logCmd == logCmdCombo.editor.item.toString()) {
                            reconnectAdb()
                        } else {
                            val item = logCmdCombo.editor.item.toString().trim()

                            if (item.isEmpty()) {
                                logCmdCombo.editor.item = LogCmdManager.DEFAULT_LOGCAT
                            }
                            LogCmdManager.logCmd = logCmdCombo.editor.item.toString()
                            updateLogCmdCombo(false)
                        }
                    }

                    event.source == deviceCombo.editor.editorComponent -> {
                        reconnectAdb()
                    }

                    event.source == scrollBackTF -> {
                        scrollBackApplyBtn.doClick()
                    }
                }
            } else if (settingsMenu.filterIncremental) {
                when {
                    event.source == showLogCombo.editor.editorComponent && showLogToggle.isSelected -> {
                        val item = showLogCombo.editor.item.toString()
                        filteredTableModel.filterLog = item
                    }

                    event.source == highlightLogCombo.editor.editorComponent && boldLogToggle.isSelected -> {
                        val item = highlightLogCombo.editor.item.toString()
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
            if (IsCreatingUI) {
                return
            }
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
                    if (boldLogToggle.isSelected && highlightLogCombo.selectedItem != null) {
                        filteredTableModel.filterHighlightLog = highlightLogCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterHighlightLog = ""
                    }
                    UIConfManager.uiConf.highlightEnabled = boldLogToggle.isSelected
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

                retryAdbToggle -> {
                    ConfigManager.saveItem(ConfigManager.ITEM_RETRY_ADB, retryAdbToggle.isSelected.toString())
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
                    LogCmdManager.getDevices()
                }

                LogCmdManager.CMD_GET_DEVICES -> {
                    if (IsCreatingUI) {
                        return
                    }
                    var selectedItem = deviceCombo.selectedItem
                    MainViewModel.connectedDevices.updateValue(LogCmdManager.devices)
                    if (selectedItem == null) {
                        selectedItem = ""
                    }

                    if (LogCmdManager.devices.contains(selectedItem.toString())) {
                        deviceStatus.text = STRINGS.ui.connected
                        setDeviceComboColor(true)
                    } else {
                        var isExist = false
                        val deviceChk = "$selectedItem:"
                        for (device in LogCmdManager.devices) {
                            if (device.contains(deviceChk)) {
                                isExist = true
                                break
                            }
                        }
                        if (isExist) {
                            deviceStatus.text = STRINGS.ui.connected
                            setDeviceComboColor(true)
                        } else {
                            deviceStatus.text = STRINGS.ui.notConnected
                            setDeviceComboColor(false)
                        }
                    }
                }

                LogCmdManager.CMD_DISCONNECT -> {
                    LogCmdManager.getDevices()
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

                highlightLogCombo -> {
                    if (highlightLogCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = highlightLogCombo
                    resetComboItem(MainViewModel.highlightHistory, MainViewModel.highlightCurrentContent.value ?: "")
                    filteredTableModel.filterHighlightLog = MainViewModel.highlightCurrentContent.value ?: ""
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
                    val item = combo.selectedItem!!.toString()
                    LogCmdManager.logCmd = item
                    updateLogCmdCombo(false)
                }
            }
        }

        override fun popupMenuCanceled(event: PopupMenuEvent) {
            isCanceled = true
        }

        override fun popupMenuWillBecomeVisible(event: PopupMenuEvent) {
            val box = event.source as JComboBox<*>
            val comp = box.ui.getAccessibleChild(box, 0) as? JPopupMenu ?: return
            val scrollPane = comp.getComponent(0) as OverlayScrollPane
            scrollPane.verticalScrollBar?.setUI(BasicScrollBarUI())
            scrollPane.horizontalScrollBar?.setUI(BasicScrollBarUI())
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
        if (IsCreatingUI) {
            return
        }
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
        val closeBtn: JButton = JButton("X") applyTooltip STRINGS.toolTip.searchCloseBtn
        val searchCombo: FilterComboBox = getFilterComboBox(useColorTag = false) applyTooltip STRINGS.toolTip.searchCombo
        val searchMatchCaseToggle: ColorToggleButton = ColorToggleButton("Aa") applyTooltip STRINGS.toolTip.searchCaseToggle
        var isInternalTargetView = true  // true : filter view, false : full view

        private var targetLabel: JLabel = if (isInternalTargetView) {
            JLabel("${STRINGS.ui.filter} ${STRINGS.ui.log}")
        } else {
            JLabel("${STRINGS.ui.full} ${STRINGS.ui.log}")
        } applyTooltip STRINGS.toolTip.searchTargetLabel
        private val upBtn: JButton = JButton(AllIcons.Arrow.Thick.Up.get()) applyTooltip STRINGS.toolTip.searchPrevBtn //△ ▲ ▽ ▼
        private val downBtn: JButton = JButton(AllIcons.Arrow.Thick.Down.get()) applyTooltip STRINGS.toolTip.searchNextBtn
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

            if (!IsCreatingUI) {
                if (aFlag) {
                    searchCombo.requestFocus()
                    searchCombo.editor.selectAll()

                    filteredTableModel.filterSearchLog = searchCombo.selectedItem!!.toString()
                } else {
                    filteredTableModel.filterSearchLog = ""
                }
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
                val box = event.source as JComboBox<*>
                val comp = box.ui.getAccessibleChild(box, 0) as? JPopupMenu ?: return
                val scrollPane = comp.getComponent(0) as JScrollPane
                scrollPane.verticalScrollBar?.setUI(BasicScrollBarUI())
                scrollPane.horizontalScrollBar?.setUI(BasicScrollBarUI())
                isCanceled = false
            }
        }

        internal inner class SearchItemHandler : ItemListener {
            override fun itemStateChanged(event: ItemEvent) {
                if (IsCreatingUI) {
                    return
                }
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
                searchPanel.isVisible = false
                viewMenu.itemSearch.state = searchPanel.isVisible
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)
        actionMapKey = javaClass.name + ":SEARCH_OPENING"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                searchPanel.isVisible = true
                viewMenu.itemSearch.state = searchPanel.isVisible
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



