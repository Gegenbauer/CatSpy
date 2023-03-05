package me.gegenbauer.logviewer.ui

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import me.gegenbauer.logviewer.GoToDialog
import me.gegenbauer.logviewer.NAME
import me.gegenbauer.logviewer.manager.*
import me.gegenbauer.logviewer.strings.Strings
import me.gegenbauer.logviewer.strings.TooltipStrings
import me.gegenbauer.logviewer.ui.about.AboutDialog
import me.gegenbauer.logviewer.ui.button.*
import me.gegenbauer.logviewer.ui.help.HelpDialog
import me.gegenbauer.logviewer.ui.log.LogCmdSettingsDialog
import me.gegenbauer.logviewer.ui.log.LogPanel
import me.gegenbauer.logviewer.ui.log.LogTableDialog
import me.gegenbauer.logviewer.ui.log.LogTableModel
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_DEBUG
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_ERROR
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_FATAL
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_INFO
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_VERBOSE
import me.gegenbauer.logviewer.ui.log.LogTableModel.Companion.LEVEL_WARNING
import me.gegenbauer.logviewer.ui.settings.AppearanceSettingsDialog
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
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.FontUIResource
import javax.swing.plaf.basic.BasicScrollBarUI
import javax.swing.text.JTextComponent
import kotlin.math.roundToInt
import kotlin.system.exitProcess


class MainUI(title: String) : JFrame() {
    companion object {
        private const val SPLIT_WEIGHT = 0.7

        private const val ROTATION_LEFT_RIGHT = 0
        private const val ROTATION_TOP_BOTTOM = 1
        private const val ROTATION_RIGHT_LEFT = 2
        private const val ROTATION_BOTTOM_TOP = 3
        private const val ROTATION_MAX = ROTATION_BOTTOM_TOP

        const val DEFAULT_FONT_NAME = "DialogInput"

        const val VERBOSE = "Verbose"
        const val DEBUG = "Debug"
        const val INFO = "Info"
        const val WARNING = "Warning"
        const val ERROR = "Error"
        const val FATAL = "Fatal"

        const val CROSS_PLATFORM_LAF = "Cross Platform"
        const val SYSTEM_LAF = "System"
        const val FLAT_LIGHT_LAF = "Flat Light"
        const val FLAT_DARK_LAF = "Flat Dark"

        var IsCreatingUI = true
    }

    private lateinit var menuBar: JMenuBar
    private lateinit var menuFile: JMenu
    private lateinit var itemFileOpen: JMenuItem
    private lateinit var itemFileFollow: JMenuItem
    private lateinit var itemFileOpenFiles: JMenuItem
    private lateinit var itemFileAppendFiles: JMenuItem
//    private lateinit var itemFileOpenRecents: JMenu
    private lateinit var itemFileExit: JMenuItem
    private lateinit var menuView: JMenu
    private lateinit var itemFull: JCheckBoxMenuItem
    private lateinit var itemSearch: JCheckBoxMenuItem
    private lateinit var itemRotation: JMenuItem
    private lateinit var menuSettings: JMenu
    private lateinit var itemlogCmd: JMenuItem
    private lateinit var itemlogFile: JMenuItem
    private lateinit var itemfilterIncremental: JCheckBoxMenuItem
    private lateinit var menuLogLevel: JMenu
    private lateinit var logLevelGroup: ButtonGroup
    private lateinit var itemAppearance: JMenuItem
    private lateinit var menuHelp: JMenu
    private lateinit var itemHelp: JMenuItem
    private lateinit var itemAbout: JMenuItem

    private lateinit var filterPanel: JPanel
    private lateinit var filterLeftPanel: JPanel

    private lateinit var logToolBar: ButtonPanel
    private lateinit var startBtn: ColorButton
    private lateinit var retryAdbToggle: ColorToggleButton
    private lateinit var stopBtn: ColorButton
    private lateinit var pauseToggle: ColorToggleButton
    private lateinit var clearViewsBtn: ColorButton
    private lateinit var saveBtn: ColorButton
//    private lateinit var rotationBtn: ColorButton
//    lateinit var filtersBtn: ColorButton
//    lateinit var cmdsBtn: ColorButton
    internal lateinit var searchPanel: SearchPanel

    private lateinit var logPanel: JPanel
    private lateinit var showLogPanel: JPanel
    private lateinit var matchCaseToggle: ColorToggleButton
    private lateinit var matchCaseTogglePanel: JPanel
    lateinit var showLogCombo: FilterComboBox
    var showLogComboStyle: FilterComboBox.Mode
    private lateinit var showLogToggle: ColorToggleButton
    private lateinit var showLogTogglePanel: JPanel

    private lateinit var boldLogPanel: JPanel
    private lateinit var boldLogCombo: FilterComboBox
    var boldLogComboStyle: FilterComboBox.Mode
    private lateinit var boldLogToggle: ColorToggleButton
    private lateinit var boldLogTogglePanel: JPanel

    private lateinit var showTagPanel: JPanel
    lateinit var showTagCombo: FilterComboBox
    var showTagComboStyle: FilterComboBox.Mode
    private lateinit var showTagToggle: ColorToggleButton
    private lateinit var showTagTogglePanel: JPanel

    private lateinit var showPidPanel: JPanel
    lateinit var showPidCombo: FilterComboBox
    var showPidComboStyle: FilterComboBox.Mode
    private lateinit var showPidToggle: ColorToggleButton
    private lateinit var showPidTogglePanel: JPanel

    private lateinit var showTidPanel: JPanel
    lateinit var showTidCombo: FilterComboBox
    var showTidComboStyle: FilterComboBox.Mode
    private lateinit var showTidToggle: ColorToggleButton
    private lateinit var showTidTogglePanel: JPanel

    private lateinit var logCmdCombo: ColorComboBox<String>

    private lateinit var deviceCombo: ColorComboBox<String>
    private lateinit var deviceStatus: JLabel
    private lateinit var adbConnectBtn: ColorButton
    private lateinit var adbRefreshBtn: ColorButton
    private lateinit var adbDisconnectBtn: ColorButton

    private lateinit var scrollBackLabel: JLabel
    private lateinit var scrollBackTF: JTextField
    private lateinit var scrollBackSplitFileToggle: ColorToggleButton
    private lateinit var scrollBackApplyBtn: ColorButton
    private lateinit var scrollBackKeepToggle: ColorToggleButton

    lateinit var filteredTableModel: LogTableModel
        private set

    private lateinit var fullTableModel: LogTableModel

    lateinit var logSplitPane: JSplitPane

    lateinit var filteredLogPanel: LogPanel
    lateinit var fullLogPanel: LogPanel
    private var selectedLine = 0

    private lateinit var statusBar: JPanel
    private lateinit var statusMethod: JLabel
    private lateinit var statusTF: JTextField

    private lateinit var followLabel: JLabel
    private lateinit var startFollowBtn: ColorButton
    private lateinit var stopFollowBtn: ColorButton
    private lateinit var pauseFollowToggle: ColorToggleButton

    private val frameMouseListener = FrameMouseListener(this)
    private val keyHandler = KeyHandler()
    private val itemHandler = ItemHandler()
    private val levelItemHandler = LevelItemHandler()
    private val actionHandler = ActionHandler()
    private val popupMenuHandler = PopupMenuHandler()
    private val mouseHandler = MouseHandler()
    private val componentHandler = ComponentHandler()
    private val statusChangeListener = StatusChangeListener()

    val configManager = ConfigManager.getInstance()
    private val colorManager = ColorManager.getInstance()

    private val logCmdManager = LogCmdManager.getInstance()
    lateinit var filtersManager: FiltersManager
    lateinit var cmdManager: CmdManager

    private var frameX = 0
    private var frameY = 0
    private var frameWidth = 1280
    private var frameHeight = 720
    private var frameExtendedState = Frame.MAXIMIZED_BOTH

    private var rotationStatus = ROTATION_LEFT_RIGHT

    var customFont: Font = Font(DEFAULT_FONT_NAME, Font.PLAIN, 12)
        set(value) {
            field = value
            if (!IsCreatingUI) {
                filteredLogPanel.customFont = value
                fullLogPanel.customFont = value
            }
        }

    var uiFontPercent = 100

    init {
        loadConfigOnCreate()
        logCmdManager.setMainUI(this)

        val laf = configManager.getItem(ConfigManager.ITEM_LOOK_AND_FEEL)

        if (laf == null) {
            ConfigManager.LaF = FLAT_LIGHT_LAF
        }
        else {
            ConfigManager.LaF = laf
        }

        val uiFontSize = configManager.getItem(ConfigManager.ITEM_UI_FONT_SIZE)
        if (!uiFontSize.isNullOrEmpty()) {
            uiFontPercent = uiFontSize.toInt()
        }

        if (ConfigManager.LaF == FLAT_LIGHT_LAF || ConfigManager.LaF == FLAT_DARK_LAF) {
            System.setProperty("flatlaf.uiScale", "$uiFontPercent%")
        }
        else {
            initFontSize(uiFontPercent)
        }

        setLaF(ConfigManager.LaF)

        val cmd = configManager.getItem(ConfigManager.ITEM_ADB_CMD)
        if (!cmd.isNullOrEmpty()) {
            logCmdManager.adbCmd = cmd
        } else {
            val os = System.getProperty("os.name")
            println("OS : $os")
            if (os.lowercase().contains("windows")) {
                logCmdManager.adbCmd = "adb.exe"
            } else {
                logCmdManager.adbCmd = "adb"
            }
        }
        logCmdManager.addEventListener(AdbHandler())
        val logSavePath = configManager.getItem(ConfigManager.ITEM_ADB_LOG_SAVE_PATH)
        if (logSavePath.isNullOrEmpty()) {
            logCmdManager.logSavePath = "."
        } else {
            logCmdManager.logSavePath = logSavePath
        }

        val logCmd = configManager.getItem(ConfigManager.ITEM_ADB_LOG_CMD)
        if (logCmd.isNullOrEmpty()) {
            logCmdManager.logCmd = LogCmdManager.DEFAULT_LOGCAT
        } else {
            logCmdManager.logCmd = logCmd
        }

        val prefix = configManager.getItem(ConfigManager.ITEM_ADB_PREFIX)
        if (prefix.isNullOrEmpty()) {
            logCmdManager.prefix = LogCmdManager.DEFAULT_PREFIX
        } else {
            logCmdManager.prefix = prefix
        }

        var prop = configManager.getItem(ConfigManager.ITEM_FRAME_X)
        if (!prop.isNullOrEmpty()) {
            frameX = prop.toInt()
        }
        prop = configManager.getItem(ConfigManager.ITEM_FRAME_Y)
        if (!prop.isNullOrEmpty()) {
            frameY = prop.toInt()
        }
        prop = configManager.getItem(ConfigManager.ITEM_FRAME_WIDTH)
        if (!prop.isNullOrEmpty()) {
            frameWidth = prop.toInt()
        }
        prop = configManager.getItem(ConfigManager.ITEM_FRAME_HEIGHT)
        if (!prop.isNullOrEmpty()) {
            frameHeight = prop.toInt()
        }
        prop = configManager.getItem(ConfigManager.ITEM_FRAME_EXTENDED_STATE)
        if (!prop.isNullOrEmpty()) {
            frameExtendedState = prop.toInt()
        }
        prop = configManager.getItem(ConfigManager.ITEM_ROTATION)
        if (!prop.isNullOrEmpty()) {
            rotationStatus = prop.toInt()
        }

        prop = configManager.getItem(ConfigManager.ITEM_LANG)
        if (!prop.isNullOrEmpty()) {
            Strings.lang = prop.toInt()
        }
        else {
            Strings.lang = Strings.EN
        }

        prop = configManager.getItem(ConfigManager.ITEM_SHOW_LOG_STYLE)
        showLogComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT
        }

        prop = configManager.getItem(ConfigManager.ITEM_BOLD_LOG_STYLE)
        boldLogComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT
        }

        prop = configManager.getItem(ConfigManager.ITEM_SHOW_TAG_STYLE)
        showTagComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT
        }

        prop = configManager.getItem(ConfigManager.ITEM_SHOW_PID_STYLE)
        showPidComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT
        }

        prop = configManager.getItem(ConfigManager.ITEM_SHOW_TID_STYLE)
        showTidComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT
        }

        createUI(title)

        if (logCmdManager.getType() == LogCmdManager.TYPE_LOGCAT) {
            logCmdManager.getDevices()
        }
    }

    private fun exit() {
        saveConfigOnDestroy()
        filteredTableModel.stopScan()
        fullTableModel.stopScan()
        logCmdManager.stop()
        exitProcess(0)
    }

    private fun loadConfigOnCreate() {
        configManager.loadConfig()
        colorManager.fullTableColor.getConfig()
        colorManager.fullTableColor.applyColor()
        colorManager.filterTableColor.getConfig()
        colorManager.filterTableColor.applyColor()
        colorManager.getConfigFilterStyle()
        configManager.saveConfig()
    }

    private fun saveConfigOnDestroy() {
        configManager.loadConfig()

        try {
            configManager.setItem(ConfigManager.ITEM_FRAME_X, location.x.toString())
        } catch (e: NullPointerException) {
            configManager.setItem(ConfigManager.ITEM_FRAME_X, "0")
        }

        try {
            configManager.setItem(ConfigManager.ITEM_FRAME_Y, location.y.toString())
        } catch (e: NullPointerException) {
            configManager.setItem(ConfigManager.ITEM_FRAME_Y, "0")
        }

        try {
            configManager.setItem(ConfigManager.ITEM_FRAME_WIDTH, size.width.toString())
        } catch (e: NullPointerException) {
            configManager.setItem(ConfigManager.ITEM_FRAME_WIDTH, "1280")
        }

        try {
            configManager.setItem(ConfigManager.ITEM_FRAME_HEIGHT, size.height.toString())
        } catch (e: NullPointerException) {
            configManager.setItem(ConfigManager.ITEM_FRAME_HEIGHT, "720")
        }

        configManager.setItem(ConfigManager.ITEM_FRAME_EXTENDED_STATE, extendedState.toString())

        var nCount = showLogCombo.itemCount
        if (nCount > ConfigManager.COUNT_SHOW_LOG) {
            nCount = ConfigManager.COUNT_SHOW_LOG
        }
        for (i in 0 until nCount) {
            configManager.setItem(ConfigManager.ITEM_SHOW_LOG + i, showLogCombo.getItemAt(i).toString())
        }

        for (i in nCount until ConfigManager.COUNT_SHOW_LOG) {
            configManager.removeConfigItem(ConfigManager.ITEM_SHOW_LOG + i)
        }

        nCount = showTagCombo.itemCount
        if (nCount > ConfigManager.COUNT_SHOW_TAG) {
            nCount = ConfigManager.COUNT_SHOW_TAG
        }
        for (i in 0 until nCount) {
            configManager.setItem(ConfigManager.ITEM_SHOW_TAG + i, showTagCombo.getItemAt(i).toString())
        }
        for (i in nCount until ConfigManager.COUNT_SHOW_TAG) {
            configManager.removeConfigItem(ConfigManager.ITEM_SHOW_TAG + i)
        }

        nCount = boldLogCombo.itemCount
        if (nCount > ConfigManager.COUNT_HIGHLIGHT_LOG) {
            nCount = ConfigManager.COUNT_HIGHLIGHT_LOG
        }
        for (i in 0 until nCount) {
            configManager.setItem(ConfigManager.ITEM_HIGHLIGHT_LOG + i, boldLogCombo.getItemAt(i).toString())
        }
        for (i in nCount until ConfigManager.COUNT_HIGHLIGHT_LOG) {
            configManager.removeConfigItem(ConfigManager.ITEM_HIGHLIGHT_LOG + i)
        }

        nCount = searchPanel.searchCombo.itemCount
        if (nCount > ConfigManager.COUNT_SEARCH_LOG) {
            nCount = ConfigManager.COUNT_SEARCH_LOG
        }
        for (i in 0 until nCount) {
            configManager.setItem(ConfigManager.ITEM_SEARCH_LOG + i, searchPanel.searchCombo.getItemAt(i).toString())
        }
        for (i in nCount until ConfigManager.COUNT_SEARCH_LOG) {
            configManager.removeConfigItem(ConfigManager.ITEM_SEARCH_LOG + i)
        }

        try {
            configManager.setItem(ConfigManager.ITEM_ADB_DEVICE, logCmdManager.targetDevice)
        } catch (e: NullPointerException) {
            configManager.setItem(ConfigManager.ITEM_ADB_DEVICE, "0.0.0.0")
        }

        try {
            configManager.setItem(ConfigManager.ITEM_ADB_LOG_CMD, logCmdCombo.editor.item.toString())
        } catch (e: NullPointerException) {
            configManager.setItem(ConfigManager.ITEM_ADB_LOG_CMD, LogCmdManager.DEFAULT_LOGCAT)
        }

        configManager.setItem(ConfigManager.ITEM_DIVIDER_LOCATION, logSplitPane.dividerLocation.toString())
        if (logSplitPane.lastDividerLocation != -1) {
            configManager.setItem(ConfigManager.ITEM_LAST_DIVIDER_LOCATION, logSplitPane.lastDividerLocation.toString())
        }

//            properties.put(ITEM_LANG, Strings.lang.toString())

        configManager.saveConfig()
    }

    private fun createUI(title: String) {
        setTitle(title)

        val img = ImageIcon(this.javaClass.getResource("/images/logo.png"))
        iconImage = img.image

        defaultCloseOperation = EXIT_ON_CLOSE
        setLocation(frameX, frameY)
        setSize(frameWidth, frameHeight)
        extendedState = frameExtendedState
        addComponentListener(componentHandler)

        menuBar = JMenuBar()
        menuFile = JMenu(Strings.FILE)
        menuFile.mnemonic = KeyEvent.VK_F

        itemFileOpen = JMenuItem(Strings.OPEN)
        itemFileOpen.addActionListener(actionHandler)
        menuFile.add(itemFileOpen)

        itemFileFollow = JMenuItem(Strings.FOLLOW)
        itemFileFollow.addActionListener(actionHandler)
        menuFile.add(itemFileFollow)

        itemFileOpenFiles = JMenuItem(Strings.OPEN_FILES)
        itemFileOpenFiles.addActionListener(actionHandler)
        menuFile.add(itemFileOpenFiles)

        itemFileAppendFiles = JMenuItem(Strings.APPEND_FILES)
        itemFileAppendFiles.addActionListener(actionHandler)
        menuFile.add(itemFileAppendFiles)

//       itemFileOpenRecents = JMenu(Strings.OPEN_RECENTS)
//       itemFileOpenRecents.addActionListener(mActionHandler)
//        menuFile.add(mItemFileOpenRecents)
        menuFile.addSeparator()

        itemFileExit = JMenuItem(Strings.EXIT)
        itemFileExit.addActionListener(actionHandler)
        menuFile.add(itemFileExit)
        menuBar.add(menuFile)

        menuView = JMenu(Strings.VIEW)
        menuView.mnemonic = KeyEvent.VK_V

        itemFull = JCheckBoxMenuItem(Strings.VIEW_FULL)
        itemFull.addActionListener(actionHandler)
        menuView.add(itemFull)

        menuView.addSeparator()

        itemSearch = JCheckBoxMenuItem(Strings.SEARCH)
        itemSearch.addActionListener(actionHandler)
        menuView.add(itemSearch)

        menuView.addSeparator()

        itemRotation = JMenuItem(Strings.ROTATION)
        itemRotation.addActionListener(actionHandler)
        menuView.add(itemRotation)

        menuBar.add(menuView)

        menuSettings = JMenu(Strings.SETTING)
        menuSettings.mnemonic = KeyEvent.VK_S

        itemlogCmd = JMenuItem("${Strings.LOG_CMD}(${Strings.ADB})")
        itemlogCmd.addActionListener(actionHandler)
        menuSettings.add(itemlogCmd)
        itemlogFile = JMenuItem(Strings.LOGFILE)
        itemlogFile.addActionListener(actionHandler)
        menuSettings.add(itemlogFile)

        menuSettings.addSeparator()

        itemfilterIncremental = JCheckBoxMenuItem(Strings.FILTER + "-" + Strings.INCREMENTAL)
        itemfilterIncremental.addActionListener(actionHandler)
        menuSettings.add(itemfilterIncremental)

        menuSettings.addSeparator()

        menuLogLevel = JMenu(Strings.LOGLEVEL)
        menuLogLevel.addActionListener(actionHandler)
        menuSettings.add(menuLogLevel)

        logLevelGroup = ButtonGroup()

        var menuItem = JRadioButtonMenuItem(VERBOSE)
        logLevelGroup.add(menuItem)
        menuLogLevel.add(menuItem)
        menuItem.isSelected = true
        menuItem.addItemListener(levelItemHandler)

        menuItem = JRadioButtonMenuItem(DEBUG)
        logLevelGroup.add(menuItem)
        menuLogLevel.add(menuItem)
        menuItem.addItemListener(levelItemHandler)

        menuItem = JRadioButtonMenuItem(INFO)
        logLevelGroup.add(menuItem)
        menuLogLevel.add(menuItem)
        menuItem.addItemListener(levelItemHandler)

        menuItem = JRadioButtonMenuItem(WARNING)
        logLevelGroup.add(menuItem)
        menuLogLevel.add(menuItem)
        menuItem.addItemListener(levelItemHandler)

        menuItem = JRadioButtonMenuItem(ERROR)
        logLevelGroup.add(menuItem)
        menuLogLevel.add(menuItem)
        menuItem.addItemListener(levelItemHandler)

        menuItem = JRadioButtonMenuItem(FATAL)
        logLevelGroup.add(menuItem)
        menuLogLevel.add(menuItem)
        menuItem.addItemListener(levelItemHandler)

        menuSettings.addSeparator()

        itemAppearance = JMenuItem(Strings.APPEARANCE)
        itemAppearance.addActionListener(actionHandler)
        menuSettings.add(itemAppearance)

        menuBar.add(menuSettings)

        menuHelp = JMenu(Strings.HELP)
        menuHelp.mnemonic = KeyEvent.VK_H

        itemHelp = JMenuItem(Strings.HELP)
        itemHelp.addActionListener(actionHandler)
        menuHelp.add(itemHelp)

        menuHelp.addSeparator()

        itemAbout = JMenuItem(Strings.ABOUT)
        itemAbout.addActionListener(actionHandler)
        menuHelp.add(itemAbout)
        menuBar.add(menuHelp)

        jMenuBar = menuBar

        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            UIManager.put("ScrollBar.thumb", ColorUIResource(Color(0xE0, 0xE0, 0xE0)))
            UIManager.put("ScrollBar.thumbHighlight", ColorUIResource(Color(0xE5, 0xE5, 0xE5)))
            UIManager.put("ScrollBar.thumbShadow", ColorUIResource(Color(0xE5, 0xE5, 0xE5)))
            UIManager.put("ComboBox.buttonDarkShadow", ColorUIResource(Color.black))
        }

        addMouseListener(frameMouseListener)
        addMouseMotionListener(frameMouseListener)
        contentPane.addMouseListener(frameMouseListener)

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exit()
            }
        })

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { p0 ->
            if (p0?.keyCode == KeyEvent.VK_PAGE_DOWN && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {
                filteredLogPanel.goToLast()
                fullLogPanel.goToLast()
            } else if (p0?.keyCode == KeyEvent.VK_PAGE_UP && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {
                filteredLogPanel.goToFirst()
                fullLogPanel.goToFirst()
//                } else if (p0?.keyCode == KeyEvent.VK_N && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {

            } else if (p0?.keyCode == KeyEvent.VK_L && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {
                deviceCombo.requestFocus()
            } else if (p0?.keyCode == KeyEvent.VK_R && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {
                reconnectAdb()
            } else if (p0?.keyCode == KeyEvent.VK_G && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {
                val goToDialog = GoToDialog(this@MainUI)
                goToDialog.setLocationRelativeTo(this@MainUI)
                goToDialog.isVisible = true
                if (goToDialog.line != -1) {
                    goToLine(goToDialog.line)
                } else {
                    println("Cancel Goto Line")
                }
            }

            false
        }

        filterPanel = JPanel()
        filterLeftPanel = JPanel()

        logToolBar = ButtonPanel()
        logToolBar.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        logToolBar.addMouseListener(mouseHandler)

        searchPanel = SearchPanel()

        val btnMargin = Insets(2, 5, 2, 5)
//        logToolBar = JPanel()
//        logToolBar.background = Color(0xE5, 0xE5, 0xE5)
        startBtn = ColorButton(Strings.START)
        startBtn.margin = btnMargin
        startBtn.toolTipText = TooltipStrings.START_BTN
        startBtn.icon = ImageIcon(this.javaClass.getResource("/images/start.png"))
        startBtn.addActionListener(actionHandler)
        startBtn.addMouseListener(mouseHandler)
        retryAdbToggle = ColorToggleButton(Strings.RETRY_ADB)
        retryAdbToggle.toolTipText = TooltipStrings.RETRY_ADB_TOGGLE
        retryAdbToggle.margin = btnMargin
//        retryAdbToggle.margin = Insets(retryAdbToggle.margin.top, 0, retryAdbToggle.margin.bottom, 0)
        retryAdbToggle.addItemListener(itemHandler)

        pauseToggle = ColorToggleButton(Strings.PAUSE)
        pauseToggle.toolTipText = TooltipStrings.PAUSE_BTN
        pauseToggle.margin = btnMargin
//        pauseToggle.margin = Insets(pauseToggle.margin.top, 0, pauseToggle.margin.bottom, 0)
        pauseToggle.addItemListener(itemHandler)


        stopBtn = ColorButton(Strings.STOP)
        stopBtn.margin = btnMargin
        stopBtn.toolTipText = TooltipStrings.STOP_BTN
        stopBtn.addActionListener(actionHandler)
        stopBtn.addMouseListener(mouseHandler)
        clearViewsBtn = ColorButton(Strings.CLEAR_VIEWS)
        clearViewsBtn.margin = btnMargin
        clearViewsBtn.toolTipText = TooltipStrings.CLEAR_BTN
        clearViewsBtn.icon = ImageIcon(this.javaClass.getResource("/images/clear.png"))

        clearViewsBtn.addActionListener(actionHandler)
        clearViewsBtn.addMouseListener(mouseHandler)
        saveBtn = ColorButton(Strings.SAVE)
        saveBtn.margin = btnMargin
        saveBtn.toolTipText = TooltipStrings.SAVE_BTN
        saveBtn.addActionListener(actionHandler)
        saveBtn.addMouseListener(mouseHandler)
//        rotationBtn = ColorButton(Strings.ROTATION)
//        rotationBtn.margin = btnMargin
//        rotationBtn.toolTipText = TooltipStrings.ROTATION_BTN
//        rotationBtn.addActionListener(actionHandler)
//        rotationBtn.addMouseListener(aouseHandler)
//        filtersBtn = ColorButton(Strings.FILTERS)
//        filtersBtn.margin = btnMargin
//        filtersBtn.toolTipText = TooltipStrings.FILTER_LIST_BTN
//        filtersBtn.addActionListener(actionHandler)
//        filtersBtn.addMouseListener(mouseHandler)
//        cmdsBtn = ColorButton(Strings.COMMANDS)
//        cmdsBtn.margin = btnMargin
//        cmdsBtn.toolTipText = TooltipStrings.CMD_LIST_BTN
//        cmdsBtn.addActionListener(actionHandler)
//        cmdsBtn.addMouseListener(mouseHandler)

        logPanel = JPanel()
        showLogPanel = JPanel()
        showLogCombo = FilterComboBox(showLogComboStyle, true)
        showLogCombo.toolTipText = TooltipStrings.LOG_COMBO
        showLogCombo.isEditable = true
        showLogCombo.renderer = FilterComboBox.ComboBoxRenderer()
        showLogCombo.editor.editorComponent.addKeyListener(keyHandler)
        showLogCombo.addItemListener(itemHandler)
        showLogCombo.addPopupMenuListener(popupMenuHandler)
        showLogCombo.editor.editorComponent.addMouseListener(mouseHandler)
        showLogToggle = ColorToggleButton(Strings.LOG)
        showLogToggle.toolTipText = TooltipStrings.LOG_TOGGLE
        showLogToggle.margin = Insets(0, 0, 0, 0)
        showLogTogglePanel = JPanel(GridLayout(1, 1))
        showLogTogglePanel.add(showLogToggle)
        showLogTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        showLogToggle.addItemListener(itemHandler)

        boldLogPanel = JPanel()
        boldLogCombo = FilterComboBox(boldLogComboStyle, false)
        boldLogCombo.toolTipText = TooltipStrings.BOLD_COMBO
        boldLogCombo.enabledTfTooltip = false
        boldLogCombo.isEditable = true
        boldLogCombo.renderer = FilterComboBox.ComboBoxRenderer()
        boldLogCombo.editor.editorComponent.addKeyListener(keyHandler)
        boldLogCombo.addItemListener(itemHandler)
        boldLogCombo.editor.editorComponent.addMouseListener(mouseHandler)
        boldLogToggle = ColorToggleButton(Strings.BOLD)
        boldLogToggle.toolTipText = TooltipStrings.BOLD_TOGGLE
        boldLogToggle.margin = Insets(0, 0, 0, 0)
        boldLogTogglePanel = JPanel(GridLayout(1, 1))
        boldLogTogglePanel.add(boldLogToggle)
        boldLogTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        boldLogToggle.addItemListener(itemHandler)

        showTagPanel = JPanel()
        showTagCombo = FilterComboBox(showTagComboStyle, false)
        showTagCombo.toolTipText = TooltipStrings.TAG_COMBO
        showTagCombo.isEditable = true
        showTagCombo.renderer = FilterComboBox.ComboBoxRenderer()
        showTagCombo.editor.editorComponent.addKeyListener(keyHandler)
        showTagCombo.addItemListener(itemHandler)
        showTagCombo.editor.editorComponent.addMouseListener(mouseHandler)
        showTagToggle = ColorToggleButton(Strings.TAG)
        showTagToggle.toolTipText = TooltipStrings.TAG_TOGGLE
        showTagToggle.margin = Insets(0, 0, 0, 0)
        showTagTogglePanel = JPanel(GridLayout(1, 1))
        showTagTogglePanel.add(showTagToggle)
        showTagTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        showTagToggle.addItemListener(itemHandler)

        showPidPanel = JPanel()
        showPidCombo = FilterComboBox(showPidComboStyle, false)
        showPidCombo.toolTipText = TooltipStrings.PID_COMBO
        showPidCombo.isEditable = true
        showPidCombo.renderer = FilterComboBox.ComboBoxRenderer()
        showPidCombo.editor.editorComponent.addKeyListener(keyHandler)
        showPidCombo.addItemListener(itemHandler)
        showPidCombo.editor.editorComponent.addMouseListener(mouseHandler)
        showPidToggle = ColorToggleButton(Strings.PID)
        showPidToggle.toolTipText = TooltipStrings.PID_TOGGLE
        showPidToggle.margin = Insets(0, 0, 0, 0)
        showPidTogglePanel = JPanel(GridLayout(1, 1))
        showPidTogglePanel.add(showPidToggle)
        showPidTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        showPidToggle.addItemListener(itemHandler)

        showTidPanel = JPanel()
        showTidCombo = FilterComboBox(showTidComboStyle, false)
        showTidCombo.toolTipText = TooltipStrings.TID_COMBO
        showTidCombo.isEditable = true
        showTidCombo.renderer = FilterComboBox.ComboBoxRenderer()
        showTidCombo.editor.editorComponent.addKeyListener(keyHandler)
        showTidCombo.addItemListener(itemHandler)
        showTidCombo.editor.editorComponent.addMouseListener(mouseHandler)
        showTidToggle = ColorToggleButton(Strings.TID)
        showTidToggle.toolTipText = TooltipStrings.TID_TOGGLE
        showTidToggle.margin = Insets(0, 0, 0, 0)
        showTidTogglePanel = JPanel(GridLayout(1, 1))
        showTidTogglePanel.add(showTidToggle)
        showTidTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        showTidToggle.addItemListener(itemHandler)

        logCmdCombo = ColorComboBox()
        logCmdCombo.toolTipText = TooltipStrings.LOG_CMD_COMBO
        logCmdCombo.isEditable = true
        logCmdCombo.renderer = ColorComboBox.ComboBoxRenderer()
        logCmdCombo.editor.editorComponent.addKeyListener(keyHandler)
        logCmdCombo.addItemListener(itemHandler)
        logCmdCombo.editor.editorComponent.addMouseListener(mouseHandler)
        logCmdCombo.addPopupMenuListener(popupMenuHandler)
        
        deviceStatus = JLabel("None", JLabel.LEFT)
        deviceStatus.isEnabled = false
        val deviceComboPanel = JPanel(BorderLayout())
        deviceCombo = ColorComboBox()
        deviceCombo.toolTipText = TooltipStrings.DEVICES_COMBO
        deviceCombo.isEditable = true
        deviceCombo.renderer = ColorComboBox.ComboBoxRenderer()
        deviceCombo.editor.editorComponent.addKeyListener(keyHandler)
        deviceCombo.addItemListener(itemHandler)
        deviceCombo.editor.editorComponent.addMouseListener(mouseHandler)
        deviceComboPanel.add(deviceCombo, BorderLayout.CENTER)
        adbConnectBtn = ColorButton(Strings.CONNECT)
        adbConnectBtn.margin = btnMargin
        adbConnectBtn.toolTipText = TooltipStrings.CONNECT_BTN
        adbConnectBtn.addActionListener(actionHandler)
//        deviceComboPanel.add(adbConnectBtn, BorderLayout.EAST)
        adbRefreshBtn = ColorButton(Strings.REFRESH)
        adbRefreshBtn.margin = btnMargin
        adbRefreshBtn.addActionListener(actionHandler)
        adbRefreshBtn.toolTipText = TooltipStrings.REFRESH_BTN
        adbDisconnectBtn = ColorButton(Strings.DISCONNECT)
        adbDisconnectBtn.margin = btnMargin
        adbDisconnectBtn.addActionListener(actionHandler)
        adbDisconnectBtn.toolTipText = TooltipStrings.DISCONNECT_BTN

        matchCaseToggle = ColorToggleButton("Aa")
        matchCaseToggle.toolTipText = TooltipStrings.CASE_TOGGLE
        matchCaseToggle.margin = Insets(0, 0, 0, 0)
        matchCaseToggle.addItemListener(itemHandler)
        matchCaseTogglePanel = JPanel(GridLayout(1, 1))
        matchCaseTogglePanel.add(matchCaseToggle)
        matchCaseTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)

        showLogPanel.layout = BorderLayout()
        showLogPanel.add(showLogTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            showLogCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        showLogPanel.add(showLogCombo, BorderLayout.CENTER)

        boldLogPanel.layout = BorderLayout()
        boldLogPanel.add(boldLogTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            boldLogCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        boldLogCombo.preferredSize = Dimension(170, boldLogCombo.preferredSize.height)
        boldLogPanel.add(boldLogCombo, BorderLayout.CENTER)
//        boldPanel.add(boldLogPanel)

        showTagPanel.layout = BorderLayout()
        showTagPanel.add(showTagTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            showTagCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        showTagCombo.preferredSize = Dimension(250, showTagCombo.preferredSize.height)
        showTagPanel.add(showTagCombo, BorderLayout.CENTER)
//        tagPanel.add(showTagPanel)

        showPidPanel.layout = BorderLayout()
        showPidPanel.add(showPidTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            showPidCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        showPidCombo.preferredSize = Dimension(120, showPidCombo.preferredSize.height)
        showPidPanel.add(showPidCombo, BorderLayout.CENTER)
//        pidPanel.add(showPidPanel)

        showTidPanel.layout = BorderLayout()
        showTidPanel.add(showTidTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            showTidCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        showTidCombo.preferredSize = Dimension(120, showTidCombo.preferredSize.height)
        showTidPanel.add(showTidCombo, BorderLayout.CENTER)
//        tidPanel.add(showTidPanel)

        logCmdCombo.preferredSize = Dimension(200, logCmdCombo.preferredSize.height)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            logCmdCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 5)
        }

        deviceCombo.preferredSize = Dimension(200, deviceCombo.preferredSize.height)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            deviceCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 5)
        }
        deviceStatus.preferredSize = Dimension(100, 30)
        deviceStatus.border = BorderFactory.createEmptyBorder(3, 0, 3, 0)
        deviceStatus.horizontalAlignment = JLabel.CENTER

        scrollBackApplyBtn = ColorButton(Strings.APPLY)
        scrollBackApplyBtn.margin = btnMargin
        scrollBackApplyBtn.toolTipText = TooltipStrings.SCROLL_BACK_APPLY_BTN
        scrollBackApplyBtn.addActionListener(actionHandler)
        scrollBackKeepToggle = ColorToggleButton(Strings.KEEP)
        scrollBackKeepToggle.toolTipText = TooltipStrings.SCROLL_BACK_KEEP_TOGGLE
        scrollBackKeepToggle.selectedBg = Color.RED
        scrollBackKeepToggle.selectedFg = Color.BLACK
        if (ConfigManager.LaF != CROSS_PLATFORM_LAF) {
            val imgIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on_warn.png"))
            scrollBackKeepToggle.selectedIcon = imgIcon
        }

        scrollBackKeepToggle.margin = btnMargin
        scrollBackKeepToggle.addItemListener(itemHandler)

        scrollBackLabel = JLabel(Strings.SCROLL_BACK_LINES)

        scrollBackTF = JTextField()
        scrollBackTF.toolTipText = TooltipStrings.SCROLL_BACK_TF
        scrollBackTF.preferredSize = Dimension(80, scrollBackTF.preferredSize.height)
        scrollBackTF.addKeyListener(keyHandler)
        scrollBackSplitFileToggle = ColorToggleButton(Strings.SPLIT_FILE)
        scrollBackSplitFileToggle.toolTipText = TooltipStrings.SCROLL_BACK_SPLIT_CHK
        scrollBackSplitFileToggle.margin = btnMargin
        scrollBackSplitFileToggle.addItemListener(itemHandler)

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
        filterPanel.addMouseListener(mouseHandler)

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

        val toolBarPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        toolBarPanel.layout = BorderLayout()
        toolBarPanel.addMouseListener(mouseHandler)
        toolBarPanel.add(logToolBar, BorderLayout.CENTER)
        toolBarPanel.add(scrollbackPanel, BorderLayout.EAST)

        filterPanel.add(toolBarPanel, BorderLayout.NORTH)
        filterPanel.add(searchPanel, BorderLayout.SOUTH)

        searchPanel.isVisible = false
        itemSearch.state = searchPanel.isVisible

        layout = BorderLayout()

        fullTableModel = LogTableModel(this, null)
        filteredTableModel = LogTableModel(this, fullTableModel)

        fullLogPanel = LogPanel(this, fullTableModel, null, FocusHandler(false))
        filteredLogPanel = LogPanel(this, filteredTableModel, fullLogPanel, FocusHandler(true))
        fullLogPanel.updateTableBar(configManager.loadCmd())
        filteredLogPanel.updateTableBar(configManager.loadFilters())

        filtersManager = FiltersManager(this, filteredLogPanel)
        cmdManager = CmdManager(this, fullLogPanel)

        when (rotationStatus) {
            ROTATION_LEFT_RIGHT -> {
                logSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, filteredLogPanel, fullLogPanel)
                logSplitPane.resizeWeight = SPLIT_WEIGHT
            }
            ROTATION_RIGHT_LEFT -> {
                logSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, fullLogPanel, filteredLogPanel)
                logSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
            }
            ROTATION_TOP_BOTTOM -> {
                logSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, false, filteredLogPanel, fullLogPanel)
                logSplitPane.resizeWeight = SPLIT_WEIGHT
            }
            ROTATION_BOTTOM_TOP -> {
                logSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, false, fullLogPanel, filteredLogPanel)
                logSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
            }
        }

        val dividerSize = configManager.getItem(ConfigManager.ITEM_APPEARANCE_DIVIDER_SIZE)
        if (!dividerSize.isNullOrEmpty()) {
            logSplitPane.dividerSize = dividerSize.toInt()
        }

        logSplitPane.isOneTouchExpandable = false

        statusBar = JPanel(BorderLayout())
        statusBar.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        statusMethod = JLabel("")
        statusMethod.isOpaque = true
        statusMethod.background = Color.DARK_GRAY
        statusMethod.addPropertyChangeListener(statusChangeListener)
        statusTF = StatusTextField(Strings.NONE)
        statusTF.document.addDocumentListener(statusChangeListener)
        statusTF.toolTipText = TooltipStrings.SAVED_FILE_TF
        statusTF.isEditable = false
        statusTF.border = BorderFactory.createEmptyBorder()

        startFollowBtn = ColorButton(Strings.START)
        startFollowBtn.margin = btnMargin
        startFollowBtn.toolTipText = TooltipStrings.START_FOLLOW_BTN
        startFollowBtn.addActionListener(actionHandler)
        startFollowBtn.addMouseListener(mouseHandler)

        pauseFollowToggle = ColorToggleButton(Strings.PAUSE)
        pauseFollowToggle.margin = Insets(pauseFollowToggle.margin.top, 0, pauseFollowToggle.margin.bottom, 0)
        pauseFollowToggle.addItemListener(itemHandler)

        stopFollowBtn = ColorButton(Strings.STOP)
        stopFollowBtn.margin = btnMargin
        stopFollowBtn.toolTipText = TooltipStrings.STOP_FOLLOW_BTN
        stopFollowBtn.addActionListener(actionHandler)
        stopFollowBtn.addMouseListener(mouseHandler)

        val followPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
        followPanel.border = BorderFactory.createEmptyBorder(0, 3, 0, 3)
        followLabel = JLabel(" ${Strings.FOLLOW} ")
        followLabel.border = BorderFactory.createDashedBorder(null, 1.0f, 2.0f)
        followPanel.add(followLabel)
        followPanel.add(startFollowBtn)
        followPanel.add(pauseFollowToggle)
        followPanel.add(stopFollowBtn)

        enabledFollowBtn(false)

        statusBar.add(statusMethod, BorderLayout.WEST)
        statusBar.add(statusTF, BorderLayout.CENTER)
        statusBar.add(followPanel, BorderLayout.EAST)

        val logLevel = configManager.getItem(ConfigManager.ITEM_LOG_LEVEL)
        if (logLevel != null) {
            for (item in logLevelGroup.elements) {
                if (logLevel == item.text) {
                    item.isSelected = true
                    break
                }
            }
        }

        var item: String?
        for (i in 0 until ConfigManager.COUNT_SHOW_LOG) {
            item = configManager.getItem(ConfigManager.ITEM_SHOW_LOG + i)
            if (item == null) {
                break
            }

            if (!showLogCombo.isExistItem(item)) {
                showLogCombo.addItem(item)
            }
        }

        showLogCombo.updateTooltip()

        if (showLogCombo.itemCount > 0) {
            showLogCombo.selectedIndex = 0
        }

        var check = configManager.getItem(ConfigManager.ITEM_SHOW_LOG_CHECK)
        if (!check.isNullOrEmpty()) {
            showLogToggle.isSelected = check.toBoolean()
        } else {
            showLogToggle.isSelected = true
        }
        showLogCombo.isEnabled = showLogToggle.isSelected

        for (i in 0 until ConfigManager.COUNT_SHOW_TAG) {
            item = configManager.getItem(ConfigManager.ITEM_SHOW_TAG + i)
            if (item == null) {
                break
            }
            showTagCombo.insertItemAt(item, i)
            if (i == 0) {
                showTagCombo.selectedIndex = 0
            }
        }

        showTagCombo.updateTooltip()

        check = configManager.getItem(ConfigManager.ITEM_SHOW_TAG_CHECK)
        if (!check.isNullOrEmpty()) {
            showTagToggle.isSelected = check.toBoolean()
        } else {
            showTagToggle.isSelected = true
        }
        showTagCombo.setEnabledFilter(showTagToggle.isSelected)

        check = configManager.getItem(ConfigManager.ITEM_SHOW_PID_CHECK)
        if (!check.isNullOrEmpty()) {
            showPidToggle.isSelected = check.toBoolean()
        } else {
            showPidToggle.isSelected = true
        }
        showPidCombo.setEnabledFilter(showPidToggle.isSelected)

        check = configManager.getItem(ConfigManager.ITEM_SHOW_TID_CHECK)
        if (!check.isNullOrEmpty()) {
            showTidToggle.isSelected = check.toBoolean()
        } else {
            showTidToggle.isSelected = true
        }
        showTidCombo.setEnabledFilter(showTidToggle.isSelected)

        for (i in 0 until ConfigManager.COUNT_HIGHLIGHT_LOG) {
            item = configManager.getItem(ConfigManager.ITEM_HIGHLIGHT_LOG + i)
            if (item == null) {
                break
            }
            boldLogCombo.insertItemAt(item, i)
            if (i == 0) {
                boldLogCombo.selectedIndex = 0
            }
        }

        boldLogCombo.updateTooltip()

        check = configManager.getItem(ConfigManager.ITEM_HIGHLIGHT_LOG_CHECK)
        if (!check.isNullOrEmpty()) {
            boldLogToggle.isSelected = check.toBoolean()
        } else {
            boldLogToggle.isSelected = true
        }
        boldLogCombo.setEnabledFilter(boldLogToggle.isSelected)

        for (i in 0 until ConfigManager.COUNT_SEARCH_LOG) {
            item = configManager.getItem(ConfigManager.ITEM_SEARCH_LOG + i)
            if (item == null) {
                break
            }
            searchPanel.searchCombo.insertItemAt(item, i)
            if (i == 0) {
                searchPanel.searchCombo.selectedIndex = 0
            }
        }

        searchPanel.searchCombo.updateTooltip()

        updateLogCmdCombo(true)

        val targetDevice = configManager.getItem(ConfigManager.ITEM_ADB_DEVICE)
        deviceCombo.insertItemAt(targetDevice, 0)
        deviceCombo.selectedIndex = 0

        if (logCmdManager.devices.contains(targetDevice)) {
            deviceStatus.text = Strings.CONNECTED
            setDeviceComboColor(true)
        } else {
            deviceStatus.text = Strings.NOT_CONNECTED
            setDeviceComboColor(false)
        }

        var fontName = configManager.getItem(ConfigManager.ITEM_FONT_NAME)
        if (fontName.isNullOrEmpty()) {
            fontName = DEFAULT_FONT_NAME
        }

        var fontSize = 12
        check = configManager.getItem(ConfigManager.ITEM_FONT_SIZE)
        if (!check.isNullOrEmpty()) {
            fontSize = check.toInt()
        }

        customFont = Font(fontName, Font.PLAIN, fontSize)
        filteredLogPanel.customFont = customFont
        fullLogPanel.customFont = customFont

        var divider = configManager.getItem(ConfigManager.ITEM_LAST_DIVIDER_LOCATION)
        if (!divider.isNullOrEmpty()) {
            logSplitPane.lastDividerLocation = divider.toInt()
        }

        divider = configManager.getItem(ConfigManager.ITEM_DIVIDER_LOCATION)
        if (!divider.isNullOrEmpty() && logSplitPane.lastDividerLocation != -1) {
            logSplitPane.dividerLocation = divider.toInt()
        }

        when (logLevel) {
            VERBOSE ->filteredTableModel.filterLevel = LEVEL_VERBOSE
            DEBUG ->filteredTableModel.filterLevel = LEVEL_DEBUG
            INFO ->filteredTableModel.filterLevel = LEVEL_INFO
            WARNING ->filteredTableModel.filterLevel = LEVEL_WARNING
            ERROR ->filteredTableModel.filterLevel = LEVEL_ERROR
            FATAL ->filteredTableModel.filterLevel = LEVEL_FATAL
        }

        if (showLogToggle.isSelected && showLogCombo.selectedItem != null) {
            filteredTableModel.filterLog = showLogCombo.selectedItem!!.toString()
        } else {
            filteredTableModel.filterLog = ""
        }
        if (boldLogToggle.isSelected && boldLogCombo.selectedItem != null) {
            filteredTableModel.filterHighlightLog = boldLogCombo.selectedItem!!.toString()
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

        check = configManager.getItem(ConfigManager.ITEM_VIEW_FULL)
        if (!check.isNullOrEmpty()) {
            itemFull.state = check.toBoolean()
        } else {
            itemFull.state = true
        }
        if (!itemFull.state) {
            windowedModeLogPanel(fullLogPanel)
        }

        check = configManager.getItem(ConfigManager.ITEM_FILTER_INCREMENTAL)
        if (!check.isNullOrEmpty()) {
            itemfilterIncremental.state = check.toBoolean()
        } else {
            itemfilterIncremental.state = false
        }

        check = configManager.getItem(ConfigManager.ITEM_SCROLL_BACK)
        if (!check.isNullOrEmpty()) {
            scrollBackTF.text = check
        } else {
            scrollBackTF.text = "0"
        }
        filteredTableModel.scrollback = scrollBackTF.text.toInt()

        check = configManager.getItem(ConfigManager.ITEM_SCROLL_BACK_SPLIT_FILE)
        if (!check.isNullOrEmpty()) {
            scrollBackSplitFileToggle.isSelected = check.toBoolean()
        } else {
            scrollBackSplitFileToggle.isSelected = false
        }
        filteredTableModel.scrollBackSplitFile = scrollBackSplitFileToggle.isSelected

        check = configManager.getItem(ConfigManager.ITEM_MATCH_CASE)
        if (!check.isNullOrEmpty()) {
            matchCaseToggle.isSelected = check.toBoolean()
        } else {
            matchCaseToggle.isSelected = false
        }
        filteredTableModel.matchCase = matchCaseToggle.isSelected

        check = configManager.getItem(ConfigManager.ITEM_SEARCH_MATCH_CASE)
        if (!check.isNullOrEmpty()) {
            searchPanel.searchMatchCaseToggle.isSelected = check.toBoolean()
        } else {
            searchPanel.searchMatchCaseToggle.isSelected = false
        }
        filteredTableModel.SearchMatchCase = searchPanel.searchMatchCaseToggle.isSelected

        check = configManager.getItem(ConfigManager.ITEM_RETRY_ADB)
        if (!check.isNullOrEmpty()) {
            retryAdbToggle.isSelected = check.toBoolean()
        } else {
            retryAdbToggle.isSelected = false
        }

        check = configManager.getItem(ConfigManager.ITEM_ICON_TEXT)
        if (!check.isNullOrEmpty()) {
            when (check) {
                ConfigManager.VALUE_ICON_TEXT_I -> {
                    setBtnIcons(true)
                    setBtnTexts(false)
                }
                ConfigManager.VALUE_ICON_TEXT_T -> {
                    setBtnIcons(false)
                    setBtnTexts(true)
                }
                else -> {
                    setBtnIcons(true)
                    setBtnTexts(true)
                }
            }
        } else {
            setBtnIcons(true)
            setBtnTexts(true)
        }

        add(filterPanel, BorderLayout.NORTH)
        add(logSplitPane, BorderLayout.CENTER)
        add(statusBar, BorderLayout.SOUTH)

        registerSearchStroke()

        IsCreatingUI = false
    }

    private fun setBtnIcons(isShow:Boolean) {
        if (isShow) {
            startBtn.icon = ImageIcon(this.javaClass.getResource("/images/start.png"))
            stopBtn.icon = ImageIcon(this.javaClass.getResource("/images/stop.png"))
            clearViewsBtn.icon = ImageIcon(this.javaClass.getResource("/images/clear.png"))
            saveBtn.icon = ImageIcon(this.javaClass.getResource("/images/save.png"))
            adbConnectBtn.icon = ImageIcon(this.javaClass.getResource("/images/connect.png"))
            adbRefreshBtn.icon = ImageIcon(this.javaClass.getResource("/images/refresh.png"))
            adbDisconnectBtn.icon = ImageIcon(this.javaClass.getResource("/images/disconnect.png"))
            scrollBackApplyBtn.icon = ImageIcon(this.javaClass.getResource("/images/apply.png"))

            retryAdbToggle.icon = ImageIcon(this.javaClass.getResource("/images/retry_off.png"))
            pauseToggle.icon = ImageIcon(this.javaClass.getResource("/images/pause_off.png"))
            scrollBackKeepToggle.icon = ImageIcon(this.javaClass.getResource("/images/keeplog_off.png"))
            scrollBackSplitFileToggle.icon = ImageIcon(this.javaClass.getResource("/images/splitfile_off.png"))

            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                retryAdbToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/retry_on_dark.png"))
                pauseToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/pause_on_dark.png"))
                scrollBackKeepToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/keeplog_on_dark.png"))
                scrollBackSplitFileToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/splitfile_on_dark.png"))
            }
            else {
                retryAdbToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/retry_on.png"))
                pauseToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/pause_on.png"))
                scrollBackKeepToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/keeplog_on.png"))
                scrollBackSplitFileToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/splitfile_on.png"))
            }

            scrollBackLabel.icon = ImageIcon(this.javaClass.getResource("/images/scrollback.png"))
        }
        else {
            startBtn.icon = null
            stopBtn.icon = null
            clearViewsBtn.icon = null
            saveBtn.icon = null
            adbConnectBtn.icon = null
            adbRefreshBtn.icon = null
            adbDisconnectBtn.icon = null
            scrollBackApplyBtn.icon = null

            retryAdbToggle.icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
            retryAdbToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on.png"))
            pauseToggle.icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
            pauseToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on.png"))
            scrollBackKeepToggle.icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
            scrollBackKeepToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on_warn.png"))
            scrollBackSplitFileToggle.icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
            scrollBackSplitFileToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on.png"))

            scrollBackLabel.icon = null
        }
    }

    private fun setBtnTexts(isShow:Boolean) {
        if (isShow) {
            startBtn.text = Strings.START
            retryAdbToggle.text = Strings.RETRY_ADB
            pauseToggle.text = Strings.PAUSE
            stopBtn.text = Strings.STOP
            clearViewsBtn.text = Strings.CLEAR_VIEWS
            saveBtn.text = Strings.SAVE
            adbConnectBtn.text = Strings.CONNECT
            adbRefreshBtn.text = Strings.REFRESH
            adbDisconnectBtn.text = Strings.DISCONNECT
            scrollBackApplyBtn.text = Strings.APPLY
            scrollBackKeepToggle.text = Strings.KEEP
            scrollBackSplitFileToggle.text = Strings.SPLIT_FILE
            scrollBackLabel.text = Strings.SCROLL_BACK_LINES
        }
        else {
            startBtn.text = null
            stopBtn.text = null
            clearViewsBtn.text = null
            saveBtn.text = null
            adbConnectBtn.text = null
            adbRefreshBtn.text = null
            adbDisconnectBtn.text = null
            scrollBackApplyBtn.text = null
            retryAdbToggle.text = null
            pauseToggle.text = null
            scrollBackKeepToggle.text = null
            scrollBackSplitFileToggle.text = null
            scrollBackLabel.text = null
        }
    }

    inner class StatusChangeListener : PropertyChangeListener, DocumentListener {
        private var method = ""
        override fun propertyChange(evt: PropertyChangeEvent?) {
            if (evt?.source == statusMethod && evt.propertyName == "text") {
                method = evt.newValue.toString().trim()
            }
        }

        override fun insertUpdate(evt: DocumentEvent?) {
            updateTitleBar(method)
        }

        override fun removeUpdate(e: DocumentEvent?) {
        }

        override fun changedUpdate(evt: DocumentEvent?) {
        }
    }

    private fun updateTitleBar(statusMethod: String) {
        title = when (statusMethod) {
            Strings.OPEN, Strings.FOLLOW, "${Strings.FOLLOW} ${Strings.STOP}" -> {
                val path: Path = Paths.get(statusTF.text)
                path.fileName.toString()
            }
            Strings.ADB, Strings.CMD, "${Strings.ADB} ${Strings.STOP}", "${Strings.CMD} ${Strings.STOP}" -> {
                logCmdManager.targetDevice.ifEmpty { NAME }
            }
            else -> {
                NAME
            }
        }
    }
    private fun setLaF(laf:String) {
        ConfigManager.LaF = laf
        when (laf) {
            CROSS_PLATFORM_LAF ->{
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
                } catch (ex: Exception) {
                    println("Failed to initialize CrossPlatformLaf")
                }
            }
            SYSTEM_LAF ->{
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                } catch (ex: Exception) {
                    println("Failed to initialize SystemLaf")
                }
            }
            FLAT_LIGHT_LAF ->{
                try {
                    UIManager.setLookAndFeel(FlatLightLaf())
                } catch (ex: Exception) {
                    println("Failed to initialize FlatLightLaf")
                }
            }
            FLAT_DARK_LAF ->{
                try {
                    UIManager.setLookAndFeel(FlatDarkLaf())
                } catch (ex: Exception) {
                    println("Failed to initialize FlatDarkLaf")
                }
            }
            else->{
                try {
                    UIManager.setLookAndFeel(FlatLightLaf())
                } catch (ex: Exception) {
                    println("Failed to initialize FlatLightLaf")
                }
            }
        }
        SwingUtilities.updateComponentTreeUI(this)
    }

    private fun addVSeparator(panel:JPanel) {
        val separator1 = JSeparator(SwingConstants.VERTICAL)
        separator1.preferredSize = Dimension(separator1.preferredSize.width, 20)
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            separator1.foreground = Color.GRAY
            separator1.background = Color.GRAY
        }
        else {
            separator1.foreground = Color.DARK_GRAY
            separator1.background = Color.DARK_GRAY
        }
        val separator2 = JSeparator(SwingConstants.VERTICAL)
        separator2.preferredSize = Dimension(separator2.preferredSize.width, 20)
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            separator2.foreground = Color.GRAY
            separator2.background = Color.GRAY
        }
        else {
            separator2.background = Color.DARK_GRAY
            separator2.foreground = Color.DARK_GRAY
        }
        panel.add(Box.createHorizontalStrut(5))
        panel.add(separator1)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            panel.add(separator2)
        }
        panel.add(Box.createHorizontalStrut(5))
    }

    private fun addVSeparator2(panel:JPanel) {
        val separator1 = JSeparator(SwingConstants.VERTICAL)
        separator1.preferredSize = Dimension(separator1.preferredSize.width / 2, 20)
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            separator1.foreground = Color.GRAY
            separator1.background = Color.GRAY
        }
        else {
            separator1.foreground = Color.DARK_GRAY
            separator1.background = Color.DARK_GRAY
        }
        panel.add(Box.createHorizontalStrut(2))
        panel.add(separator1)
        panel.add(Box.createHorizontalStrut(2))
    }

    private fun initFontSize(fontSize: Int) {
        val multiplier = fontSize / 100.0f
        val defaults = UIManager.getDefaults()
        val e: Enumeration<*> = defaults.keys()
        while (e.hasMoreElements()) {
            val key = e.nextElement()
            val value = defaults[key]
            if (value is Font) {
                val newSize = (value.size * multiplier).roundToInt()
                if (value is FontUIResource) {
                    defaults[key] = FontUIResource(value.name, value.style, newSize)
                } else {
                    defaults[key] = Font(value.name, value.style, newSize)
                }
            }
        }
    }

    fun windowedModeLogPanel(logPanel: LogPanel) {
        if (logPanel.parent == logSplitPane) {
            logPanel.isWindowedMode = true
            itemRotation.isEnabled = false
            logSplitPane.remove(logPanel)
            if (itemFull.state) {
                val logTableDialog = LogTableDialog(this@MainUI, logPanel)
                logTableDialog.isVisible = true
            }
        }
    }

    fun attachLogPanel(logPanel: LogPanel) {
        if (logPanel.parent != logSplitPane) {
            logPanel.isWindowedMode = false
            itemRotation.isEnabled = true
            logSplitPane.remove(filteredLogPanel)
            logSplitPane.remove(fullLogPanel)
            when (rotationStatus) {
                ROTATION_LEFT_RIGHT -> {
                    logSplitPane.orientation = JSplitPane.HORIZONTAL_SPLIT
                    logSplitPane.add(filteredLogPanel)
                    logSplitPane.add(fullLogPanel)
                    logSplitPane.resizeWeight = SPLIT_WEIGHT
                }
                ROTATION_RIGHT_LEFT -> {
                    logSplitPane.orientation = JSplitPane.HORIZONTAL_SPLIT
                    logSplitPane.add(fullLogPanel)
                    logSplitPane.add(filteredLogPanel)
                    logSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
                }
                ROTATION_TOP_BOTTOM -> {
                    logSplitPane.orientation = JSplitPane.VERTICAL_SPLIT
                    logSplitPane.add(filteredLogPanel)
                    logSplitPane.add(fullLogPanel)
                    logSplitPane.resizeWeight = SPLIT_WEIGHT
                }
                ROTATION_BOTTOM_TOP -> {
                    logSplitPane.orientation = JSplitPane.VERTICAL_SPLIT
                    logSplitPane.add(fullLogPanel)
                    logSplitPane.add(filteredLogPanel)
                    logSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
                }
            }
        }
    }

    fun openFile(path: String, isAppend: Boolean) {
        println("Opening: $path, $isAppend")
        statusMethod.text = " ${Strings.OPEN} "
        filteredTableModel.stopScan()
        filteredTableModel.stopFollow()

        if (isAppend) {
            statusTF.text += "| $path"
        } else {
            statusTF.text = path
        }
        fullTableModel.setLogFile(path)
        filteredTableModel.setLogFile(path)
        fullTableModel.loadItems(isAppend)
        filteredTableModel.loadItems(isAppend)

        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x50, 0x50, 0x00)
        }
        else {
            statusMethod.background = Color(0xF0, 0xF0, 0x30)
        }
        enabledFollowBtn(true)

        repaint()

        return
    }

    fun setSaveLogFile() {
        val dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HH.mm.ss")
        var device = deviceCombo.selectedItem!!.toString()
        device = device.substringBefore(":")
        if (logCmdManager.prefix.isEmpty()) {
            logCmdManager.prefix = LogCmdManager.DEFAULT_PREFIX
        }

        val filePath = "${logCmdManager.logSavePath}/${logCmdManager.prefix}_${device}_${dtf.format(LocalDateTime.now())}.txt"
        var file = File(filePath)
        var idx = 1
        var filePathSaved = filePath
        while (file.isFile) {
            filePathSaved = "${filePath}-$idx.txt"
            file = File(filePathSaved)
            idx++
        }

        fullTableModel.setLogFile(filePathSaved)
        filteredTableModel.setLogFile(filePathSaved)
        statusTF.text = filePathSaved
    }

    fun startAdbScan(reconnect: Boolean) {
        if (logCmdManager.getType() == LogCmdManager.TYPE_CMD) {
            statusMethod.text = " ${Strings.CMD} "
        }
        else {
            statusMethod.text = " ${Strings.ADB} "
        }

        filteredTableModel.stopScan()
        filteredTableModel.stopFollow()
        pauseToggle.isSelected = false
        setSaveLogFile()
        if (reconnect) {
            logCmdManager.targetDevice = deviceCombo.selectedItem!!.toString()
            logCmdManager.startLogcat()
        }
        filteredTableModel.startScan()
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x00, 0x50, 0x00)
        }
        else {
            statusMethod.background = Color(0x90, 0xE0, 0x90)
        }

        enabledFollowBtn(false)
    }

    fun stopAdbScan() {
        if (logCmdManager.getType() == LogCmdManager.TYPE_CMD) {
            statusMethod.text = " ${Strings.CMD} ${Strings.STOP} "
        }
        else {
            statusMethod.text = " ${Strings.ADB} ${Strings.STOP} "
        }

        if (!filteredTableModel.isScanning()) {
            println("stopAdbScan : not adb scanning mode")
            return
        }
        filteredTableModel.stopScan()
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x50, 0x50, 0x50)
        }
        else {
            statusMethod.background = Color.LIGHT_GRAY
        }

        enabledFollowBtn(true)
    }

    fun isRestartAdbLogcat(): Boolean {
        return retryAdbToggle.isSelected
    }

    fun restartAdbLogcat() {
        println("Restart Adb Logcat")
        logCmdManager.stop()
        logCmdManager.targetDevice = deviceCombo.selectedItem!!.toString()
        logCmdManager.startLogcat()
    }

    fun pauseAdbScan(pause: Boolean) {
        if (!filteredTableModel.isScanning()) {
            println("pauseAdbScan : not adb scanning mode")
            return
        }
        filteredTableModel.pauseScan(pause)
    }

    fun setFollowLogFile(filePath: String) {
        fullTableModel.setLogFile(filePath)
        filteredTableModel.setLogFile(filePath)
        statusTF.text = filePath
    }

    fun startFileFollow() {
        statusMethod.text = " ${Strings.FOLLOW} "
        filteredTableModel.stopScan()
        filteredTableModel.stopFollow()
        pauseFollowToggle.isSelected = false
        filteredTableModel.startFollow()

        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x00, 0x00, 0x50)
        }
        else {
            statusMethod.background = Color(0xA0, 0xA0, 0xF0)
        }

        enabledFollowBtn(true)
    }

    fun stopFileFollow() {
        if (!filteredTableModel.isFollowing()) {
            println("stopAdbScan : not file follow mode")
            return
        }
        statusMethod.text = " ${Strings.FOLLOW} ${Strings.STOP} "
        filteredTableModel.stopFollow()
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            statusMethod.background = Color(0x50, 0x50, 0x50)
        }
        else {
            statusMethod.background = Color.LIGHT_GRAY
        }
        enabledFollowBtn(true)
    }

    fun pauseFileFollow(pause: Boolean) {
        if (!filteredTableModel.isFollowing()) {
            println("pauseFileFollow : not file follow mode")
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
        override fun actionPerformed(p0: ActionEvent?) {
            when (p0?.source) {
                itemFileOpen -> {
                    val fileDialog = FileDialog(this@MainUI, Strings.FILE + " " + Strings.OPEN, FileDialog.LOAD)
                    fileDialog.isMultipleMode = false
                    fileDialog.directory = fullTableModel.logFile?.parent
                    fileDialog.isVisible = true
                    if (fileDialog.file != null) {
                        val file = File(fileDialog.directory + fileDialog.file)
                        openFile(file.absolutePath, false)
                    } else {
                        println("Cancel Open")
                    }
                }
                itemFileFollow -> {
                    val fileDialog = FileDialog(this@MainUI, Strings.FILE + " " + Strings.FOLLOW, FileDialog.LOAD)
                    fileDialog.isMultipleMode = false
                    fileDialog.directory = fullTableModel.logFile?.parent
                    fileDialog.isVisible = true
                    if (fileDialog.file != null) {
                        val file = File(fileDialog.directory + fileDialog.file)
                        setFollowLogFile(file.absolutePath)
                        startFileFollow()
                    } else {
                        println("Cancel Open")
                    }
                }
                itemFileOpenFiles -> {
                    val fileDialog = FileDialog(this@MainUI, Strings.FILE + " " + Strings.OPEN_FILES, FileDialog.LOAD)
                    fileDialog.isMultipleMode = true
                    fileDialog.directory = fullTableModel.logFile?.parent
                    fileDialog.isVisible = true
                    val fileList = fileDialog.files
                    if (fileList != null) {
                        var isFirst = true
                        for (file in fileList) {
                            if (isFirst) {
                                openFile(file.absolutePath, false)
                                isFirst = false
                            } else {
                                openFile(file.absolutePath, true)
                            }
                        }
                    } else {
                        println("Cancel Open")
                    }
                }
                itemFileAppendFiles -> {
                    val fileDialog = FileDialog(this@MainUI, Strings.FILE + " " + Strings.APPEND_FILES, FileDialog.LOAD)
                    fileDialog.isMultipleMode = true
                    fileDialog.directory = fullTableModel.logFile?.parent
                    fileDialog.isVisible = true
                    val fileList = fileDialog.files
                    if (fileList != null) {
                        for (file in fileList) {
                            openFile(file.absolutePath, true)
                        }
                    } else {
                        println("Cancel Open")
                    }
                }
                itemFileExit -> {
                    exit()
                }
                itemlogCmd, itemlogFile -> {
                    val settingsDialog = LogCmdSettingsDialog(this@MainUI)
                    settingsDialog.setLocationRelativeTo(this@MainUI)
                    settingsDialog.isVisible = true
                }
                itemFull -> {
                    if (itemFull.state) {
                        attachLogPanel(fullLogPanel)
                    } else {
                        windowedModeLogPanel(fullLogPanel)
                    }

                    configManager.saveItem(ConfigManager.ITEM_VIEW_FULL, itemFull.state.toString())
                }

                itemSearch -> {
                    searchPanel.isVisible = !searchPanel.isVisible
                    itemSearch.state = searchPanel.isVisible
                }

                itemfilterIncremental -> {
                    configManager.saveItem(ConfigManager.ITEM_FILTER_INCREMENTAL, itemfilterIncremental.state.toString())
                }
                itemAppearance -> {
                    val appearanceSettingsDialog = AppearanceSettingsDialog(this@MainUI)
                    appearanceSettingsDialog.setLocationRelativeTo(this@MainUI)
                    appearanceSettingsDialog.isVisible = true
                }
                itemAbout -> {
                    val aboutDialog = AboutDialog(this@MainUI)
                    aboutDialog.setLocationRelativeTo(this@MainUI)
                    aboutDialog.isVisible = true
                }
                itemHelp -> {
                    val helpDialog = HelpDialog(this@MainUI)
                    helpDialog.setLocationRelativeTo(this@MainUI)
                    helpDialog.isVisible = true
                }
                adbConnectBtn -> {
                    stopAdbScan()
                    logCmdManager.targetDevice = deviceCombo.selectedItem!!.toString()
                    logCmdManager.connect()
                }
                adbRefreshBtn -> {
                    logCmdManager.getDevices()
                }
                adbDisconnectBtn -> {
                    stopAdbScan()
                    logCmdManager.disconnect()
                }
                scrollBackApplyBtn -> {
                    try {
                        filteredTableModel.scrollback = scrollBackTF.text.toString().trim().toInt()
                    } catch (e: java.lang.NumberFormatException) {
                        filteredTableModel.scrollback = 0
                        scrollBackTF.text = "0"
                    }
                    filteredTableModel.scrollBackSplitFile = scrollBackSplitFileToggle.isSelected

                    configManager.saveItem(ConfigManager.ITEM_SCROLL_BACK, scrollBackTF.text)
                    configManager.saveItem(ConfigManager.ITEM_SCROLL_BACK_SPLIT_FILE, scrollBackSplitFileToggle.isSelected.toString())
                }
                startBtn -> {
                    startAdbScan(true)
                }
                stopBtn -> {
                    stopAdbScan()
                    logCmdManager.stop()
    //            } else if (p0?.source == pauseBtn) {
                }
                clearViewsBtn -> {
                    filteredTableModel.clearItems()
                    repaint()
                }
                saveBtn -> {
    //                filteredTableModel.clearItems()
                    if (filteredTableModel.isScanning()) {
                        setSaveLogFile()
                    }
                    else {
                        println("SaveBtn : not adb scanning mode")
                    }
    //                repaint()
                }
                itemRotation -> {
                    rotationStatus++

                    if (rotationStatus > ROTATION_MAX) {
                        rotationStatus = ROTATION_LEFT_RIGHT
                    }

                    configManager.saveItem(ConfigManager.ITEM_ROTATION, rotationStatus.toString())

                    logSplitPane.remove(filteredLogPanel)
                    logSplitPane.remove(fullLogPanel)
                    when (rotationStatus) {
                        ROTATION_LEFT_RIGHT -> {
                            logSplitPane.orientation = JSplitPane.HORIZONTAL_SPLIT
                            logSplitPane.add(filteredLogPanel)
                            logSplitPane.add(fullLogPanel)
                            logSplitPane.resizeWeight = SPLIT_WEIGHT
                        }
                        ROTATION_RIGHT_LEFT -> {
                            logSplitPane.orientation = JSplitPane.HORIZONTAL_SPLIT
                            logSplitPane.add(fullLogPanel)
                            logSplitPane.add(filteredLogPanel)
                            logSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
                        }
                        ROTATION_TOP_BOTTOM -> {
                            logSplitPane.orientation = JSplitPane.VERTICAL_SPLIT
                            logSplitPane.add(filteredLogPanel)
                            logSplitPane.add(fullLogPanel)
                            logSplitPane.resizeWeight = SPLIT_WEIGHT
                        }
                        ROTATION_BOTTOM_TOP -> {
                            logSplitPane.orientation = JSplitPane.VERTICAL_SPLIT
                            logSplitPane.add(fullLogPanel)
                            logSplitPane.add(filteredLogPanel)
                            logSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
                        }
                    }
                }
//                filtersBtn -> {
//                    filtersManager.showDialog()
//                }
//                cmdsBtn -> {
//                    cmdsManager.showDialog()
//                }
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
        var itemIconText: JMenuItem = JMenuItem("IconText")
        var itemIcon: JMenuItem = JMenuItem("Icon")
        var itemText: JMenuItem = JMenuItem("Text")
        private val actionHandler = ActionHandler()

        init {
           itemIconText.addActionListener(actionHandler)
            add(itemIconText)
           itemIcon.addActionListener(actionHandler)
            add(itemIcon)
           itemText.addActionListener(actionHandler)
            add(itemText)
        }
        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(p0: ActionEvent?) {
                when (p0?.source) {
                   itemIconText -> {
                        setBtnIcons(true)
                        setBtnTexts(true)
                        configManager.saveItem(ConfigManager.ITEM_ICON_TEXT, ConfigManager.VALUE_ICON_TEXT_I_T)
                    }
                   itemIcon -> {
                        setBtnIcons(true)
                        setBtnTexts(false)
                        configManager.saveItem(ConfigManager.ITEM_ICON_TEXT, ConfigManager.VALUE_ICON_TEXT_I)
                    }
                   itemText -> {
                        setBtnIcons(false)
                        setBtnTexts(true)
                        configManager.saveItem(ConfigManager.ITEM_ICON_TEXT, ConfigManager.VALUE_ICON_TEXT_I)
                    }
                }
            }
        }
    }
    internal inner class FrameMouseListener(private val frame: JFrame) : MouseAdapter() {
        private var mouseDownCompCoords: Point? = null

        private var popupMenu: JPopupMenu? = null
        override fun mouseReleased(e: MouseEvent) {
            mouseDownCompCoords = null

            if (SwingUtilities.isRightMouseButton(e)) {
                if (e.source == this@MainUI.contentPane) {
                    popupMenu = FramePopUp()
                    popupMenu?.show(e.component, e.x, e.y)
                }
            }
            else {
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
        var selectAllItem: JMenuItem
        var copyItem: JMenuItem
        var pasteItem: JMenuItem
        var reconnectItem: JMenuItem
        var combo: JComboBox<String>?
        private val actionHandler = ActionHandler()

        init {
            selectAllItem = JMenuItem("Select All")
            selectAllItem.addActionListener(actionHandler)
            add(selectAllItem)
            copyItem = JMenuItem("Copy")
            copyItem.addActionListener(actionHandler)
            add(copyItem)
            pasteItem = JMenuItem("Paste")
            pasteItem.addActionListener(actionHandler)
            add(pasteItem)
            reconnectItem = JMenuItem("Reconnect " + deviceCombo.selectedItem?.toString())
            reconnectItem.addActionListener(actionHandler)
            add(reconnectItem)
            this.combo = combo
        }
        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(p0: ActionEvent?) {
                when (p0?.source) {
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
                    item.foreground = Color.decode(ColorManager.getInstance().filterTableColor.strFilteredFGs[num])
                    item.background = Color.decode(ColorManager.getInstance().filterTableColor.strFilteredBGs[num])
                    item.addActionListener(actionHandler)
                    addColorTagItems.add(item)
                    add(item)
                }
            }
        }
        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(p0: ActionEvent?) {
                when (p0?.source) {
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
                        val item = p0?.source as JMenuItem
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
        override fun mouseClicked(p0: MouseEvent?) {
            super.mouseClicked(p0)
        }

        private var popupMenu: JPopupMenu? = null
        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                when (event.source) {
                    deviceCombo.editor.editorComponent -> {
                        popupMenu = PopUpCombobox(deviceCombo)
                        popupMenu?.show(event.component, event.x, event.y)
                    }
                    showLogCombo.editor.editorComponent, boldLogCombo.editor.editorComponent, showTagCombo.editor.editorComponent, showPidCombo.editor.editorComponent, showTidCombo.editor.editorComponent -> {
                        lateinit var combo: FilterComboBox
                        when (event.source) {
                            showLogCombo.editor.editorComponent -> {
                                combo = showLogCombo
                            }
                            boldLogCombo.editor.editorComponent -> {
                                combo = boldLogCombo
                            }
                            showTagCombo.editor.editorComponent -> {
                                combo = showTagCombo
                            }
                            showPidCombo.editor.editorComponent -> {
                                combo = showPidCombo
                            }
                            showTidCombo.editor.editorComponent -> {
                                combo = showTidCombo
                            }
                        }
                        popupMenu = PopUpFilterCombobox(combo)
                        popupMenu?.show(event.component, event.x, event.y)
                    }
                    else -> {
                        val compo = event.source as JComponent
                        val event = MouseEvent(compo.parent, event.id, event.`when`, event.modifiers, event.x + compo.x, event.y + compo.y, event.clickCount, event.isPopupTrigger)

                        compo.parent.dispatchEvent(event)
                    }
                }
            }
            else {
                popupMenu?.isVisible = false
            }

            super.mouseReleased(event)
        }
    }

    fun reconnectAdb() {
        println("Reconnect ADB")
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

//    fun clearSaveAdbLog() {
//        Thread(Runnable {
//            run {
//                saveBtn.doClick()
//            }
//        }).start()
//    }

    fun getTextShowLogCombo() : String {
        if (showLogCombo.selectedItem == null) {
           return ""
        }
        return showLogCombo.selectedItem!!.toString()
    }

    fun setTextShowLogCombo(text : String) {
        showLogCombo.selectedItem = text
        showLogCombo.updateTooltip()
    }

    fun getTextSearchCombo() : String {
        if (searchPanel.searchCombo.selectedItem == null) {
            return ""
        }
        return searchPanel.searchCombo.selectedItem!!.toString()
    }

    fun setTextSearchCombo(text : String) {
        searchPanel.searchCombo.selectedItem = text
        filteredTableModel.filterSearchLog = searchPanel.searchCombo.selectedItem!!.toString()
        searchPanel.isVisible = true
        itemSearch.state = searchPanel.isVisible
    }

    fun applyShowLogCombo() {
        val item = showLogCombo.selectedItem!!.toString()
        resetComboItem(showLogCombo, item)
        filteredTableModel.filterLog = item
    }

    fun applyShowLogComboEditor() {
        val editorCom = showLogCombo.editor?.editorComponent as JTextComponent
        val text = editorCom.text
        setTextShowLogCombo(text)
        applyShowLogCombo()
    }
    fun setDeviceComboColor(isConnected: Boolean) {
        if (isConnected) {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                deviceCombo.editor.editorComponent.foreground = Color(0x7070C0)
            }
            else {
                deviceCombo.editor.editorComponent.foreground = Color.BLUE
            }
        } else {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                deviceCombo.editor.editorComponent.foreground = Color(0xC07070)
            }
            else {
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
                logCmd = configManager.getItem("${ConfigManager.ITEM_ADB_LOG_CMD}_$i")
                if (logCmd.isNullOrBlank()) {
                    continue
                }

                logCmdCombo.addItem(logCmd)
            }
            logCmdCombo.selectedIndex = -1
            if (currLogCmd.isBlank()) {
                logCmdCombo.editor.item = logCmdManager.logCmd
            }
            else {
                logCmdCombo.editor.item = currLogCmd
            }
        }

        logCmdCombo.toolTipText = "\"${logCmdManager.logCmd}\"\n\n${TooltipStrings.LOG_CMD_COMBO}"

        if (logCmdManager.logCmd == logCmdCombo.editor.item.toString()) {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                logCmdCombo.editor.editorComponent.foreground = Color(0x7070C0)
            }
            else {
                logCmdCombo.editor.editorComponent.foreground = Color.BLUE
            }
        } else {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                logCmdCombo.editor.editorComponent.foreground = Color(0xC07070)
            }
            else {
                logCmdCombo.editor.editorComponent.foreground = Color.RED
            }
        }
    }

    internal inner class KeyHandler : KeyAdapter() {
        override fun keyReleased(p0: KeyEvent?) {
            if (KeyEvent.VK_ENTER != p0?.keyCode && p0?.source == logCmdCombo.editor.editorComponent) {
                updateLogCmdCombo(false)
            }

            if (KeyEvent.VK_ENTER == p0?.keyCode) {
                when {
                    p0.source == showLogCombo.editor.editorComponent && showLogToggle.isSelected -> {
                        val combo = showLogCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        filteredTableModel.filterLog = item
                    }
                    p0.source == boldLogCombo.editor.editorComponent && boldLogToggle.isSelected -> {
                        val combo = boldLogCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        filteredTableModel.filterHighlightLog = item
                    }
                    p0.source == showTagCombo.editor.editorComponent && showTagToggle.isSelected -> {
                        val combo = showTagCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        filteredTableModel.filterTag = item
                    }
                    p0.source == showPidCombo.editor.editorComponent && showPidToggle.isSelected -> {
                        val combo = showPidCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        filteredTableModel.filterPid = item
                    }
                    p0.source == showTidCombo.editor.editorComponent && showTidToggle.isSelected -> {
                        val combo = showTidCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        filteredTableModel.filterTid = item
                    }
                    p0.source == logCmdCombo.editor.editorComponent -> {
                        if (logCmdManager.logCmd == logCmdCombo.editor.item.toString()) {
                            reconnectAdb()
                        }
                        else {
                            val item = logCmdCombo.editor.item.toString().trim()

                            if (item.isEmpty()) {
                                logCmdCombo.editor.item = LogCmdManager.DEFAULT_LOGCAT
                            }
                            logCmdManager.logCmd = logCmdCombo.editor.item.toString()
                            updateLogCmdCombo(false)
                        }
                    }
                    p0.source == deviceCombo.editor.editorComponent -> {
                        reconnectAdb()
                    }
                    p0.source == scrollBackTF -> {
                        scrollBackApplyBtn.doClick()
                    }
                }
            } else if (p0 != null && itemfilterIncremental.state) {
                when {
                    p0.source == showLogCombo.editor.editorComponent && showLogToggle.isSelected -> {
                        val item = showLogCombo.editor.item.toString()
                        filteredTableModel.filterLog = item
                    }
                    p0.source == boldLogCombo.editor.editorComponent && boldLogToggle.isSelected -> {
                        val item = boldLogCombo.editor.item.toString()
                        filteredTableModel.filterHighlightLog = item
                    }
                    p0.source == showTagCombo.editor.editorComponent && showTagToggle.isSelected -> {
                        val item = showTagCombo.editor.item.toString()
                        filteredTableModel.filterTag = item
                    }
                    p0.source == showPidCombo.editor.editorComponent && showPidToggle.isSelected -> {
                        val item = showPidCombo.editor.item.toString()
                        filteredTableModel.filterPid = item
                    }
                    p0.source == showTidCombo.editor.editorComponent && showTidToggle.isSelected -> {
                        val item = showTidCombo.editor.item.toString()
                        filteredTableModel.filterTid = item
                    }
                }
            }
            super.keyReleased(p0)
        }
    }

    internal inner class ItemHandler : ItemListener {
        override fun itemStateChanged(p0: ItemEvent?) {
            when (p0?.source) {
                showLogToggle -> {
                    showLogCombo.setEnabledFilter(showLogToggle.isSelected)
                }
                boldLogToggle -> {
                    boldLogCombo.setEnabledFilter(boldLogToggle.isSelected)
                }
                showTagToggle -> {
                    showTagCombo.setEnabledFilter(showTagToggle.isSelected)
                }
                showPidToggle -> {
                    showPidCombo.setEnabledFilter(showPidToggle.isSelected)
                }
                showTidToggle -> {
                    showTidCombo.setEnabledFilter(showTidToggle.isSelected)
                }
            }

            if (IsCreatingUI) {
                return
            }
            when (p0?.source) {
                showLogToggle -> {
                    if (showLogToggle.isSelected && showLogCombo.selectedItem != null) {
                        filteredTableModel.filterLog = showLogCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterLog = ""
                    }
                    configManager.saveItem(ConfigManager.ITEM_SHOW_LOG_CHECK, showLogToggle.isSelected.toString())
                }
                boldLogToggle -> {
                    if (boldLogToggle.isSelected && boldLogCombo.selectedItem != null) {
                        filteredTableModel.filterHighlightLog = boldLogCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterHighlightLog = ""
                    }
                    configManager.saveItem(ConfigManager.ITEM_HIGHLIGHT_LOG_CHECK, boldLogToggle.isSelected.toString())
                }
                showTagToggle -> {
                    if (showTagToggle.isSelected && showTagCombo.selectedItem != null) {
                        filteredTableModel.filterTag = showTagCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterTag = ""
                    }
                    configManager.saveItem(ConfigManager.ITEM_SHOW_TAG_CHECK, showTagToggle.isSelected.toString())
                }
                showPidToggle -> {
                    if (showPidToggle.isSelected && showPidCombo.selectedItem != null) {
                        filteredTableModel.filterPid = showPidCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterPid = ""
                    }
                    configManager.saveItem(ConfigManager.ITEM_SHOW_PID_CHECK, showPidToggle.isSelected.toString())
                }
                showTidToggle -> {
                    if (showTidToggle.isSelected && showTidCombo.selectedItem != null) {
                        filteredTableModel.filterTid = showTidCombo.selectedItem!!.toString()
                    } else {
                        filteredTableModel.filterTid = ""
                    }
                    configManager.saveItem(ConfigManager.ITEM_SHOW_TID_CHECK, showTidToggle.isSelected.toString())
                }
                matchCaseToggle -> {
                    filteredTableModel.matchCase = matchCaseToggle.isSelected
                    configManager.saveItem(ConfigManager.ITEM_MATCH_CASE, matchCaseToggle.isSelected.toString())
                }
                scrollBackKeepToggle -> {
                    filteredTableModel.scrollBackKeep = scrollBackKeepToggle.isSelected
                }
                retryAdbToggle -> {
                    configManager.saveItem(ConfigManager.ITEM_RETRY_ADB, retryAdbToggle.isSelected.toString())
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

    internal inner class LevelItemHandler : ItemListener {
        override fun itemStateChanged(p0: ItemEvent?) {
            val item = p0?.source as JRadioButtonMenuItem
            when (item.text) {
                VERBOSE ->filteredTableModel.filterLevel = LEVEL_VERBOSE
                DEBUG ->filteredTableModel.filterLevel = LEVEL_DEBUG
                INFO ->filteredTableModel.filterLevel = LEVEL_INFO
                WARNING ->filteredTableModel.filterLevel = LEVEL_WARNING
                ERROR ->filteredTableModel.filterLevel = LEVEL_ERROR
                FATAL ->filteredTableModel.filterLevel = LEVEL_FATAL
            }
            configManager.saveItem(ConfigManager.ITEM_LOG_LEVEL, item.text)
        }
    }

    internal inner class AdbHandler : LogCmdManager.AdbEventListener {
        override fun changedStatus(event: LogCmdManager.AdbEvent) {
            when (event.cmd) {
                LogCmdManager.CMD_CONNECT -> {
                    logCmdManager.getDevices()
                }
                LogCmdManager.CMD_GET_DEVICES -> {
                    if (IsCreatingUI) {
                        return
                    }
                    var selectedItem = deviceCombo.selectedItem
                    deviceCombo.removeAllItems()
                    for (item in logCmdManager.devices) {
                        deviceCombo.addItem(item)
                    }
                    if (selectedItem == null) {
                        selectedItem = ""
                    }

                    if (logCmdManager.devices.contains(selectedItem.toString())) {
                        deviceStatus.text = Strings.CONNECTED
                        setDeviceComboColor(true)
                    } else {
                        var isExist = false
                        val deviceChk = "$selectedItem:"
                        for (device in logCmdManager.devices) {
                            if (device.contains(deviceChk)) {
                                isExist = true
                                selectedItem = device
                                break
                            }
                        }
                        if (isExist) {
                            deviceStatus.text = Strings.CONNECTED
                            setDeviceComboColor(true)
                        } else {
                            deviceStatus.text = Strings.NOT_CONNECTED
                            setDeviceComboColor(false)
                        }
                    }
                    deviceCombo.selectedItem = selectedItem
                }
                LogCmdManager.CMD_DISCONNECT -> {
                    logCmdManager.getDevices()
                }
            }
        }
    }

    internal inner class PopupMenuHandler : PopupMenuListener {
        private var isCanceled = false
        override fun popupMenuWillBecomeInvisible(p0: PopupMenuEvent?) {
            if (isCanceled) {
                isCanceled = false
                return
            }
            when (p0?.source) {
                showLogCombo -> {
                    if (showLogCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = showLogCombo
                    val item = combo.selectedItem!!.toString()
                    if (combo.editor.item.toString() != item) {
                        return
                    }
                    resetComboItem(combo, item)
                    filteredTableModel.filterLog = item
                    combo.updateTooltip()
                }
                boldLogCombo -> {
                    if (boldLogCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = boldLogCombo
                    val item = combo.selectedItem!!.toString()
                    resetComboItem(combo, item)
                    filteredTableModel.filterHighlightLog = item
                    combo.updateTooltip()
                }
                showTagCombo -> {
                    if (showTagCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = showTagCombo
                    val item = combo.selectedItem!!.toString()
                    resetComboItem(combo, item)
                    filteredTableModel.filterTag = item
                    combo.updateTooltip()
                }
                showPidCombo -> {
                    if (showPidCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = showPidCombo
                    val item = combo.selectedItem!!.toString()
                    resetComboItem(combo, item)
                    filteredTableModel.filterPid = item
                    combo.updateTooltip()
                }
                showTidCombo -> {
                    if (showTidCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = showTidCombo
                    val item = combo.selectedItem!!.toString()
                    resetComboItem(combo, item)
                    filteredTableModel.filterTid = item
                    combo.updateTooltip()
                }

                logCmdCombo -> {
                    if (logCmdCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = logCmdCombo
                    val item = combo.selectedItem!!.toString()
                    logCmdManager.logCmd = item
                    updateLogCmdCombo(false)
                }
            }
        }

        override fun popupMenuCanceled(p0: PopupMenuEvent?) {
            isCanceled = true
        }

        override fun popupMenuWillBecomeVisible(p0: PopupMenuEvent?) {
            val box = p0?.source as JComboBox<*>
            val comp = box.ui.getAccessibleChild(box, 0) as? JPopupMenu ?: return
            val scrollPane = comp.getComponent(0) as JScrollPane
            scrollPane.verticalScrollBar?.setUI(BasicScrollBarUI())
            scrollPane.horizontalScrollBar?.setUI(BasicScrollBarUI())
            isCanceled = false
        }
    }

    internal inner class ComponentHandler : ComponentAdapter() {
        override fun componentResized(p0: ComponentEvent?) {
            revalidate()
            super.componentResized(p0)
        }
    }

    fun resetComboItem(combo: FilterComboBox, item: String) {
        if (combo.isExistItem(item)) {
            if (combo.selectedIndex == 0) {
                return
            }
            combo.removeItem(item)
        }
        combo.insertItemAt(item, 0)
        combo.selectedIndex = 0
        return
    }

    fun goToLine(line: Int) {
        println("Line : $line")
        if (line < 0) {
            return
        }
        var num = 0
        for (idx in 0 until filteredTableModel.rowCount) {
            num = filteredTableModel.getValueAt(idx, 0).toString().trim().toInt()
            if (line <= num) {
                filteredLogPanel.goToRow(idx, 0)
                break
            }
        }

        if (line != num) {
            for (idx in 0 until fullTableModel.rowCount) {
                num = fullTableModel.getValueAt(idx, 0).toString().trim().toInt()
                if (line <= num) {
                    fullLogPanel.goToRow(idx, 0)
                    break
                }
            }
        }
    }

    fun markLine() {
        if (IsCreatingUI) {
            return
        }
        selectedLine = filteredLogPanel.getSelectedLine()
    }

    fun getMarkLine(): Int {
        return selectedLine
    }

    fun goToMarkedLine() {
        if (IsCreatingUI) {
            return
        }
        goToLine(selectedLine)
    }

    fun updateUIAfterVisible(args: Array<String>) {
        if (showLogCombo.selectedIndex >= 0 && (showLogComboStyle == FilterComboBox.Mode.MULTI_LINE || showLogComboStyle == FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT)) {
            val selectedItem = showLogCombo.selectedItem
            showLogCombo.selectedItem = ""
            showLogCombo.selectedItem = selectedItem
            showLogCombo.parent.revalidate()
            showLogCombo.parent.repaint()
        }
        if (showTagCombo.selectedIndex >= 0 && (showTagComboStyle == FilterComboBox.Mode.MULTI_LINE || showTagComboStyle == FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT)) {
            val selectedItem = showTagCombo.selectedItem
            showTagCombo.selectedItem = ""
            showTagCombo.selectedItem = selectedItem
            showTagCombo.parent.revalidate()
            showTagCombo.parent.repaint()
        }
        if (boldLogCombo.selectedIndex >= 0 && (boldLogComboStyle == FilterComboBox.Mode.MULTI_LINE || boldLogComboStyle == FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT)) {
            val selectedItem = boldLogCombo.selectedItem
            boldLogCombo.selectedItem = ""
            boldLogCombo.selectedItem = selectedItem
            boldLogCombo.parent.revalidate()
            boldLogCombo.parent.repaint()
        }
        colorManager.applyFilterStyle()

        showLogCombo.enabledTfTooltip = true
        showTagCombo.enabledTfTooltip = true
        showPidCombo.enabledTfTooltip = true
        showTidCombo.enabledTfTooltip = true

        var isFirst = true
        for (fileName in args) {
            val file = File(fileName)
            if (file.isFile) {
                if (isFirst) {
                    openFile(file.absolutePath, false)
                    isFirst = false
                } else {
                    openFile(file.absolutePath, true)
                }
            }
        }
    }

    fun repaintUI() {
    }

    internal inner class StatusTextField(text: String?) : JTextField(text) {
        private var prevText = ""
        override fun getToolTipText(event: MouseEvent?): String? {
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
        var searchCombo: FilterComboBox
        var searchMatchCaseToggle: ColorToggleButton
        private var targetLabel: JLabel
        private var upBtn: ColorButton
        private var downBtn: ColorButton
        var closeBtn: ColorButton

        var isInternalTargetView = true  // true : filter view, false : full view

        private val searchActionHandler = SearchActionHandler()
        private val searchKeyHandler = SearchKeyHandler()
        private val searchPopupMenuHandler = SearchPopupMenuHandler()

        init {
            searchCombo = FilterComboBox(FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT, false)
            searchCombo.preferredSize = Dimension(700, searchCombo.preferredSize.height)
            if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
                searchCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 5)
            }

            searchCombo.toolTipText = TooltipStrings.SEARCH_COMBO
            searchCombo.enabledTfTooltip = false
            searchCombo.isEditable = true
            searchCombo.renderer = FilterComboBox.ComboBoxRenderer()
            searchCombo.editor.editorComponent.addKeyListener(searchKeyHandler)
            searchCombo.addPopupMenuListener(searchPopupMenuHandler)

            searchMatchCaseToggle = ColorToggleButton("Aa")
            searchMatchCaseToggle.toolTipText = TooltipStrings.SEARCH_CASE_TOGGLE
            searchMatchCaseToggle.margin = Insets(0, 0, 0, 0)
            searchMatchCaseToggle.addItemListener(SearchItemHandler())
            searchMatchCaseToggle.background = background
            searchMatchCaseToggle.border = BorderFactory.createEmptyBorder()

            upBtn = ColorButton("▲") //△ ▲ ▽ ▼
            upBtn.toolTipText = TooltipStrings.SEARCH_PREV_BTN
            upBtn.margin = Insets(0, 7, 0, 7)
            upBtn.addActionListener(searchActionHandler)
            upBtn.background = background
            upBtn.border = BorderFactory.createEmptyBorder()

            downBtn = ColorButton("▼") //△ ▲ ▽ ▼
            downBtn.toolTipText = TooltipStrings.SEARCH_NEXT_BTN
            downBtn.margin = Insets(0, 7, 0, 7)
            downBtn.addActionListener(searchActionHandler)
            downBtn.background = background
            downBtn.border = BorderFactory.createEmptyBorder()

            targetLabel = if (isInternalTargetView) {
                JLabel("${Strings.FILTER} ${Strings.LOG}")
            } else {
                JLabel("${Strings.FULL} ${Strings.LOG}")
            }
            targetLabel.toolTipText = TooltipStrings.SEARCH_TARGET_LABEL

            closeBtn = ColorButton("X")
            closeBtn.toolTipText = TooltipStrings.SEARCH_CLOSE_BTN
            closeBtn.margin = Insets(0, 0, 0, 0)
            closeBtn.addActionListener(searchActionHandler)
            closeBtn.background = background
            closeBtn.border = BorderFactory.createEmptyBorder()


            val searchPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
            searchPanel.add(searchCombo)
            searchPanel.add(searchMatchCaseToggle)
            searchPanel.add(upBtn)
            searchPanel.add(downBtn)

            val statusPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 2))
            statusPanel.add(targetLabel)
            statusPanel.add(closeBtn)

            layout = BorderLayout()
            add(searchPanel, BorderLayout.WEST)
            add(statusPanel, BorderLayout.EAST)
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
                targetLabel.text = "${Strings.FILTER} ${Strings.LOG}"
            } else {
                targetLabel.text = "${Strings.FULL} ${Strings.LOG}"
            }
        }

        fun moveToNext() {
            if (isInternalTargetView) {
                filteredTableModel.moveToNextSearch()
            }
            else {
                fullTableModel.moveToNextSearch()
            }
        }

        fun moveToPrev() {
            if (isInternalTargetView) {
                filteredTableModel.moveToPrevSearch()
            }
            else {
                fullTableModel.moveToPrevSearch()
            }
        }

        internal inner class SearchActionHandler : ActionListener {
            override fun actionPerformed(p0: ActionEvent?) {
                when (p0?.source) {
                    upBtn -> {
                        moveToPrev()
                    }
                    downBtn -> {
                        moveToNext()
                    }
                    closeBtn -> {
                        searchPanel.isVisible = false
                        itemSearch.state = searchPanel.isVisible
                    }
                }
            }
        }

        internal inner class SearchKeyHandler : KeyAdapter() {
            override fun keyReleased(p0: KeyEvent?) {
                if (KeyEvent.VK_ENTER == p0?.keyCode) {
                    when (p0.source) {
                        searchCombo.editor.editorComponent -> {
                            val item = searchCombo.selectedItem!!.toString()
                            resetComboItem(searchCombo, item)
                            filteredTableModel.filterSearchLog = item
                            if (KeyEvent.SHIFT_MASK == p0.modifiers) {
                                moveToPrev()
                            }
                            else {
                                moveToNext()
                            }
                        }
                    }
                }
                super.keyReleased(p0)
            }
        }
        internal inner class SearchPopupMenuHandler : PopupMenuListener {
            private var isCanceled = false
            override fun popupMenuWillBecomeInvisible(p0: PopupMenuEvent?) {
                if (isCanceled) {
                    isCanceled = false
                    return
                }
                when (p0?.source) {
                    searchCombo -> {
                        if (searchCombo.selectedIndex < 0) {
                            return
                        }
                        val item = searchCombo.selectedItem!!.toString()
                        resetComboItem(searchCombo, item)
                        filteredTableModel.filterSearchLog = item
                        searchCombo.updateTooltip()
                    }
                }
            }

            override fun popupMenuCanceled(p0: PopupMenuEvent?) {
                isCanceled = true
            }

            override fun popupMenuWillBecomeVisible(p0: PopupMenuEvent?) {
                val box = p0?.source as JComboBox<*>
                val comp = box.ui.getAccessibleChild(box, 0) as? JPopupMenu ?: return
                val scrollPane = comp.getComponent(0) as JScrollPane
                scrollPane.verticalScrollBar?.setUI(BasicScrollBarUI())
                scrollPane.horizontalScrollBar?.setUI(BasicScrollBarUI())
                isCanceled = false
            }
        }

        internal inner class SearchItemHandler : ItemListener {
            override fun itemStateChanged(p0: ItemEvent?) {
                if (IsCreatingUI) {
                    return
                }
                when (p0?.source) {
                    searchMatchCaseToggle -> {
                        filteredTableModel.SearchMatchCase = searchMatchCaseToggle.isSelected
                        configManager.saveItem(ConfigManager.ITEM_SEARCH_MATCH_CASE, searchMatchCaseToggle.isSelected.toString())
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
                itemSearch.state = searchPanel.isVisible
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK)
        actionMapKey = javaClass.name + ":SEARCH_OPENING"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                searchPanel.isVisible = true
                itemSearch.state = searchPanel.isVisible
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
            filteredLogPanel
        }
        else {
            fullLogPanel
        }

        targetPanel.toolTipText = result
        if (isNext) {
            ToolTipManager.sharedInstance().mouseMoved(MouseEvent(targetPanel, 0, 0, 0, targetPanel.width / 3, targetPanel.height - 50, 0, false))
        }
        else {
            ToolTipManager.sharedInstance().mouseMoved(MouseEvent(targetPanel, 0, 0, 0, targetPanel.width / 3, 0, 0, false))
        }

        val clearThread = Thread {
            run {
                Thread.sleep(1000)
                SwingUtilities.invokeAndWait {
                    targetPanel.toolTipText = ""
                }
            }
        }

        clearThread.start()
    }

    inner class FocusHandler(isFilter: Boolean) : FocusAdapter() {
        val isFilter = isFilter
        override fun focusGained(e: FocusEvent?) {
            super.focusGained(e)
            searchPanel.setTargetView(isFilter)
        }
    }
}

