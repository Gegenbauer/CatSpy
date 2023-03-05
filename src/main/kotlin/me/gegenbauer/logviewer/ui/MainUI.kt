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

    private lateinit var mMenuBar: JMenuBar
    private lateinit var mMenuFile: JMenu
    private lateinit var mItemFileOpen: JMenuItem
    private lateinit var mItemFileFollow: JMenuItem
    private lateinit var mItemFileOpenFiles: JMenuItem
    private lateinit var mItemFileAppendFiles: JMenuItem
//    private lateinit var mItemFileOpenRecents: JMenu
    private lateinit var mItemFileExit: JMenuItem
    private lateinit var mMenuView: JMenu
    private lateinit var mItemFull: JCheckBoxMenuItem
    private lateinit var mItemSearch: JCheckBoxMenuItem
    private lateinit var mItemRotation: JMenuItem
    private lateinit var mMenuSettings: JMenu
    private lateinit var mItemLogCmd: JMenuItem
    private lateinit var mItemLogFile: JMenuItem
    private lateinit var mItemFilterIncremental: JCheckBoxMenuItem
    private lateinit var mMenuLogLevel: JMenu
    private lateinit var mLogLevelGroup: ButtonGroup
    private lateinit var mItemAppearance: JMenuItem
    private lateinit var mMenuHelp: JMenu
    private lateinit var mItemHelp: JMenuItem
    private lateinit var mItemAbout: JMenuItem

    private lateinit var mFilterPanel: JPanel
    private lateinit var mFilterLeftPanel: JPanel

    private lateinit var mLogToolBar: ButtonPanel
    private lateinit var mStartBtn: ColorButton
    private lateinit var mRetryAdbToggle: ColorToggleButton
    private lateinit var mStopBtn: ColorButton
    private lateinit var mPauseToggle: ColorToggleButton
    private lateinit var mClearViewsBtn: ColorButton
    private lateinit var mSaveBtn: ColorButton
//    private lateinit var mRotationBtn: ColorButton
//    lateinit var mFiltersBtn: ColorButton
//    lateinit var mCmdsBtn: ColorButton
    internal lateinit var mSearchPanel: SearchPanel

    private lateinit var mLogPanel: JPanel
    private lateinit var mShowLogPanel: JPanel
    private lateinit var mMatchCaseToggle: ColorToggleButton
    private lateinit var mMatchCaseTogglePanel: JPanel
    lateinit var mShowLogCombo: FilterComboBox
    var mShowLogComboStyle: FilterComboBox.Mode
    private lateinit var mShowLogToggle: ColorToggleButton
    private lateinit var mShowLogTogglePanel: JPanel

    private lateinit var mBoldLogPanel: JPanel
    private lateinit var mBoldLogCombo: FilterComboBox
    var mBoldLogComboStyle: FilterComboBox.Mode
    private lateinit var mBoldLogToggle: ColorToggleButton
    private lateinit var mBoldLogTogglePanel: JPanel

    private lateinit var mShowTagPanel: JPanel
    lateinit var mShowTagCombo: FilterComboBox
    var mShowTagComboStyle: FilterComboBox.Mode
    private lateinit var mShowTagToggle: ColorToggleButton
    private lateinit var mShowTagTogglePanel: JPanel

    private lateinit var mShowPidPanel: JPanel
    lateinit var mShowPidCombo: FilterComboBox
    var mShowPidComboStyle: FilterComboBox.Mode
    private lateinit var mShowPidToggle: ColorToggleButton
    private lateinit var mShowPidTogglePanel: JPanel

    private lateinit var mShowTidPanel: JPanel
    lateinit var mShowTidCombo: FilterComboBox
    var mShowTidComboStyle: FilterComboBox.Mode
    private lateinit var mShowTidToggle: ColorToggleButton
    private lateinit var mShowTidTogglePanel: JPanel

    private lateinit var mLogCmdCombo: ColorComboBox<String>

    private lateinit var mDeviceCombo: ColorComboBox<String>
    private lateinit var mDeviceStatus: JLabel
    private lateinit var mAdbConnectBtn: ColorButton
    private lateinit var mAdbRefreshBtn: ColorButton
    private lateinit var mAdbDisconnectBtn: ColorButton

    private lateinit var mScrollbackLabel: JLabel
    private lateinit var mScrollbackTF: JTextField
    private lateinit var mScrollbackSplitFileToggle: ColorToggleButton
    private lateinit var mScrollbackApplyBtn: ColorButton
    private lateinit var mScrollbackKeepToggle: ColorToggleButton

    lateinit var mFilteredTableModel: LogTableModel
        private set

    private lateinit var mFullTableModel: LogTableModel

    lateinit var mLogSplitPane: JSplitPane

    lateinit var mFilteredLogPanel: LogPanel
    lateinit var mFullLogPanel: LogPanel
    private var mSelectedLine = 0

    private lateinit var mStatusBar: JPanel
    private lateinit var mStatusMethod: JLabel
    private lateinit var mStatusTF: JTextField

    private lateinit var mFollowLabel: JLabel
    private lateinit var mStartFollowBtn: ColorButton
    private lateinit var mStopFollowBtn: ColorButton
    private lateinit var mPauseFollowToggle: ColorToggleButton

    private val mFrameMouseListener = FrameMouseListener(this)
    private val mKeyHandler = KeyHandler()
    private val mItemHandler = ItemHandler()
    private val mLevelItemHandler = LevelItemHandler()
    private val mActionHandler = ActionHandler()
    private val mPopupMenuHandler = PopupMenuHandler()
    private val mMouseHandler = MouseHandler()
    private val mComponentHandler = ComponentHandler()
    private val mStatusChangeListener = StatusChangeListener()

    val mConfigManager = ConfigManager.getInstance()
    private val mColorManager = ColorManager.getInstance()

    private val mLogCmdManager = LogCmdManager.getInstance()
    lateinit var mFiltersManager: FiltersManager
    lateinit var mCmdsManager: CmdsManager

    private var mFrameX = 0
    private var mFrameY = 0
    private var mFrameWidth = 1280
    private var mFrameHeight = 720
    private var mFrameExtendedState = Frame.MAXIMIZED_BOTH

    private var mRotationStatus = ROTATION_LEFT_RIGHT

    var mFont: Font = Font(DEFAULT_FONT_NAME, Font.PLAIN, 12)
        set(value) {
            field = value
            if (!IsCreatingUI) {
                mFilteredLogPanel.mFont = value
                mFullLogPanel.mFont = value
            }
        }

    var mUIFontPercent = 100

    init {
        loadConfigOnCreate()
        mLogCmdManager.setMainUI(this)

        val laf = mConfigManager.getItem(ConfigManager.ITEM_LOOK_AND_FEEL)

        if (laf == null) {
            ConfigManager.LaF = FLAT_LIGHT_LAF
        }
        else {
            ConfigManager.LaF = laf
        }

        val uiFontSize = mConfigManager.getItem(ConfigManager.ITEM_UI_FONT_SIZE)
        if (!uiFontSize.isNullOrEmpty()) {
            mUIFontPercent = uiFontSize.toInt()
        }

        if (ConfigManager.LaF == FLAT_LIGHT_LAF || ConfigManager.LaF == FLAT_DARK_LAF) {
            System.setProperty("flatlaf.uiScale", "$mUIFontPercent%")
        }
        else {
            initFontSize(mUIFontPercent)
        }

        setLaF(ConfigManager.LaF)

        val cmd = mConfigManager.getItem(ConfigManager.ITEM_ADB_CMD)
        if (!cmd.isNullOrEmpty()) {
            mLogCmdManager.adbCmd = cmd
        } else {
            val os = System.getProperty("os.name")
            println("OS : $os")
            if (os.lowercase().contains("windows")) {
                mLogCmdManager.adbCmd = "adb.exe"
            } else {
                mLogCmdManager.adbCmd = "adb"
            }
        }
        mLogCmdManager.addEventListener(AdbHandler())
        val logSavePath = mConfigManager.getItem(ConfigManager.ITEM_ADB_LOG_SAVE_PATH)
        if (logSavePath.isNullOrEmpty()) {
            mLogCmdManager.logSavePath = "."
        } else {
            mLogCmdManager.logSavePath = logSavePath
        }

        val logCmd = mConfigManager.getItem(ConfigManager.ITEM_ADB_LOG_CMD)
        if (logCmd.isNullOrEmpty()) {
            mLogCmdManager.logCmd = LogCmdManager.DEFAULT_LOGCAT
        } else {
            mLogCmdManager.logCmd = logCmd
        }

        val prefix = mConfigManager.getItem(ConfigManager.ITEM_ADB_PREFIX)
        if (prefix.isNullOrEmpty()) {
            mLogCmdManager.prefix = LogCmdManager.DEFAULT_PREFIX
        } else {
            mLogCmdManager.prefix = prefix
        }

        var prop = mConfigManager.getItem(ConfigManager.ITEM_FRAME_X)
        if (!prop.isNullOrEmpty()) {
            mFrameX = prop.toInt()
        }
        prop = mConfigManager.getItem(ConfigManager.ITEM_FRAME_Y)
        if (!prop.isNullOrEmpty()) {
            mFrameY = prop.toInt()
        }
        prop = mConfigManager.getItem(ConfigManager.ITEM_FRAME_WIDTH)
        if (!prop.isNullOrEmpty()) {
            mFrameWidth = prop.toInt()
        }
        prop = mConfigManager.getItem(ConfigManager.ITEM_FRAME_HEIGHT)
        if (!prop.isNullOrEmpty()) {
            mFrameHeight = prop.toInt()
        }
        prop = mConfigManager.getItem(ConfigManager.ITEM_FRAME_EXTENDED_STATE)
        if (!prop.isNullOrEmpty()) {
            mFrameExtendedState = prop.toInt()
        }
        prop = mConfigManager.getItem(ConfigManager.ITEM_ROTATION)
        if (!prop.isNullOrEmpty()) {
            mRotationStatus = prop.toInt()
        }

        prop = mConfigManager.getItem(ConfigManager.ITEM_LANG)
        if (!prop.isNullOrEmpty()) {
            Strings.lang = prop.toInt()
        }
        else {
            Strings.lang = Strings.EN
        }

        prop = mConfigManager.getItem(ConfigManager.ITEM_SHOW_LOG_STYLE)
        mShowLogComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT
        }

        prop = mConfigManager.getItem(ConfigManager.ITEM_BOLD_LOG_STYLE)
        mBoldLogComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT
        }

        prop = mConfigManager.getItem(ConfigManager.ITEM_SHOW_TAG_STYLE)
        mShowTagComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT
        }

        prop = mConfigManager.getItem(ConfigManager.ITEM_SHOW_PID_STYLE)
        mShowPidComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT
        }

        prop = mConfigManager.getItem(ConfigManager.ITEM_SHOW_TID_STYLE)
        mShowTidComboStyle = if (!prop.isNullOrEmpty()) {
            FilterComboBox.Mode.fromInt(prop.toInt())
        }
        else {
            FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT
        }

        createUI(title)

        if (mLogCmdManager.getType() == LogCmdManager.TYPE_LOGCAT) {
            mLogCmdManager.getDevices()
        }
    }

    private fun exit() {
        saveConfigOnDestroy()
        mFilteredTableModel.stopScan()
        mFullTableModel.stopScan()
        mLogCmdManager.stop()
        exitProcess(0)
    }

    private fun loadConfigOnCreate() {
        mConfigManager.loadConfig()
        mColorManager.mFullTableColor.getConfig()
        mColorManager.mFullTableColor.applyColor()
        mColorManager.mFilterTableColor.getConfig()
        mColorManager.mFilterTableColor.applyColor()
        mColorManager.getConfigFilterStyle()
        mConfigManager.saveConfig()
    }

    private fun saveConfigOnDestroy() {
        mConfigManager.loadConfig()

        try {
            mConfigManager.setItem(ConfigManager.ITEM_FRAME_X, location.x.toString())
        } catch (e: NullPointerException) {
            mConfigManager.setItem(ConfigManager.ITEM_FRAME_X, "0")
        }

        try {
            mConfigManager.setItem(ConfigManager.ITEM_FRAME_Y, location.y.toString())
        } catch (e: NullPointerException) {
            mConfigManager.setItem(ConfigManager.ITEM_FRAME_Y, "0")
        }

        try {
            mConfigManager.setItem(ConfigManager.ITEM_FRAME_WIDTH, size.width.toString())
        } catch (e: NullPointerException) {
            mConfigManager.setItem(ConfigManager.ITEM_FRAME_WIDTH, "1280")
        }

        try {
            mConfigManager.setItem(ConfigManager.ITEM_FRAME_HEIGHT, size.height.toString())
        } catch (e: NullPointerException) {
            mConfigManager.setItem(ConfigManager.ITEM_FRAME_HEIGHT, "720")
        }

        mConfigManager.setItem(ConfigManager.ITEM_FRAME_EXTENDED_STATE, extendedState.toString())

        var nCount = mShowLogCombo.itemCount
        if (nCount > ConfigManager.COUNT_SHOW_LOG) {
            nCount = ConfigManager.COUNT_SHOW_LOG
        }
        for (i in 0 until nCount) {
            mConfigManager.setItem(ConfigManager.ITEM_SHOW_LOG + i, mShowLogCombo.getItemAt(i).toString())
        }

        for (i in nCount until ConfigManager.COUNT_SHOW_LOG) {
            mConfigManager.removeConfigItem(ConfigManager.ITEM_SHOW_LOG + i)
        }

        nCount = mShowTagCombo.itemCount
        if (nCount > ConfigManager.COUNT_SHOW_TAG) {
            nCount = ConfigManager.COUNT_SHOW_TAG
        }
        for (i in 0 until nCount) {
            mConfigManager.setItem(ConfigManager.ITEM_SHOW_TAG + i, mShowTagCombo.getItemAt(i).toString())
        }
        for (i in nCount until ConfigManager.COUNT_SHOW_TAG) {
            mConfigManager.removeConfigItem(ConfigManager.ITEM_SHOW_TAG + i)
        }

        nCount = mBoldLogCombo.itemCount
        if (nCount > ConfigManager.COUNT_HIGHLIGHT_LOG) {
            nCount = ConfigManager.COUNT_HIGHLIGHT_LOG
        }
        for (i in 0 until nCount) {
            mConfigManager.setItem(ConfigManager.ITEM_HIGHLIGHT_LOG + i, mBoldLogCombo.getItemAt(i).toString())
        }
        for (i in nCount until ConfigManager.COUNT_HIGHLIGHT_LOG) {
            mConfigManager.removeConfigItem(ConfigManager.ITEM_HIGHLIGHT_LOG + i)
        }

        nCount = mSearchPanel.mSearchCombo.itemCount
        if (nCount > ConfigManager.COUNT_SEARCH_LOG) {
            nCount = ConfigManager.COUNT_SEARCH_LOG
        }
        for (i in 0 until nCount) {
            mConfigManager.setItem(ConfigManager.ITEM_SEARCH_LOG + i, mSearchPanel.mSearchCombo.getItemAt(i).toString())
        }
        for (i in nCount until ConfigManager.COUNT_SEARCH_LOG) {
            mConfigManager.removeConfigItem(ConfigManager.ITEM_SEARCH_LOG + i)
        }

        try {
            mConfigManager.setItem(ConfigManager.ITEM_ADB_DEVICE, mLogCmdManager.targetDevice)
        } catch (e: NullPointerException) {
            mConfigManager.setItem(ConfigManager.ITEM_ADB_DEVICE, "0.0.0.0")
        }

        try {
            mConfigManager.setItem(ConfigManager.ITEM_ADB_LOG_CMD, mLogCmdCombo.editor.item.toString())
        } catch (e: NullPointerException) {
            mConfigManager.setItem(ConfigManager.ITEM_ADB_LOG_CMD, LogCmdManager.DEFAULT_LOGCAT)
        }

        mConfigManager.setItem(ConfigManager.ITEM_DIVIDER_LOCATION, mLogSplitPane.dividerLocation.toString())
        if (mLogSplitPane.lastDividerLocation != -1) {
            mConfigManager.setItem(ConfigManager.ITEM_LAST_DIVIDER_LOCATION, mLogSplitPane.lastDividerLocation.toString())
        }

//            mProperties.put(ITEM_LANG, Strings.lang.toString())

        mConfigManager.saveConfig()
    }

    private fun createUI(title: String) {
        setTitle(title)

        val img = ImageIcon(this.javaClass.getResource("/images/logo.png"))
        iconImage = img.image

        defaultCloseOperation = EXIT_ON_CLOSE
        setLocation(mFrameX, mFrameY)
        setSize(mFrameWidth, mFrameHeight)
        extendedState = mFrameExtendedState
        addComponentListener(mComponentHandler)

        mMenuBar = JMenuBar()
        mMenuFile = JMenu(Strings.FILE)
        mMenuFile.mnemonic = KeyEvent.VK_F

        mItemFileOpen = JMenuItem(Strings.OPEN)
        mItemFileOpen.addActionListener(mActionHandler)
        mMenuFile.add(mItemFileOpen)

        mItemFileFollow = JMenuItem(Strings.FOLLOW)
        mItemFileFollow.addActionListener(mActionHandler)
        mMenuFile.add(mItemFileFollow)

        mItemFileOpenFiles = JMenuItem(Strings.OPEN_FILES)
        mItemFileOpenFiles.addActionListener(mActionHandler)
        mMenuFile.add(mItemFileOpenFiles)

        mItemFileAppendFiles = JMenuItem(Strings.APPEND_FILES)
        mItemFileAppendFiles.addActionListener(mActionHandler)
        mMenuFile.add(mItemFileAppendFiles)

//        mItemFileOpenRecents = JMenu(Strings.OPEN_RECENTS)
//        mItemFileOpenRecents.addActionListener(mActionHandler)
//        mMenuFile.add(mItemFileOpenRecents)
        mMenuFile.addSeparator()

        mItemFileExit = JMenuItem(Strings.EXIT)
        mItemFileExit.addActionListener(mActionHandler)
        mMenuFile.add(mItemFileExit)
        mMenuBar.add(mMenuFile)

        mMenuView = JMenu(Strings.VIEW)
        mMenuView.mnemonic = KeyEvent.VK_V

        mItemFull = JCheckBoxMenuItem(Strings.VIEW_FULL)
        mItemFull.addActionListener(mActionHandler)
        mMenuView.add(mItemFull)

        mMenuView.addSeparator()

        mItemSearch = JCheckBoxMenuItem(Strings.SEARCH)
        mItemSearch.addActionListener(mActionHandler)
        mMenuView.add(mItemSearch)

        mMenuView.addSeparator()

        mItemRotation = JMenuItem(Strings.ROTATION)
        mItemRotation.addActionListener(mActionHandler)
        mMenuView.add(mItemRotation)

        mMenuBar.add(mMenuView)

        mMenuSettings = JMenu(Strings.SETTING)
        mMenuSettings.mnemonic = KeyEvent.VK_S

        mItemLogCmd = JMenuItem("${Strings.LOG_CMD}(${Strings.ADB})")
        mItemLogCmd.addActionListener(mActionHandler)
        mMenuSettings.add(mItemLogCmd)
        mItemLogFile = JMenuItem(Strings.LOGFILE)
        mItemLogFile.addActionListener(mActionHandler)
        mMenuSettings.add(mItemLogFile)

        mMenuSettings.addSeparator()

        mItemFilterIncremental = JCheckBoxMenuItem(Strings.FILTER + "-" + Strings.INCREMENTAL)
        mItemFilterIncremental.addActionListener(mActionHandler)
        mMenuSettings.add(mItemFilterIncremental)

        mMenuSettings.addSeparator()

        mMenuLogLevel = JMenu(Strings.LOGLEVEL)
        mMenuLogLevel.addActionListener(mActionHandler)
        mMenuSettings.add(mMenuLogLevel)

        mLogLevelGroup = ButtonGroup()

        var menuItem = JRadioButtonMenuItem(VERBOSE)
        mLogLevelGroup.add(menuItem)
        mMenuLogLevel.add(menuItem)
        menuItem.isSelected = true
        menuItem.addItemListener(mLevelItemHandler)

        menuItem = JRadioButtonMenuItem(DEBUG)
        mLogLevelGroup.add(menuItem)
        mMenuLogLevel.add(menuItem)
        menuItem.addItemListener(mLevelItemHandler)

        menuItem = JRadioButtonMenuItem(INFO)
        mLogLevelGroup.add(menuItem)
        mMenuLogLevel.add(menuItem)
        menuItem.addItemListener(mLevelItemHandler)

        menuItem = JRadioButtonMenuItem(WARNING)
        mLogLevelGroup.add(menuItem)
        mMenuLogLevel.add(menuItem)
        menuItem.addItemListener(mLevelItemHandler)

        menuItem = JRadioButtonMenuItem(ERROR)
        mLogLevelGroup.add(menuItem)
        mMenuLogLevel.add(menuItem)
        menuItem.addItemListener(mLevelItemHandler)

        menuItem = JRadioButtonMenuItem(FATAL)
        mLogLevelGroup.add(menuItem)
        mMenuLogLevel.add(menuItem)
        menuItem.addItemListener(mLevelItemHandler)

        mMenuSettings.addSeparator()

        mItemAppearance = JMenuItem(Strings.APPEARANCE)
        mItemAppearance.addActionListener(mActionHandler)
        mMenuSettings.add(mItemAppearance)

        mMenuBar.add(mMenuSettings)

        mMenuHelp = JMenu(Strings.HELP)
        mMenuHelp.mnemonic = KeyEvent.VK_H

        mItemHelp = JMenuItem(Strings.HELP)
        mItemHelp.addActionListener(mActionHandler)
        mMenuHelp.add(mItemHelp)

        mMenuHelp.addSeparator()

        mItemAbout = JMenuItem(Strings.ABOUT)
        mItemAbout.addActionListener(mActionHandler)
        mMenuHelp.add(mItemAbout)
        mMenuBar.add(mMenuHelp)

        jMenuBar = mMenuBar

        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            UIManager.put("ScrollBar.thumb", ColorUIResource(Color(0xE0, 0xE0, 0xE0)))
            UIManager.put("ScrollBar.thumbHighlight", ColorUIResource(Color(0xE5, 0xE5, 0xE5)))
            UIManager.put("ScrollBar.thumbShadow", ColorUIResource(Color(0xE5, 0xE5, 0xE5)))
            UIManager.put("ComboBox.buttonDarkShadow", ColorUIResource(Color.black))
        }

        addMouseListener(mFrameMouseListener)
        addMouseMotionListener(mFrameMouseListener)
        contentPane.addMouseListener(mFrameMouseListener)

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exit()
            }
        })

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { p0 ->
            if (p0?.keyCode == KeyEvent.VK_PAGE_DOWN && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {
                mFilteredLogPanel.goToLast()
                mFullLogPanel.goToLast()
            } else if (p0?.keyCode == KeyEvent.VK_PAGE_UP && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {
                mFilteredLogPanel.goToFirst()
                mFullLogPanel.goToFirst()
//                } else if (p0?.keyCode == KeyEvent.VK_N && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {

            } else if (p0?.keyCode == KeyEvent.VK_L && (p0.modifiers and KeyEvent.CTRL_MASK) != 0) {
                mDeviceCombo.requestFocus()
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

        mFilterPanel = JPanel()
        mFilterLeftPanel = JPanel()

        mLogToolBar = ButtonPanel()
        mLogToolBar.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        mLogToolBar.addMouseListener(mMouseHandler)

        mSearchPanel = SearchPanel()

        val btnMargin = Insets(2, 5, 2, 5)
//        mLogToolBar = JPanel()
//        mLogToolBar.background = Color(0xE5, 0xE5, 0xE5)
        mStartBtn = ColorButton(Strings.START)
        mStartBtn.margin = btnMargin
        mStartBtn.toolTipText = TooltipStrings.START_BTN
        mStartBtn.icon = ImageIcon(this.javaClass.getResource("/images/start.png"))
        mStartBtn.addActionListener(mActionHandler)
        mStartBtn.addMouseListener(mMouseHandler)
        mRetryAdbToggle = ColorToggleButton(Strings.RETRY_ADB)
        mRetryAdbToggle.toolTipText = TooltipStrings.RETRY_ADB_TOGGLE
        mRetryAdbToggle.margin = btnMargin
//        mRetryAdbToggle.margin = Insets(mRetryAdbToggle.margin.top, 0, mRetryAdbToggle.margin.bottom, 0)
        mRetryAdbToggle.addItemListener(mItemHandler)

        mPauseToggle = ColorToggleButton(Strings.PAUSE)
        mPauseToggle.toolTipText = TooltipStrings.PAUSE_BTN
        mPauseToggle.margin = btnMargin
//        mPauseToggle.margin = Insets(mPauseToggle.margin.top, 0, mPauseToggle.margin.bottom, 0)
        mPauseToggle.addItemListener(mItemHandler)


        mStopBtn = ColorButton(Strings.STOP)
        mStopBtn.margin = btnMargin
        mStopBtn.toolTipText = TooltipStrings.STOP_BTN
        mStopBtn.addActionListener(mActionHandler)
        mStopBtn.addMouseListener(mMouseHandler)
        mClearViewsBtn = ColorButton(Strings.CLEAR_VIEWS)
        mClearViewsBtn.margin = btnMargin
        mClearViewsBtn.toolTipText = TooltipStrings.CLEAR_BTN
        mClearViewsBtn.icon = ImageIcon(this.javaClass.getResource("/images/clear.png"))

        mClearViewsBtn.addActionListener(mActionHandler)
        mClearViewsBtn.addMouseListener(mMouseHandler)
        mSaveBtn = ColorButton(Strings.SAVE)
        mSaveBtn.margin = btnMargin
        mSaveBtn.toolTipText = TooltipStrings.SAVE_BTN
        mSaveBtn.addActionListener(mActionHandler)
        mSaveBtn.addMouseListener(mMouseHandler)
//        mRotationBtn = ColorButton(Strings.ROTATION)
//        mRotationBtn.margin = btnMargin
//        mRotationBtn.toolTipText = TooltipStrings.ROTATION_BTN
//        mRotationBtn.addActionListener(mActionHandler)
//        mRotationBtn.addMouseListener(mMouseHandler)
//        mFiltersBtn = ColorButton(Strings.FILTERS)
//        mFiltersBtn.margin = btnMargin
//        mFiltersBtn.toolTipText = TooltipStrings.FILTER_LIST_BTN
//        mFiltersBtn.addActionListener(mActionHandler)
//        mFiltersBtn.addMouseListener(mMouseHandler)
//        mCmdsBtn = ColorButton(Strings.CMDS)
//        mCmdsBtn.margin = btnMargin
//        mCmdsBtn.toolTipText = TooltipStrings.CMD_LIST_BTN
//        mCmdsBtn.addActionListener(mActionHandler)
//        mCmdsBtn.addMouseListener(mMouseHandler)

        mLogPanel = JPanel()
        mShowLogPanel = JPanel()
        mShowLogCombo = FilterComboBox(mShowLogComboStyle, true)
        mShowLogCombo.toolTipText = TooltipStrings.LOG_COMBO
        mShowLogCombo.isEditable = true
        mShowLogCombo.renderer = FilterComboBox.ComboBoxRenderer()
        mShowLogCombo.editor.editorComponent.addKeyListener(mKeyHandler)
        mShowLogCombo.addItemListener(mItemHandler)
        mShowLogCombo.addPopupMenuListener(mPopupMenuHandler)
        mShowLogCombo.editor.editorComponent.addMouseListener(mMouseHandler)
        mShowLogToggle = ColorToggleButton(Strings.LOG)
        mShowLogToggle.toolTipText = TooltipStrings.LOG_TOGGLE
        mShowLogToggle.margin = Insets(0, 0, 0, 0)
        mShowLogTogglePanel = JPanel(GridLayout(1, 1))
        mShowLogTogglePanel.add(mShowLogToggle)
        mShowLogTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        mShowLogToggle.addItemListener(mItemHandler)

        mBoldLogPanel = JPanel()
        mBoldLogCombo = FilterComboBox(mBoldLogComboStyle, false)
        mBoldLogCombo.toolTipText = TooltipStrings.BOLD_COMBO
        mBoldLogCombo.mEnabledTfTooltip = false
        mBoldLogCombo.isEditable = true
        mBoldLogCombo.renderer = FilterComboBox.ComboBoxRenderer()
        mBoldLogCombo.editor.editorComponent.addKeyListener(mKeyHandler)
        mBoldLogCombo.addItemListener(mItemHandler)
        mBoldLogCombo.editor.editorComponent.addMouseListener(mMouseHandler)
        mBoldLogToggle = ColorToggleButton(Strings.BOLD)
        mBoldLogToggle.toolTipText = TooltipStrings.BOLD_TOGGLE
        mBoldLogToggle.margin = Insets(0, 0, 0, 0)
        mBoldLogTogglePanel = JPanel(GridLayout(1, 1))
        mBoldLogTogglePanel.add(mBoldLogToggle)
        mBoldLogTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        mBoldLogToggle.addItemListener(mItemHandler)

        mShowTagPanel = JPanel()
        mShowTagCombo = FilterComboBox(mShowTagComboStyle, false)
        mShowTagCombo.toolTipText = TooltipStrings.TAG_COMBO
        mShowTagCombo.isEditable = true
        mShowTagCombo.renderer = FilterComboBox.ComboBoxRenderer()
        mShowTagCombo.editor.editorComponent.addKeyListener(mKeyHandler)
        mShowTagCombo.addItemListener(mItemHandler)
        mShowTagCombo.editor.editorComponent.addMouseListener(mMouseHandler)
        mShowTagToggle = ColorToggleButton(Strings.TAG)
        mShowTagToggle.toolTipText = TooltipStrings.TAG_TOGGLE
        mShowTagToggle.margin = Insets(0, 0, 0, 0)
        mShowTagTogglePanel = JPanel(GridLayout(1, 1))
        mShowTagTogglePanel.add(mShowTagToggle)
        mShowTagTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        mShowTagToggle.addItemListener(mItemHandler)

        mShowPidPanel = JPanel()
        mShowPidCombo = FilterComboBox(mShowPidComboStyle, false)
        mShowPidCombo.toolTipText = TooltipStrings.PID_COMBO
        mShowPidCombo.isEditable = true
        mShowPidCombo.renderer = FilterComboBox.ComboBoxRenderer()
        mShowPidCombo.editor.editorComponent.addKeyListener(mKeyHandler)
        mShowPidCombo.addItemListener(mItemHandler)
        mShowPidCombo.editor.editorComponent.addMouseListener(mMouseHandler)
        mShowPidToggle = ColorToggleButton(Strings.PID)
        mShowPidToggle.toolTipText = TooltipStrings.PID_TOGGLE
        mShowPidToggle.margin = Insets(0, 0, 0, 0)
        mShowPidTogglePanel = JPanel(GridLayout(1, 1))
        mShowPidTogglePanel.add(mShowPidToggle)
        mShowPidTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        mShowPidToggle.addItemListener(mItemHandler)

        mShowTidPanel = JPanel()
        mShowTidCombo = FilterComboBox(mShowTidComboStyle, false)
        mShowTidCombo.toolTipText = TooltipStrings.TID_COMBO
        mShowTidCombo.isEditable = true
        mShowTidCombo.renderer = FilterComboBox.ComboBoxRenderer()
        mShowTidCombo.editor.editorComponent.addKeyListener(mKeyHandler)
        mShowTidCombo.addItemListener(mItemHandler)
        mShowTidCombo.editor.editorComponent.addMouseListener(mMouseHandler)
        mShowTidToggle = ColorToggleButton(Strings.TID)
        mShowTidToggle.toolTipText = TooltipStrings.TID_TOGGLE
        mShowTidToggle.margin = Insets(0, 0, 0, 0)
        mShowTidTogglePanel = JPanel(GridLayout(1, 1))
        mShowTidTogglePanel.add(mShowTidToggle)
        mShowTidTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)
        mShowTidToggle.addItemListener(mItemHandler)

        mLogCmdCombo = ColorComboBox()
        mLogCmdCombo.toolTipText = TooltipStrings.LOG_CMD_COMBO
        mLogCmdCombo.isEditable = true
        mLogCmdCombo.renderer = ColorComboBox.ComboBoxRenderer()
        mLogCmdCombo.editor.editorComponent.addKeyListener(mKeyHandler)
        mLogCmdCombo.addItemListener(mItemHandler)
        mLogCmdCombo.editor.editorComponent.addMouseListener(mMouseHandler)
        mLogCmdCombo.addPopupMenuListener(mPopupMenuHandler)
        
        mDeviceStatus = JLabel("None", JLabel.LEFT)
        mDeviceStatus.isEnabled = false
        val deviceComboPanel = JPanel(BorderLayout())
        mDeviceCombo = ColorComboBox()
        mDeviceCombo.toolTipText = TooltipStrings.DEVICES_COMBO
        mDeviceCombo.isEditable = true
        mDeviceCombo.renderer = ColorComboBox.ComboBoxRenderer()
        mDeviceCombo.editor.editorComponent.addKeyListener(mKeyHandler)
        mDeviceCombo.addItemListener(mItemHandler)
        mDeviceCombo.editor.editorComponent.addMouseListener(mMouseHandler)
        deviceComboPanel.add(mDeviceCombo, BorderLayout.CENTER)
        mAdbConnectBtn = ColorButton(Strings.CONNECT)
        mAdbConnectBtn.margin = btnMargin
        mAdbConnectBtn.toolTipText = TooltipStrings.CONNECT_BTN
        mAdbConnectBtn.addActionListener(mActionHandler)
//        deviceComboPanel.add(mAdbConnectBtn, BorderLayout.EAST)
        mAdbRefreshBtn = ColorButton(Strings.REFRESH)
        mAdbRefreshBtn.margin = btnMargin
        mAdbRefreshBtn.addActionListener(mActionHandler)
        mAdbRefreshBtn.toolTipText = TooltipStrings.REFRESH_BTN
        mAdbDisconnectBtn = ColorButton(Strings.DISCONNECT)
        mAdbDisconnectBtn.margin = btnMargin
        mAdbDisconnectBtn.addActionListener(mActionHandler)
        mAdbDisconnectBtn.toolTipText = TooltipStrings.DISCONNECT_BTN

        mMatchCaseToggle = ColorToggleButton("Aa")
        mMatchCaseToggle.toolTipText = TooltipStrings.CASE_TOGGLE
        mMatchCaseToggle.margin = Insets(0, 0, 0, 0)
        mMatchCaseToggle.addItemListener(mItemHandler)
        mMatchCaseTogglePanel = JPanel(GridLayout(1, 1))
        mMatchCaseTogglePanel.add(mMatchCaseToggle)
        mMatchCaseTogglePanel.border = BorderFactory.createEmptyBorder(3,3,3,3)

        mShowLogPanel.layout = BorderLayout()
        mShowLogPanel.add(mShowLogTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            mShowLogCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        mShowLogPanel.add(mShowLogCombo, BorderLayout.CENTER)

        mBoldLogPanel.layout = BorderLayout()
        mBoldLogPanel.add(mBoldLogTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            mBoldLogCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        mBoldLogCombo.preferredSize = Dimension(170, mBoldLogCombo.preferredSize.height)
        mBoldLogPanel.add(mBoldLogCombo, BorderLayout.CENTER)
//        mBoldPanel.add(mBoldLogPanel)

        mShowTagPanel.layout = BorderLayout()
        mShowTagPanel.add(mShowTagTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            mShowTagCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        mShowTagCombo.preferredSize = Dimension(250, mShowTagCombo.preferredSize.height)
        mShowTagPanel.add(mShowTagCombo, BorderLayout.CENTER)
//        mTagPanel.add(mShowTagPanel)

        mShowPidPanel.layout = BorderLayout()
        mShowPidPanel.add(mShowPidTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            mShowPidCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        mShowPidCombo.preferredSize = Dimension(120, mShowPidCombo.preferredSize.height)
        mShowPidPanel.add(mShowPidCombo, BorderLayout.CENTER)
//        mPidPanel.add(mShowPidPanel)

        mShowTidPanel.layout = BorderLayout()
        mShowTidPanel.add(mShowTidTogglePanel, BorderLayout.WEST)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            mShowTidCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 3)
        }
        mShowTidCombo.preferredSize = Dimension(120, mShowTidCombo.preferredSize.height)
        mShowTidPanel.add(mShowTidCombo, BorderLayout.CENTER)
//        mTidPanel.add(mShowTidPanel)

        mLogCmdCombo.preferredSize = Dimension(200, mLogCmdCombo.preferredSize.height)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            mLogCmdCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 5)
        }

        mDeviceCombo.preferredSize = Dimension(200, mDeviceCombo.preferredSize.height)
        if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
            mDeviceCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 5)
        }
        mDeviceStatus.preferredSize = Dimension(100, 30)
        mDeviceStatus.border = BorderFactory.createEmptyBorder(3, 0, 3, 0)
        mDeviceStatus.horizontalAlignment = JLabel.CENTER

        mScrollbackApplyBtn = ColorButton(Strings.APPLY)
        mScrollbackApplyBtn.margin = btnMargin
        mScrollbackApplyBtn.toolTipText = TooltipStrings.SCROLLBACK_APPLY_BTN
        mScrollbackApplyBtn.addActionListener(mActionHandler)
        mScrollbackKeepToggle = ColorToggleButton(Strings.KEEP)
        mScrollbackKeepToggle.toolTipText = TooltipStrings.SCROLLBACK_KEEP_TOGGLE
        mScrollbackKeepToggle.mSelectedBg = Color.RED
        mScrollbackKeepToggle.mSelectedFg = Color.BLACK
        if (ConfigManager.LaF != CROSS_PLATFORM_LAF) {
            val imgIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on_warn.png"))
            mScrollbackKeepToggle.selectedIcon = imgIcon
        }

        mScrollbackKeepToggle.margin = btnMargin
        mScrollbackKeepToggle.addItemListener(mItemHandler)

        mScrollbackLabel = JLabel(Strings.SCROLLBACK_LINES)

        mScrollbackTF = JTextField()
        mScrollbackTF.toolTipText = TooltipStrings.SCROLLBACK_TF
        mScrollbackTF.preferredSize = Dimension(80, mScrollbackTF.preferredSize.height)
        mScrollbackTF.addKeyListener(mKeyHandler)
        mScrollbackSplitFileToggle = ColorToggleButton(Strings.SPLIT_FILE)
        mScrollbackSplitFileToggle.toolTipText = TooltipStrings.SCROLLBACK_SPLIT_CHK
        mScrollbackSplitFileToggle.margin = btnMargin
        mScrollbackSplitFileToggle.addItemListener(mItemHandler)

        val itemFilterPanel = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0))
        itemFilterPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        itemFilterPanel.add(mShowTagPanel)
        itemFilterPanel.add(mShowPidPanel)
        itemFilterPanel.add(mShowTidPanel)
        itemFilterPanel.add(mBoldLogPanel)
        itemFilterPanel.add(mMatchCaseTogglePanel)

        mLogPanel.layout = BorderLayout()
        mLogPanel.add(mShowLogPanel, BorderLayout.CENTER)
        mLogPanel.add(itemFilterPanel, BorderLayout.EAST)

        mFilterLeftPanel.layout = BorderLayout()
        mFilterLeftPanel.add(mLogPanel, BorderLayout.NORTH)
        mFilterLeftPanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)

        mFilterPanel.layout = BorderLayout()
        mFilterPanel.add(mFilterLeftPanel, BorderLayout.CENTER)
        mFilterPanel.addMouseListener(mMouseHandler)

        mLogToolBar.add(mStartBtn)
        mLogToolBar.add(mRetryAdbToggle)
        addVSeparator2(mLogToolBar)
        mLogToolBar.add(mPauseToggle)
        mLogToolBar.add(mStopBtn)
        mLogToolBar.add(mSaveBtn)

        addVSeparator(mLogToolBar)

        mLogToolBar.add(mLogCmdCombo)

        addVSeparator(mLogToolBar)

        mLogToolBar.add(deviceComboPanel)
        mLogToolBar.add(mAdbConnectBtn)
        mLogToolBar.add(mAdbDisconnectBtn)
        mLogToolBar.add(mAdbRefreshBtn)

        addVSeparator(mLogToolBar)

        mLogToolBar.add(mClearViewsBtn)


        val scrollbackPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
        scrollbackPanel.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        addVSeparator(scrollbackPanel)
        scrollbackPanel.add(mScrollbackLabel)
        scrollbackPanel.add(mScrollbackTF)
        scrollbackPanel.add(mScrollbackSplitFileToggle)
        scrollbackPanel.add(mScrollbackApplyBtn)
        scrollbackPanel.add(mScrollbackKeepToggle)

        val toolBarPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        toolBarPanel.layout = BorderLayout()
        toolBarPanel.addMouseListener(mMouseHandler)
        toolBarPanel.add(mLogToolBar, BorderLayout.CENTER)
        toolBarPanel.add(scrollbackPanel, BorderLayout.EAST)

        mFilterPanel.add(toolBarPanel, BorderLayout.NORTH)
        mFilterPanel.add(mSearchPanel, BorderLayout.SOUTH)

        mSearchPanel.isVisible = false
        mItemSearch.state = mSearchPanel.isVisible

        layout = BorderLayout()

        mFullTableModel = LogTableModel(this, null)
        mFilteredTableModel = LogTableModel(this, mFullTableModel)

        mFullLogPanel = LogPanel(this, mFullTableModel, null, FocusHandler(false))
        mFilteredLogPanel = LogPanel(this, mFilteredTableModel, mFullLogPanel, FocusHandler(true))
        mFullLogPanel.updateTableBar(mConfigManager.loadCmds())
        mFilteredLogPanel.updateTableBar(mConfigManager.loadFilters())

        mFiltersManager = FiltersManager(this, mFilteredLogPanel)
        mCmdsManager = CmdsManager(this, mFullLogPanel)

        when (mRotationStatus) {
            ROTATION_LEFT_RIGHT -> {
                mLogSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, mFilteredLogPanel, mFullLogPanel)
                mLogSplitPane.resizeWeight = SPLIT_WEIGHT
            }
            ROTATION_RIGHT_LEFT -> {
                mLogSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, mFullLogPanel, mFilteredLogPanel)
                mLogSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
            }
            ROTATION_TOP_BOTTOM -> {
                mLogSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, false, mFilteredLogPanel, mFullLogPanel)
                mLogSplitPane.resizeWeight = SPLIT_WEIGHT
            }
            ROTATION_BOTTOM_TOP -> {
                mLogSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, false, mFullLogPanel, mFilteredLogPanel)
                mLogSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
            }
        }

        val dividerSize = mConfigManager.getItem(ConfigManager.ITEM_APPEARANCE_DIVIDER_SIZE)
        if (!dividerSize.isNullOrEmpty()) {
            mLogSplitPane.dividerSize = dividerSize.toInt()
        }

        mLogSplitPane.isOneTouchExpandable = false

        mStatusBar = JPanel(BorderLayout())
        mStatusBar.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        mStatusMethod = JLabel("")
        mStatusMethod.isOpaque = true
        mStatusMethod.background = Color.DARK_GRAY
        mStatusMethod.addPropertyChangeListener(mStatusChangeListener)
        mStatusTF = StatusTextField(Strings.NONE)
        mStatusTF.document.addDocumentListener(mStatusChangeListener)
        mStatusTF.toolTipText = TooltipStrings.SAVED_FILE_TF
        mStatusTF.isEditable = false
        mStatusTF.border = BorderFactory.createEmptyBorder()

        mStartFollowBtn = ColorButton(Strings.START)
        mStartFollowBtn.margin = btnMargin
        mStartFollowBtn.toolTipText = TooltipStrings.START_FOLLOW_BTN
        mStartFollowBtn.addActionListener(mActionHandler)
        mStartFollowBtn.addMouseListener(mMouseHandler)

        mPauseFollowToggle = ColorToggleButton(Strings.PAUSE)
        mPauseFollowToggle.margin = Insets(mPauseFollowToggle.margin.top, 0, mPauseFollowToggle.margin.bottom, 0)
        mPauseFollowToggle.addItemListener(mItemHandler)

        mStopFollowBtn = ColorButton(Strings.STOP)
        mStopFollowBtn.margin = btnMargin
        mStopFollowBtn.toolTipText = TooltipStrings.STOP_FOLLOW_BTN
        mStopFollowBtn.addActionListener(mActionHandler)
        mStopFollowBtn.addMouseListener(mMouseHandler)

        val followPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
        followPanel.border = BorderFactory.createEmptyBorder(0, 3, 0, 3)
        mFollowLabel = JLabel(" ${Strings.FOLLOW} ")
        mFollowLabel.border = BorderFactory.createDashedBorder(null, 1.0f, 2.0f)
        followPanel.add(mFollowLabel)
        followPanel.add(mStartFollowBtn)
        followPanel.add(mPauseFollowToggle)
        followPanel.add(mStopFollowBtn)

        enabledFollowBtn(false)

        mStatusBar.add(mStatusMethod, BorderLayout.WEST)
        mStatusBar.add(mStatusTF, BorderLayout.CENTER)
        mStatusBar.add(followPanel, BorderLayout.EAST)

        val logLevel = mConfigManager.getItem(ConfigManager.ITEM_LOG_LEVEL)
        if (logLevel != null) {
            for (item in mLogLevelGroup.elements) {
                if (logLevel == item.text) {
                    item.isSelected = true
                    break
                }
            }
        }

        var item: String?
        for (i in 0 until ConfigManager.COUNT_SHOW_LOG) {
            item = mConfigManager.getItem(ConfigManager.ITEM_SHOW_LOG + i)
            if (item == null) {
                break
            }

            if (!mShowLogCombo.isExistItem(item)) {
                mShowLogCombo.addItem(item)
            }
        }

        mShowLogCombo.updateTooltip()

        if (mShowLogCombo.itemCount > 0) {
            mShowLogCombo.selectedIndex = 0
        }

        var check = mConfigManager.getItem(ConfigManager.ITEM_SHOW_LOG_CHECK)
        if (!check.isNullOrEmpty()) {
            mShowLogToggle.isSelected = check.toBoolean()
        } else {
            mShowLogToggle.isSelected = true
        }
        mShowLogCombo.isEnabled = mShowLogToggle.isSelected

        for (i in 0 until ConfigManager.COUNT_SHOW_TAG) {
            item = mConfigManager.getItem(ConfigManager.ITEM_SHOW_TAG + i)
            if (item == null) {
                break
            }
            mShowTagCombo.insertItemAt(item, i)
            if (i == 0) {
                mShowTagCombo.selectedIndex = 0
            }
        }

        mShowTagCombo.updateTooltip()

        check = mConfigManager.getItem(ConfigManager.ITEM_SHOW_TAG_CHECK)
        if (!check.isNullOrEmpty()) {
            mShowTagToggle.isSelected = check.toBoolean()
        } else {
            mShowTagToggle.isSelected = true
        }
        mShowTagCombo.setEnabledFilter(mShowTagToggle.isSelected)

        check = mConfigManager.getItem(ConfigManager.ITEM_SHOW_PID_CHECK)
        if (!check.isNullOrEmpty()) {
            mShowPidToggle.isSelected = check.toBoolean()
        } else {
            mShowPidToggle.isSelected = true
        }
        mShowPidCombo.setEnabledFilter(mShowPidToggle.isSelected)

        check = mConfigManager.getItem(ConfigManager.ITEM_SHOW_TID_CHECK)
        if (!check.isNullOrEmpty()) {
            mShowTidToggle.isSelected = check.toBoolean()
        } else {
            mShowTidToggle.isSelected = true
        }
        mShowTidCombo.setEnabledFilter(mShowTidToggle.isSelected)

        for (i in 0 until ConfigManager.COUNT_HIGHLIGHT_LOG) {
            item = mConfigManager.getItem(ConfigManager.ITEM_HIGHLIGHT_LOG + i)
            if (item == null) {
                break
            }
            mBoldLogCombo.insertItemAt(item, i)
            if (i == 0) {
                mBoldLogCombo.selectedIndex = 0
            }
        }

        mBoldLogCombo.updateTooltip()

        check = mConfigManager.getItem(ConfigManager.ITEM_HIGHLIGHT_LOG_CHECK)
        if (!check.isNullOrEmpty()) {
            mBoldLogToggle.isSelected = check.toBoolean()
        } else {
            mBoldLogToggle.isSelected = true
        }
        mBoldLogCombo.setEnabledFilter(mBoldLogToggle.isSelected)

        for (i in 0 until ConfigManager.COUNT_SEARCH_LOG) {
            item = mConfigManager.getItem(ConfigManager.ITEM_SEARCH_LOG + i)
            if (item == null) {
                break
            }
            mSearchPanel.mSearchCombo.insertItemAt(item, i)
            if (i == 0) {
                mSearchPanel.mSearchCombo.selectedIndex = 0
            }
        }

        mSearchPanel.mSearchCombo.updateTooltip()

        updateLogCmdCombo(true)

        val targetDevice = mConfigManager.getItem(ConfigManager.ITEM_ADB_DEVICE)
        mDeviceCombo.insertItemAt(targetDevice, 0)
        mDeviceCombo.selectedIndex = 0

        if (mLogCmdManager.devices.contains(targetDevice)) {
            mDeviceStatus.text = Strings.CONNECTED
            setDeviceComboColor(true)
        } else {
            mDeviceStatus.text = Strings.NOT_CONNECTED
            setDeviceComboColor(false)
        }

        var fontName = mConfigManager.getItem(ConfigManager.ITEM_FONT_NAME)
        if (fontName.isNullOrEmpty()) {
            fontName = DEFAULT_FONT_NAME
        }

        var fontSize = 12
        check = mConfigManager.getItem(ConfigManager.ITEM_FONT_SIZE)
        if (!check.isNullOrEmpty()) {
            fontSize = check.toInt()
        }

        mFont = Font(fontName, Font.PLAIN, fontSize)
        mFilteredLogPanel.mFont = mFont
        mFullLogPanel.mFont = mFont

        var divider = mConfigManager.getItem(ConfigManager.ITEM_LAST_DIVIDER_LOCATION)
        if (!divider.isNullOrEmpty()) {
            mLogSplitPane.lastDividerLocation = divider.toInt()
        }

        divider = mConfigManager.getItem(ConfigManager.ITEM_DIVIDER_LOCATION)
        if (!divider.isNullOrEmpty() && mLogSplitPane.lastDividerLocation != -1) {
            mLogSplitPane.dividerLocation = divider.toInt()
        }

        when (logLevel) {
            VERBOSE ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_VERBOSE
            DEBUG ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_DEBUG
            INFO ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_INFO
            WARNING ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_WARNING
            ERROR ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_ERROR
            FATAL ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_FATAL
        }

        if (mShowLogToggle.isSelected && mShowLogCombo.selectedItem != null) {
            mFilteredTableModel.mFilterLog = mShowLogCombo.selectedItem!!.toString()
        } else {
            mFilteredTableModel.mFilterLog = ""
        }
        if (mBoldLogToggle.isSelected && mBoldLogCombo.selectedItem != null) {
            mFilteredTableModel.mFilterHighlightLog = mBoldLogCombo.selectedItem!!.toString()
        } else {
            mFilteredTableModel.mFilterHighlightLog = ""
        }
        if (mSearchPanel.isVisible && mSearchPanel.mSearchCombo.selectedItem != null) {
            mFilteredTableModel.mFilterSearchLog = mSearchPanel.mSearchCombo.selectedItem!!.toString()
        } else {
            mFilteredTableModel.mFilterSearchLog = ""
        }
        if (mShowTagToggle.isSelected && mShowTagCombo.selectedItem != null) {
            mFilteredTableModel.mFilterTag = mShowTagCombo.selectedItem!!.toString()
        } else {
            mFilteredTableModel.mFilterTag = ""
        }
        if (mShowPidToggle.isSelected && mShowPidCombo.selectedItem != null) {
            mFilteredTableModel.mFilterPid = mShowPidCombo.selectedItem!!.toString()
        } else {
            mFilteredTableModel.mFilterPid = ""
        }
        if (mShowTidToggle.isSelected && mShowTidCombo.selectedItem != null) {
            mFilteredTableModel.mFilterTid = mShowTidCombo.selectedItem!!.toString()
        } else {
            mFilteredTableModel.mFilterTid = ""
        }

        check = mConfigManager.getItem(ConfigManager.ITEM_VIEW_FULL)
        if (!check.isNullOrEmpty()) {
            mItemFull.state = check.toBoolean()
        } else {
            mItemFull.state = true
        }
        if (!mItemFull.state) {
            windowedModeLogPanel(mFullLogPanel)
        }

        check = mConfigManager.getItem(ConfigManager.ITEM_FILTER_INCREMENTAL)
        if (!check.isNullOrEmpty()) {
            mItemFilterIncremental.state = check.toBoolean()
        } else {
            mItemFilterIncremental.state = false
        }

        check = mConfigManager.getItem(ConfigManager.ITEM_SCROLLBACK)
        if (!check.isNullOrEmpty()) {
            mScrollbackTF.text = check
        } else {
            mScrollbackTF.text = "0"
        }
        mFilteredTableModel.mScrollback = mScrollbackTF.text.toInt()

        check = mConfigManager.getItem(ConfigManager.ITEM_SCROLLBACK_SPLIT_FILE)
        if (!check.isNullOrEmpty()) {
            mScrollbackSplitFileToggle.isSelected = check.toBoolean()
        } else {
            mScrollbackSplitFileToggle.isSelected = false
        }
        mFilteredTableModel.mScrollbackSplitFile = mScrollbackSplitFileToggle.isSelected

        check = mConfigManager.getItem(ConfigManager.ITEM_MATCH_CASE)
        if (!check.isNullOrEmpty()) {
            mMatchCaseToggle.isSelected = check.toBoolean()
        } else {
            mMatchCaseToggle.isSelected = false
        }
        mFilteredTableModel.mMatchCase = mMatchCaseToggle.isSelected

        check = mConfigManager.getItem(ConfigManager.ITEM_SEARCH_MATCH_CASE)
        if (!check.isNullOrEmpty()) {
            mSearchPanel.mSearchMatchCaseToggle.isSelected = check.toBoolean()
        } else {
            mSearchPanel.mSearchMatchCaseToggle.isSelected = false
        }
        mFilteredTableModel.mSearchMatchCase = mSearchPanel.mSearchMatchCaseToggle.isSelected

        check = mConfigManager.getItem(ConfigManager.ITEM_RETRY_ADB)
        if (!check.isNullOrEmpty()) {
            mRetryAdbToggle.isSelected = check.toBoolean()
        } else {
            mRetryAdbToggle.isSelected = false
        }

        check = mConfigManager.getItem(ConfigManager.ITEM_ICON_TEXT)
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

        add(mFilterPanel, BorderLayout.NORTH)
        add(mLogSplitPane, BorderLayout.CENTER)
        add(mStatusBar, BorderLayout.SOUTH)

        registerSearchStroke()

        IsCreatingUI = false
    }

    private fun setBtnIcons(isShow:Boolean) {
        if (isShow) {
            mStartBtn.icon = ImageIcon(this.javaClass.getResource("/images/start.png"))
            mStopBtn.icon = ImageIcon(this.javaClass.getResource("/images/stop.png"))
            mClearViewsBtn.icon = ImageIcon(this.javaClass.getResource("/images/clear.png"))
            mSaveBtn.icon = ImageIcon(this.javaClass.getResource("/images/save.png"))
            mAdbConnectBtn.icon = ImageIcon(this.javaClass.getResource("/images/connect.png"))
            mAdbRefreshBtn.icon = ImageIcon(this.javaClass.getResource("/images/refresh.png"))
            mAdbDisconnectBtn.icon = ImageIcon(this.javaClass.getResource("/images/disconnect.png"))
            mScrollbackApplyBtn.icon = ImageIcon(this.javaClass.getResource("/images/apply.png"))

            mRetryAdbToggle.icon = ImageIcon(this.javaClass.getResource("/images/retry_off.png"))
            mPauseToggle.icon = ImageIcon(this.javaClass.getResource("/images/pause_off.png"))
            mScrollbackKeepToggle.icon = ImageIcon(this.javaClass.getResource("/images/keeplog_off.png"))
            mScrollbackSplitFileToggle.icon = ImageIcon(this.javaClass.getResource("/images/splitfile_off.png"))

            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                mRetryAdbToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/retry_on_dark.png"))
                mPauseToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/pause_on_dark.png"))
                mScrollbackKeepToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/keeplog_on_dark.png"))
                mScrollbackSplitFileToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/splitfile_on_dark.png"))
            }
            else {
                mRetryAdbToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/retry_on.png"))
                mPauseToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/pause_on.png"))
                mScrollbackKeepToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/keeplog_on.png"))
                mScrollbackSplitFileToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/splitfile_on.png"))
            }

            mScrollbackLabel.icon = ImageIcon(this.javaClass.getResource("/images/scrollback.png"))
        }
        else {
            mStartBtn.icon = null
            mStopBtn.icon = null
            mClearViewsBtn.icon = null
            mSaveBtn.icon = null
            mAdbConnectBtn.icon = null
            mAdbRefreshBtn.icon = null
            mAdbDisconnectBtn.icon = null
            mScrollbackApplyBtn.icon = null

            mRetryAdbToggle.icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
            mRetryAdbToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on.png"))
            mPauseToggle.icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
            mPauseToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on.png"))
            mScrollbackKeepToggle.icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
            mScrollbackKeepToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on_warn.png"))
            mScrollbackSplitFileToggle.icon = ImageIcon(this.javaClass.getResource("/images/toggle_off.png"))
            mScrollbackSplitFileToggle.selectedIcon = ImageIcon(this.javaClass.getResource("/images/toggle_on.png"))

            mScrollbackLabel.icon = null
        }
    }

    private fun setBtnTexts(isShow:Boolean) {
        if (isShow) {
            mStartBtn.text = Strings.START
            mRetryAdbToggle.text = Strings.RETRY_ADB
            mPauseToggle.text = Strings.PAUSE
            mStopBtn.text = Strings.STOP
            mClearViewsBtn.text = Strings.CLEAR_VIEWS
            mSaveBtn.text = Strings.SAVE
            mAdbConnectBtn.text = Strings.CONNECT
            mAdbRefreshBtn.text = Strings.REFRESH
            mAdbDisconnectBtn.text = Strings.DISCONNECT
            mScrollbackApplyBtn.text = Strings.APPLY
            mScrollbackKeepToggle.text = Strings.KEEP
            mScrollbackSplitFileToggle.text = Strings.SPLIT_FILE
            mScrollbackLabel.text = Strings.SCROLLBACK_LINES
        }
        else {
            mStartBtn.text = null
            mStopBtn.text = null
            mClearViewsBtn.text = null
            mSaveBtn.text = null
            mAdbConnectBtn.text = null
            mAdbRefreshBtn.text = null
            mAdbDisconnectBtn.text = null
            mScrollbackApplyBtn.text = null
            mRetryAdbToggle.text = null
            mPauseToggle.text = null
            mScrollbackKeepToggle.text = null
            mScrollbackSplitFileToggle.text = null
            mScrollbackLabel.text = null
        }
    }

    inner class StatusChangeListener : PropertyChangeListener, DocumentListener {
        private var mMethod = ""
        override fun propertyChange(evt: PropertyChangeEvent?) {
            if (evt?.source == mStatusMethod && evt.propertyName == "text") {
                mMethod = evt.newValue.toString().trim()
            }
        }

        override fun insertUpdate(evt: DocumentEvent?) {
            updateTitleBar(mMethod)
        }

        override fun removeUpdate(e: DocumentEvent?) {
        }

        override fun changedUpdate(evt: DocumentEvent?) {
        }
    }

    private fun updateTitleBar(statusMethod: String) {
        title = when (statusMethod) {
            Strings.OPEN, Strings.FOLLOW, "${Strings.FOLLOW} ${Strings.STOP}" -> {
                val path: Path = Paths.get(mStatusTF.text)
                path.fileName.toString()
            }
            Strings.ADB, Strings.CMD, "${Strings.ADB} ${Strings.STOP}", "${Strings.CMD} ${Strings.STOP}" -> {
                mLogCmdManager.targetDevice.ifEmpty { NAME }
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
        if (logPanel.parent == mLogSplitPane) {
            logPanel.mIsWindowedMode = true
            mItemRotation.isEnabled = false
            mLogSplitPane.remove(logPanel)
            if (mItemFull.state) {
                val logTableDialog = LogTableDialog(this@MainUI, logPanel)
                logTableDialog.isVisible = true
            }
        }
    }

    fun attachLogPanel(logPanel: LogPanel) {
        if (logPanel.parent != mLogSplitPane) {
            logPanel.mIsWindowedMode = false
            mItemRotation.isEnabled = true
            mLogSplitPane.remove(mFilteredLogPanel)
            mLogSplitPane.remove(mFullLogPanel)
            when (mRotationStatus) {
                ROTATION_LEFT_RIGHT -> {
                    mLogSplitPane.orientation = JSplitPane.HORIZONTAL_SPLIT
                    mLogSplitPane.add(mFilteredLogPanel)
                    mLogSplitPane.add(mFullLogPanel)
                    mLogSplitPane.resizeWeight = SPLIT_WEIGHT
                }
                ROTATION_RIGHT_LEFT -> {
                    mLogSplitPane.orientation = JSplitPane.HORIZONTAL_SPLIT
                    mLogSplitPane.add(mFullLogPanel)
                    mLogSplitPane.add(mFilteredLogPanel)
                    mLogSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
                }
                ROTATION_TOP_BOTTOM -> {
                    mLogSplitPane.orientation = JSplitPane.VERTICAL_SPLIT
                    mLogSplitPane.add(mFilteredLogPanel)
                    mLogSplitPane.add(mFullLogPanel)
                    mLogSplitPane.resizeWeight = SPLIT_WEIGHT
                }
                ROTATION_BOTTOM_TOP -> {
                    mLogSplitPane.orientation = JSplitPane.VERTICAL_SPLIT
                    mLogSplitPane.add(mFullLogPanel)
                    mLogSplitPane.add(mFilteredLogPanel)
                    mLogSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
                }
            }
        }
    }

    fun openFile(path: String, isAppend: Boolean) {
        println("Opening: $path, $isAppend")
        mStatusMethod.text = " ${Strings.OPEN} "
        mFilteredTableModel.stopScan()
        mFilteredTableModel.stopFollow()

        if (isAppend) {
            mStatusTF.text += "| $path"
        } else {
            mStatusTF.text = path
        }
        mFullTableModel.setLogFile(path)
        mFilteredTableModel.setLogFile(path)
        mFullTableModel.loadItems(isAppend)
        mFilteredTableModel.loadItems(isAppend)

        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            mStatusMethod.background = Color(0x50, 0x50, 0x00)
        }
        else {
            mStatusMethod.background = Color(0xF0, 0xF0, 0x30)
        }
        enabledFollowBtn(true)

        repaint()

        return
    }

    fun setSaveLogFile() {
        val dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HH.mm.ss")
        var device = mDeviceCombo.selectedItem!!.toString()
        device = device.substringBefore(":")
        if (mLogCmdManager.prefix.isEmpty()) {
            mLogCmdManager.prefix = LogCmdManager.DEFAULT_PREFIX
        }

        val filePath = "${mLogCmdManager.logSavePath}/${mLogCmdManager.prefix}_${device}_${dtf.format(LocalDateTime.now())}.txt"
        var file = File(filePath)
        var idx = 1
        var filePathSaved = filePath
        while (file.isFile) {
            filePathSaved = "${filePath}-$idx.txt"
            file = File(filePathSaved)
            idx++
        }

        mFullTableModel.setLogFile(filePathSaved)
        mFilteredTableModel.setLogFile(filePathSaved)
        mStatusTF.text = filePathSaved
    }

    fun startAdbScan(reconnect: Boolean) {
        if (mLogCmdManager.getType() == LogCmdManager.TYPE_CMD) {
            mStatusMethod.text = " ${Strings.CMD} "
        }
        else {
            mStatusMethod.text = " ${Strings.ADB} "
        }

        mFilteredTableModel.stopScan()
        mFilteredTableModel.stopFollow()
        mPauseToggle.isSelected = false
        setSaveLogFile()
        if (reconnect) {
            mLogCmdManager.targetDevice = mDeviceCombo.selectedItem!!.toString()
            mLogCmdManager.startLogcat()
        }
        mFilteredTableModel.startScan()
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            mStatusMethod.background = Color(0x00, 0x50, 0x00)
        }
        else {
            mStatusMethod.background = Color(0x90, 0xE0, 0x90)
        }

        enabledFollowBtn(false)
    }

    fun stopAdbScan() {
        if (mLogCmdManager.getType() == LogCmdManager.TYPE_CMD) {
            mStatusMethod.text = " ${Strings.CMD} ${Strings.STOP} "
        }
        else {
            mStatusMethod.text = " ${Strings.ADB} ${Strings.STOP} "
        }

        if (!mFilteredTableModel.isScanning()) {
            println("stopAdbScan : not adb scanning mode")
            return
        }
        mFilteredTableModel.stopScan()
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            mStatusMethod.background = Color(0x50, 0x50, 0x50)
        }
        else {
            mStatusMethod.background = Color.LIGHT_GRAY
        }

        enabledFollowBtn(true)
    }

    fun isRestartAdbLogcat(): Boolean {
        return mRetryAdbToggle.isSelected
    }

    fun restartAdbLogcat() {
        println("Restart Adb Logcat")
        mLogCmdManager.stop()
        mLogCmdManager.targetDevice = mDeviceCombo.selectedItem!!.toString()
        mLogCmdManager.startLogcat()
    }

    fun pauseAdbScan(pause: Boolean) {
        if (!mFilteredTableModel.isScanning()) {
            println("pauseAdbScan : not adb scanning mode")
            return
        }
        mFilteredTableModel.pauseScan(pause)
    }

    fun setFollowLogFile(filePath: String) {
        mFullTableModel.setLogFile(filePath)
        mFilteredTableModel.setLogFile(filePath)
        mStatusTF.text = filePath
    }

    fun startFileFollow() {
        mStatusMethod.text = " ${Strings.FOLLOW} "
        mFilteredTableModel.stopScan()
        mFilteredTableModel.stopFollow()
        mPauseFollowToggle.isSelected = false
        mFilteredTableModel.startFollow()

        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            mStatusMethod.background = Color(0x00, 0x00, 0x50)
        }
        else {
            mStatusMethod.background = Color(0xA0, 0xA0, 0xF0)
        }

        enabledFollowBtn(true)
    }

    fun stopFileFollow() {
        if (!mFilteredTableModel.isFollowing()) {
            println("stopAdbScan : not file follow mode")
            return
        }
        mStatusMethod.text = " ${Strings.FOLLOW} ${Strings.STOP} "
        mFilteredTableModel.stopFollow()
        if (ConfigManager.LaF == FLAT_DARK_LAF) {
            mStatusMethod.background = Color(0x50, 0x50, 0x50)
        }
        else {
            mStatusMethod.background = Color.LIGHT_GRAY
        }
        enabledFollowBtn(true)
    }

    fun pauseFileFollow(pause: Boolean) {
        if (!mFilteredTableModel.isFollowing()) {
            println("pauseFileFollow : not file follow mode")
            return
        }
        mFilteredTableModel.pauseFollow(pause)
    }

    private fun enabledFollowBtn(enabled: Boolean) {
        mFollowLabel.isEnabled = enabled
        mStartFollowBtn.isEnabled = enabled
        mPauseFollowToggle.isEnabled = enabled
        mStopFollowBtn.isEnabled = enabled
    }

    internal inner class ActionHandler : ActionListener {
        override fun actionPerformed(p0: ActionEvent?) {
            when (p0?.source) {
                mItemFileOpen -> {
                    val fileDialog = FileDialog(this@MainUI, Strings.FILE + " " + Strings.OPEN, FileDialog.LOAD)
                    fileDialog.isMultipleMode = false
                    fileDialog.directory = mFullTableModel.mLogFile?.parent
                    fileDialog.isVisible = true
                    if (fileDialog.file != null) {
                        val file = File(fileDialog.directory + fileDialog.file)
                        openFile(file.absolutePath, false)
                    } else {
                        println("Cancel Open")
                    }
                }
                mItemFileFollow -> {
                    val fileDialog = FileDialog(this@MainUI, Strings.FILE + " " + Strings.FOLLOW, FileDialog.LOAD)
                    fileDialog.isMultipleMode = false
                    fileDialog.directory = mFullTableModel.mLogFile?.parent
                    fileDialog.isVisible = true
                    if (fileDialog.file != null) {
                        val file = File(fileDialog.directory + fileDialog.file)
                        setFollowLogFile(file.absolutePath)
                        startFileFollow()
                    } else {
                        println("Cancel Open")
                    }
                }
                mItemFileOpenFiles -> {
                    val fileDialog = FileDialog(this@MainUI, Strings.FILE + " " + Strings.OPEN_FILES, FileDialog.LOAD)
                    fileDialog.isMultipleMode = true
                    fileDialog.directory = mFullTableModel.mLogFile?.parent
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
                mItemFileAppendFiles -> {
                    val fileDialog = FileDialog(this@MainUI, Strings.FILE + " " + Strings.APPEND_FILES, FileDialog.LOAD)
                    fileDialog.isMultipleMode = true
                    fileDialog.directory = mFullTableModel.mLogFile?.parent
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
                mItemFileExit -> {
                    exit()
                }
                mItemLogCmd, mItemLogFile -> {
                    val settingsDialog = LogCmdSettingsDialog(this@MainUI)
                    settingsDialog.setLocationRelativeTo(this@MainUI)
                    settingsDialog.isVisible = true
                }
                mItemFull -> {
                    if (mItemFull.state) {
                        attachLogPanel(mFullLogPanel)
                    } else {
                        windowedModeLogPanel(mFullLogPanel)
                    }

                    mConfigManager.saveItem(ConfigManager.ITEM_VIEW_FULL, mItemFull.state.toString())
                }

                mItemSearch -> {
                    mSearchPanel.isVisible = !mSearchPanel.isVisible
                    mItemSearch.state = mSearchPanel.isVisible
                }

                mItemFilterIncremental -> {
                    mConfigManager.saveItem(ConfigManager.ITEM_FILTER_INCREMENTAL, mItemFilterIncremental.state.toString())
                }
                mItemAppearance -> {
                    val appearanceSettingsDialog = AppearanceSettingsDialog(this@MainUI)
                    appearanceSettingsDialog.setLocationRelativeTo(this@MainUI)
                    appearanceSettingsDialog.isVisible = true
                }
                mItemAbout -> {
                    val aboutDialog = AboutDialog(this@MainUI)
                    aboutDialog.setLocationRelativeTo(this@MainUI)
                    aboutDialog.isVisible = true
                }
                mItemHelp -> {
                    val helpDialog = HelpDialog(this@MainUI)
                    helpDialog.setLocationRelativeTo(this@MainUI)
                    helpDialog.isVisible = true
                }
                mAdbConnectBtn -> {
                    stopAdbScan()
                    mLogCmdManager.targetDevice = mDeviceCombo.selectedItem!!.toString()
                    mLogCmdManager.connect()
                }
                mAdbRefreshBtn -> {
                    mLogCmdManager.getDevices()
                }
                mAdbDisconnectBtn -> {
                    stopAdbScan()
                    mLogCmdManager.disconnect()
                }
                mScrollbackApplyBtn -> {
                    try {
                        mFilteredTableModel.mScrollback = mScrollbackTF.text.toString().trim().toInt()
                    } catch (e: java.lang.NumberFormatException) {
                        mFilteredTableModel.mScrollback = 0
                        mScrollbackTF.text = "0"
                    }
                    mFilteredTableModel.mScrollbackSplitFile = mScrollbackSplitFileToggle.isSelected

                    mConfigManager.saveItem(ConfigManager.ITEM_SCROLLBACK, mScrollbackTF.text)
                    mConfigManager.saveItem(ConfigManager.ITEM_SCROLLBACK_SPLIT_FILE, mScrollbackSplitFileToggle.isSelected.toString())
                }
                mStartBtn -> {
                    startAdbScan(true)
                }
                mStopBtn -> {
                    stopAdbScan()
                    mLogCmdManager.stop()
    //            } else if (p0?.source == mPauseBtn) {
                }
                mClearViewsBtn -> {
                    mFilteredTableModel.clearItems()
                    repaint()
                }
                mSaveBtn -> {
    //                mFilteredTableModel.clearItems()
                    if (mFilteredTableModel.isScanning()) {
                        setSaveLogFile()
                    }
                    else {
                        println("SaveBtn : not adb scanning mode")
                    }
    //                repaint()
                }
                mItemRotation -> {
                    mRotationStatus++

                    if (mRotationStatus > ROTATION_MAX) {
                        mRotationStatus = ROTATION_LEFT_RIGHT
                    }

                    mConfigManager.saveItem(ConfigManager.ITEM_ROTATION, mRotationStatus.toString())

                    mLogSplitPane.remove(mFilteredLogPanel)
                    mLogSplitPane.remove(mFullLogPanel)
                    when (mRotationStatus) {
                        ROTATION_LEFT_RIGHT -> {
                            mLogSplitPane.orientation = JSplitPane.HORIZONTAL_SPLIT
                            mLogSplitPane.add(mFilteredLogPanel)
                            mLogSplitPane.add(mFullLogPanel)
                            mLogSplitPane.resizeWeight = SPLIT_WEIGHT
                        }
                        ROTATION_RIGHT_LEFT -> {
                            mLogSplitPane.orientation = JSplitPane.HORIZONTAL_SPLIT
                            mLogSplitPane.add(mFullLogPanel)
                            mLogSplitPane.add(mFilteredLogPanel)
                            mLogSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
                        }
                        ROTATION_TOP_BOTTOM -> {
                            mLogSplitPane.orientation = JSplitPane.VERTICAL_SPLIT
                            mLogSplitPane.add(mFilteredLogPanel)
                            mLogSplitPane.add(mFullLogPanel)
                            mLogSplitPane.resizeWeight = SPLIT_WEIGHT
                        }
                        ROTATION_BOTTOM_TOP -> {
                            mLogSplitPane.orientation = JSplitPane.VERTICAL_SPLIT
                            mLogSplitPane.add(mFullLogPanel)
                            mLogSplitPane.add(mFilteredLogPanel)
                            mLogSplitPane.resizeWeight = 1 - SPLIT_WEIGHT
                        }
                    }
                }
//                mFiltersBtn -> {
//                    mFiltersManager.showDialog()
//                }
//                mCmdsBtn -> {
//                    mCmdsManager.showDialog()
//                }
                mStartFollowBtn -> {
                    startFileFollow()
                }
                mStopFollowBtn -> {
                    stopFileFollow()
                }
            }
        }
    }

    internal inner class FramePopUp : JPopupMenu() {
        var mItemIconText: JMenuItem = JMenuItem("IconText")
        var mItemIcon: JMenuItem = JMenuItem("Icon")
        var mItemText: JMenuItem = JMenuItem("Text")
        private val mActionHandler = ActionHandler()

        init {
            mItemIconText.addActionListener(mActionHandler)
            add(mItemIconText)
            mItemIcon.addActionListener(mActionHandler)
            add(mItemIcon)
            mItemText.addActionListener(mActionHandler)
            add(mItemText)
        }
        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(p0: ActionEvent?) {
                when (p0?.source) {
                    mItemIconText -> {
                        setBtnIcons(true)
                        setBtnTexts(true)
                        mConfigManager.saveItem(ConfigManager.ITEM_ICON_TEXT, ConfigManager.VALUE_ICON_TEXT_I_T)
                    }
                    mItemIcon -> {
                        setBtnIcons(true)
                        setBtnTexts(false)
                        mConfigManager.saveItem(ConfigManager.ITEM_ICON_TEXT, ConfigManager.VALUE_ICON_TEXT_I)
                    }
                    mItemText -> {
                        setBtnIcons(false)
                        setBtnTexts(true)
                        mConfigManager.saveItem(ConfigManager.ITEM_ICON_TEXT, ConfigManager.VALUE_ICON_TEXT_I)
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
        var mSelectAllItem: JMenuItem
        var mCopyItem: JMenuItem
        var mPasteItem: JMenuItem
        var mReconnectItem: JMenuItem
        var mCombo: JComboBox<String>?
        private val mActionHandler = ActionHandler()

        init {
            mSelectAllItem = JMenuItem("Select All")
            mSelectAllItem.addActionListener(mActionHandler)
            add(mSelectAllItem)
            mCopyItem = JMenuItem("Copy")
            mCopyItem.addActionListener(mActionHandler)
            add(mCopyItem)
            mPasteItem = JMenuItem("Paste")
            mPasteItem.addActionListener(mActionHandler)
            add(mPasteItem)
            mReconnectItem = JMenuItem("Reconnect " + mDeviceCombo.selectedItem?.toString())
            mReconnectItem.addActionListener(mActionHandler)
            add(mReconnectItem)
            mCombo = combo
        }
        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(p0: ActionEvent?) {
                when (p0?.source) {
                    mSelectAllItem -> {
                        mCombo?.editor?.selectAll()
                    }
                    mCopyItem -> {
                        val editorCom = mCombo?.editor?.editorComponent as JTextComponent
                        val stringSelection = StringSelection(editorCom.selectedText)
                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(stringSelection, null)
                    }
                    mPasteItem -> {
                        val editorCom = mCombo?.editor?.editorComponent as JTextComponent
                        editorCom.paste()
                    }
                    mReconnectItem -> {
                        reconnectAdb()
                    }
                }
            }
        }
    }

    internal inner class PopUpFilterCombobox(combo: FilterComboBox) : JPopupMenu() {
        var mSelectAllItem: JMenuItem
        var mCopyItem: JMenuItem
        var mPasteItem: JMenuItem
        var mRemoveColorTagsItem: JMenuItem
        lateinit var mRemoveOneColorTagItem: JMenuItem
        lateinit var mAddColorTagItems: ArrayList<JMenuItem>
        var mCombo: FilterComboBox
        private val mActionHandler = ActionHandler()

        init {
            mCombo = combo
            mSelectAllItem = JMenuItem("Select All")
            mSelectAllItem.addActionListener(mActionHandler)
            add(mSelectAllItem)
            mCopyItem = JMenuItem("Copy")
            mCopyItem.addActionListener(mActionHandler)
            add(mCopyItem)
            mPasteItem = JMenuItem("Paste")
            mPasteItem.addActionListener(mActionHandler)
            add(mPasteItem)
            mRemoveColorTagsItem = JMenuItem("Remove All Color Tags")
            mRemoveColorTagsItem.addActionListener(mActionHandler)
            add(mRemoveColorTagsItem)


            if (mCombo.mUseColorTag) {
                mRemoveOneColorTagItem = JMenuItem("Remove Color Tag")
                mRemoveOneColorTagItem.addActionListener(mActionHandler)
                add(mRemoveOneColorTagItem)
                mAddColorTagItems = arrayListOf()
                for (idx in 0..8) {
                    val num = idx + 1
                    val item = JMenuItem("Add Color Tag : #$num")
                    item.isOpaque = true
                    item.foreground = Color.decode(ColorManager.getInstance().mFilterTableColor.StrFilteredFGs[num])
                    item.background = Color.decode(ColorManager.getInstance().mFilterTableColor.StrFilteredBGs[num])
                    item.addActionListener(mActionHandler)
                    mAddColorTagItems.add(item)
                    add(item)
                }
            }
        }
        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(p0: ActionEvent?) {
                when (p0?.source) {
                    mSelectAllItem -> {
                        mCombo.editor?.selectAll()
                    }
                    mCopyItem -> {
                        val editorCom = mCombo.editor?.editorComponent as JTextComponent
                        val stringSelection = StringSelection(editorCom.selectedText)
                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(stringSelection, null)
                    }
                    mPasteItem -> {
                        val editorCom = mCombo.editor?.editorComponent as JTextComponent
                        editorCom.paste()
                    }
                    mRemoveColorTagsItem -> {
                        mCombo.removeAllColorTags()
                        if (mCombo == mShowLogCombo) {
                            applyShowLogComboEditor()
                        }
                    }
                    mRemoveOneColorTagItem -> {
                        mCombo.removeColorTag()
                        if (mCombo == mShowLogCombo) {
                            applyShowLogComboEditor()
                        }
                    }
                    else -> {
                        val item = p0?.source as JMenuItem
                        if (mAddColorTagItems.contains(item)) {
                            val textSplit = item.text.split(":")
                            if (textSplit.size == 2) {
                                mCombo.addColorTag(textSplit[1].trim())
                                if (mCombo == mShowLogCombo) {
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
        override fun mouseReleased(p0: MouseEvent?) {
            if (p0 == null) {
                super.mouseReleased(p0)
                return
            }

            if (SwingUtilities.isRightMouseButton(p0)) {
                when (p0.source) {
                    mDeviceCombo.editor.editorComponent -> {
                        popupMenu = PopUpCombobox(mDeviceCombo)
                        popupMenu?.show(p0.component, p0.x, p0.y)
                    }
                    mShowLogCombo.editor.editorComponent, mBoldLogCombo.editor.editorComponent, mShowTagCombo.editor.editorComponent, mShowPidCombo.editor.editorComponent, mShowTidCombo.editor.editorComponent -> {
                        lateinit var combo: FilterComboBox
                        when (p0.source) {
                            mShowLogCombo.editor.editorComponent -> {
                                combo = mShowLogCombo
                            }
                            mBoldLogCombo.editor.editorComponent -> {
                                combo = mBoldLogCombo
                            }
                            mShowTagCombo.editor.editorComponent -> {
                                combo = mShowTagCombo
                            }
                            mShowPidCombo.editor.editorComponent -> {
                                combo = mShowPidCombo
                            }
                            mShowTidCombo.editor.editorComponent -> {
                                combo = mShowTidCombo
                            }
                        }
                        popupMenu = PopUpFilterCombobox(combo)
                        popupMenu?.show(p0.component, p0.x, p0.y)
                    }
                    else -> {
                        val compo = p0.source as JComponent
                        val event = MouseEvent(compo.parent, p0.id, p0.`when`, p0.modifiers, p0.x + compo.x, p0.y + compo.y, p0.clickCount, p0.isPopupTrigger)

                        compo.parent.dispatchEvent(event)
                    }
                }
            }
            else {
                popupMenu?.isVisible = false
            }

            super.mouseReleased(p0)
        }
    }

    fun reconnectAdb() {
        println("Reconnect ADB")
        mStopBtn.doClick()
        Thread.sleep(200)

        if (mDeviceCombo.selectedItem!!.toString().isNotBlank()) {
            mAdbConnectBtn.doClick()
            Thread.sleep(200)
        }

        Thread {
            run {
                Thread.sleep(200)
                mClearViewsBtn.doClick()
                Thread.sleep(200)
                mStartBtn.doClick()
            }
        }.start()
    }

    fun startAdbLog() {
        Thread {
            run {
                mStartBtn.doClick()
            }
        }.start()
    }

    fun stopAdbLog() {
        mStopBtn.doClick()
    }

    fun clearAdbLog() {
        Thread {
            run {
                mClearViewsBtn.doClick()
            }
        }.start()
    }

//    fun clearSaveAdbLog() {
//        Thread(Runnable {
//            run {
//                mSaveBtn.doClick()
//            }
//        }).start()
//    }

    fun getTextShowLogCombo() : String {
        if (mShowLogCombo.selectedItem == null) {
           return ""
        }
        return mShowLogCombo.selectedItem!!.toString()
    }

    fun setTextShowLogCombo(text : String) {
        mShowLogCombo.selectedItem = text
        mShowLogCombo.updateTooltip()
    }

    fun getTextSearchCombo() : String {
        if (mSearchPanel.mSearchCombo.selectedItem == null) {
            return ""
        }
        return mSearchPanel.mSearchCombo.selectedItem!!.toString()
    }

    fun setTextSearchCombo(text : String) {
        mSearchPanel.mSearchCombo.selectedItem = text
        mFilteredTableModel.mFilterSearchLog = mSearchPanel.mSearchCombo.selectedItem!!.toString()
        mSearchPanel.isVisible = true
        mItemSearch.state = mSearchPanel.isVisible
    }

    fun applyShowLogCombo() {
        val item = mShowLogCombo.selectedItem!!.toString()
        resetComboItem(mShowLogCombo, item)
        mFilteredTableModel.mFilterLog = item
    }

    fun applyShowLogComboEditor() {
        val editorCom = mShowLogCombo.editor?.editorComponent as JTextComponent
        val text = editorCom.text
        setTextShowLogCombo(text)
        applyShowLogCombo()
    }
    fun setDeviceComboColor(isConnected: Boolean) {
        if (isConnected) {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                mDeviceCombo.editor.editorComponent.foreground = Color(0x7070C0)
            }
            else {
                mDeviceCombo.editor.editorComponent.foreground = Color.BLUE
            }
        } else {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                mDeviceCombo.editor.editorComponent.foreground = Color(0xC07070)
            }
            else {
                mDeviceCombo.editor.editorComponent.foreground = Color.RED
            }
        }
    }

    fun updateLogCmdCombo(isReload: Boolean) {
        if (isReload) {
            var logCmd: String?
            val currLogCmd = mLogCmdCombo.editor.item.toString()
            mLogCmdCombo.removeAllItems()
            for (i in 0 until LogCmdManager.LOG_CMD_MAX) {
                logCmd = mConfigManager.getItem("${ConfigManager.ITEM_ADB_LOG_CMD}_$i")
                if (logCmd.isNullOrBlank()) {
                    continue
                }

                mLogCmdCombo.addItem(logCmd)
            }
            mLogCmdCombo.selectedIndex = -1
            if (currLogCmd.isBlank()) {
                mLogCmdCombo.editor.item = mLogCmdManager.logCmd
            }
            else {
                mLogCmdCombo.editor.item = currLogCmd
            }
        }

        mLogCmdCombo.toolTipText = "\"${mLogCmdManager.logCmd}\"\n\n${TooltipStrings.LOG_CMD_COMBO}"

        if (mLogCmdManager.logCmd == mLogCmdCombo.editor.item.toString()) {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                mLogCmdCombo.editor.editorComponent.foreground = Color(0x7070C0)
            }
            else {
                mLogCmdCombo.editor.editorComponent.foreground = Color.BLUE
            }
        } else {
            if (ConfigManager.LaF == FLAT_DARK_LAF) {
                mLogCmdCombo.editor.editorComponent.foreground = Color(0xC07070)
            }
            else {
                mLogCmdCombo.editor.editorComponent.foreground = Color.RED
            }
        }
    }

    internal inner class KeyHandler : KeyAdapter() {
        override fun keyReleased(p0: KeyEvent?) {
            if (KeyEvent.VK_ENTER != p0?.keyCode && p0?.source == mLogCmdCombo.editor.editorComponent) {
                updateLogCmdCombo(false)
            }

            if (KeyEvent.VK_ENTER == p0?.keyCode) {
                when {
                    p0.source == mShowLogCombo.editor.editorComponent && mShowLogToggle.isSelected -> {
                        val combo = mShowLogCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        mFilteredTableModel.mFilterLog = item
                    }
                    p0.source == mBoldLogCombo.editor.editorComponent && mBoldLogToggle.isSelected -> {
                        val combo = mBoldLogCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        mFilteredTableModel.mFilterHighlightLog = item
                    }
                    p0.source == mShowTagCombo.editor.editorComponent && mShowTagToggle.isSelected -> {
                        val combo = mShowTagCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        mFilteredTableModel.mFilterTag = item
                    }
                    p0.source == mShowPidCombo.editor.editorComponent && mShowPidToggle.isSelected -> {
                        val combo = mShowPidCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        mFilteredTableModel.mFilterPid = item
                    }
                    p0.source == mShowTidCombo.editor.editorComponent && mShowTidToggle.isSelected -> {
                        val combo = mShowTidCombo
                        val item = combo.selectedItem!!.toString()
                        resetComboItem(combo, item)
                        mFilteredTableModel.mFilterTid = item
                    }
                    p0.source == mLogCmdCombo.editor.editorComponent -> {
                        if (mLogCmdManager.logCmd == mLogCmdCombo.editor.item.toString()) {
                            reconnectAdb()
                        }
                        else {
                            val item = mLogCmdCombo.editor.item.toString().trim()

                            if (item.isEmpty()) {
                                mLogCmdCombo.editor.item = LogCmdManager.DEFAULT_LOGCAT
                            }
                            mLogCmdManager.logCmd = mLogCmdCombo.editor.item.toString()
                            updateLogCmdCombo(false)
                        }
                    }
                    p0.source == mDeviceCombo.editor.editorComponent -> {
                        reconnectAdb()
                    }
                    p0.source == mScrollbackTF -> {
                        mScrollbackApplyBtn.doClick()
                    }
                }
            } else if (p0 != null && mItemFilterIncremental.state) {
                when {
                    p0.source == mShowLogCombo.editor.editorComponent && mShowLogToggle.isSelected -> {
                        val item = mShowLogCombo.editor.item.toString()
                        mFilteredTableModel.mFilterLog = item
                    }
                    p0.source == mBoldLogCombo.editor.editorComponent && mBoldLogToggle.isSelected -> {
                        val item = mBoldLogCombo.editor.item.toString()
                        mFilteredTableModel.mFilterHighlightLog = item
                    }
                    p0.source == mShowTagCombo.editor.editorComponent && mShowTagToggle.isSelected -> {
                        val item = mShowTagCombo.editor.item.toString()
                        mFilteredTableModel.mFilterTag = item
                    }
                    p0.source == mShowPidCombo.editor.editorComponent && mShowPidToggle.isSelected -> {
                        val item = mShowPidCombo.editor.item.toString()
                        mFilteredTableModel.mFilterPid = item
                    }
                    p0.source == mShowTidCombo.editor.editorComponent && mShowTidToggle.isSelected -> {
                        val item = mShowTidCombo.editor.item.toString()
                        mFilteredTableModel.mFilterTid = item
                    }
                }
            }
            super.keyReleased(p0)
        }
    }

    internal inner class ItemHandler : ItemListener {
        override fun itemStateChanged(p0: ItemEvent?) {
            when (p0?.source) {
                mShowLogToggle -> {
                    mShowLogCombo.setEnabledFilter(mShowLogToggle.isSelected)
                }
                mBoldLogToggle -> {
                    mBoldLogCombo.setEnabledFilter(mBoldLogToggle.isSelected)
                }
                mShowTagToggle -> {
                    mShowTagCombo.setEnabledFilter(mShowTagToggle.isSelected)
                }
                mShowPidToggle -> {
                    mShowPidCombo.setEnabledFilter(mShowPidToggle.isSelected)
                }
                mShowTidToggle -> {
                    mShowTidCombo.setEnabledFilter(mShowTidToggle.isSelected)
                }
            }

            if (IsCreatingUI) {
                return
            }
            when (p0?.source) {
                mShowLogToggle -> {
                    if (mShowLogToggle.isSelected && mShowLogCombo.selectedItem != null) {
                        mFilteredTableModel.mFilterLog = mShowLogCombo.selectedItem!!.toString()
                    } else {
                        mFilteredTableModel.mFilterLog = ""
                    }
                    mConfigManager.saveItem(ConfigManager.ITEM_SHOW_LOG_CHECK, mShowLogToggle.isSelected.toString())
                }
                mBoldLogToggle -> {
                    if (mBoldLogToggle.isSelected && mBoldLogCombo.selectedItem != null) {
                        mFilteredTableModel.mFilterHighlightLog = mBoldLogCombo.selectedItem!!.toString()
                    } else {
                        mFilteredTableModel.mFilterHighlightLog = ""
                    }
                    mConfigManager.saveItem(ConfigManager.ITEM_HIGHLIGHT_LOG_CHECK, mBoldLogToggle.isSelected.toString())
                }
                mShowTagToggle -> {
                    if (mShowTagToggle.isSelected && mShowTagCombo.selectedItem != null) {
                        mFilteredTableModel.mFilterTag = mShowTagCombo.selectedItem!!.toString()
                    } else {
                        mFilteredTableModel.mFilterTag = ""
                    }
                    mConfigManager.saveItem(ConfigManager.ITEM_SHOW_TAG_CHECK, mShowTagToggle.isSelected.toString())
                }
                mShowPidToggle -> {
                    if (mShowPidToggle.isSelected && mShowPidCombo.selectedItem != null) {
                        mFilteredTableModel.mFilterPid = mShowPidCombo.selectedItem!!.toString()
                    } else {
                        mFilteredTableModel.mFilterPid = ""
                    }
                    mConfigManager.saveItem(ConfigManager.ITEM_SHOW_PID_CHECK, mShowPidToggle.isSelected.toString())
                }
                mShowTidToggle -> {
                    if (mShowTidToggle.isSelected && mShowTidCombo.selectedItem != null) {
                        mFilteredTableModel.mFilterTid = mShowTidCombo.selectedItem!!.toString()
                    } else {
                        mFilteredTableModel.mFilterTid = ""
                    }
                    mConfigManager.saveItem(ConfigManager.ITEM_SHOW_TID_CHECK, mShowTidToggle.isSelected.toString())
                }
                mMatchCaseToggle -> {
                    mFilteredTableModel.mMatchCase = mMatchCaseToggle.isSelected
                    mConfigManager.saveItem(ConfigManager.ITEM_MATCH_CASE, mMatchCaseToggle.isSelected.toString())
                }
                mScrollbackKeepToggle -> {
                    mFilteredTableModel.mScrollbackKeep = mScrollbackKeepToggle.isSelected
                }
                mRetryAdbToggle -> {
                    mConfigManager.saveItem(ConfigManager.ITEM_RETRY_ADB, mRetryAdbToggle.isSelected.toString())
                }
                mPauseToggle -> {
                    pauseAdbScan(mPauseToggle.isSelected)
                }
                mPauseFollowToggle -> {
                    pauseFileFollow(mPauseFollowToggle.isSelected)
                }
            }
        }
    }

    internal inner class LevelItemHandler : ItemListener {
        override fun itemStateChanged(p0: ItemEvent?) {
            val item = p0?.source as JRadioButtonMenuItem
            when (item.text) {
                VERBOSE ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_VERBOSE
                DEBUG ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_DEBUG
                INFO ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_INFO
                WARNING ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_WARNING
                ERROR ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_ERROR
                FATAL ->mFilteredTableModel.mFilterLevel = mFilteredTableModel.LEVEL_FATAL
            }
            mConfigManager.saveItem(ConfigManager.ITEM_LOG_LEVEL, item.text)
        }
    }

    internal inner class AdbHandler : LogCmdManager.AdbEventListener {
        override fun changedStatus(event: LogCmdManager.AdbEvent) {
            when (event.cmd) {
                LogCmdManager.CMD_CONNECT -> {
                    mLogCmdManager.getDevices()
                }
                LogCmdManager.CMD_GET_DEVICES -> {
                    if (IsCreatingUI) {
                        return
                    }
                    var selectedItem = mDeviceCombo.selectedItem
                    mDeviceCombo.removeAllItems()
                    for (item in mLogCmdManager.devices) {
                        mDeviceCombo.addItem(item)
                    }
                    if (selectedItem == null) {
                        selectedItem = ""
                    }

                    if (mLogCmdManager.devices.contains(selectedItem.toString())) {
                        mDeviceStatus.text = Strings.CONNECTED
                        setDeviceComboColor(true)
                    } else {
                        var isExist = false
                        val deviceChk = "$selectedItem:"
                        for (device in mLogCmdManager.devices) {
                            if (device.contains(deviceChk)) {
                                isExist = true
                                selectedItem = device
                                break
                            }
                        }
                        if (isExist) {
                            mDeviceStatus.text = Strings.CONNECTED
                            setDeviceComboColor(true)
                        } else {
                            mDeviceStatus.text = Strings.NOT_CONNECTED
                            setDeviceComboColor(false)
                        }
                    }
                    mDeviceCombo.selectedItem = selectedItem
                }
                LogCmdManager.CMD_DISCONNECT -> {
                    mLogCmdManager.getDevices()
                }
            }
        }
    }

    internal inner class PopupMenuHandler : PopupMenuListener {
        private var mIsCanceled = false
        override fun popupMenuWillBecomeInvisible(p0: PopupMenuEvent?) {
            if (mIsCanceled) {
                mIsCanceled = false
                return
            }
            when (p0?.source) {
                mShowLogCombo -> {
                    if (mShowLogCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = mShowLogCombo
                    val item = combo.selectedItem!!.toString()
                    if (combo.editor.item.toString() != item) {
                        return
                    }
                    resetComboItem(combo, item)
                    mFilteredTableModel.mFilterLog = item
                    combo.updateTooltip()
                }
                mBoldLogCombo -> {
                    if (mBoldLogCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = mBoldLogCombo
                    val item = combo.selectedItem!!.toString()
                    resetComboItem(combo, item)
                    mFilteredTableModel.mFilterHighlightLog = item
                    combo.updateTooltip()
                }
                mShowTagCombo -> {
                    if (mShowTagCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = mShowTagCombo
                    val item = combo.selectedItem!!.toString()
                    resetComboItem(combo, item)
                    mFilteredTableModel.mFilterTag = item
                    combo.updateTooltip()
                }
                mShowPidCombo -> {
                    if (mShowPidCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = mShowPidCombo
                    val item = combo.selectedItem!!.toString()
                    resetComboItem(combo, item)
                    mFilteredTableModel.mFilterPid = item
                    combo.updateTooltip()
                }
                mShowTidCombo -> {
                    if (mShowTidCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = mShowTidCombo
                    val item = combo.selectedItem!!.toString()
                    resetComboItem(combo, item)
                    mFilteredTableModel.mFilterTid = item
                    combo.updateTooltip()
                }

                mLogCmdCombo -> {
                    if (mLogCmdCombo.selectedIndex < 0) {
                        return
                    }
                    val combo = mLogCmdCombo
                    val item = combo.selectedItem!!.toString()
                    mLogCmdManager.logCmd = item
                    updateLogCmdCombo(false)
                }
            }
        }

        override fun popupMenuCanceled(p0: PopupMenuEvent?) {
            mIsCanceled = true
        }

        override fun popupMenuWillBecomeVisible(p0: PopupMenuEvent?) {
            val box = p0?.source as JComboBox<*>
            val comp = box.ui.getAccessibleChild(box, 0) as? JPopupMenu ?: return
            val scrollPane = comp.getComponent(0) as JScrollPane
            scrollPane.verticalScrollBar?.setUI(BasicScrollBarUI())
            scrollPane.horizontalScrollBar?.setUI(BasicScrollBarUI())
            mIsCanceled = false
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
        for (idx in 0 until mFilteredTableModel.rowCount) {
            num = mFilteredTableModel.getValueAt(idx, 0).toString().trim().toInt()
            if (line <= num) {
                mFilteredLogPanel.goToRow(idx, 0)
                break
            }
        }

        if (line != num) {
            for (idx in 0 until mFullTableModel.rowCount) {
                num = mFullTableModel.getValueAt(idx, 0).toString().trim().toInt()
                if (line <= num) {
                    mFullLogPanel.goToRow(idx, 0)
                    break
                }
            }
        }
    }

    fun markLine() {
        if (IsCreatingUI) {
            return
        }
        mSelectedLine = mFilteredLogPanel.getSelectedLine()
    }

    fun getMarkLine(): Int {
        return mSelectedLine
    }

    fun goToMarkedLine() {
        if (IsCreatingUI) {
            return
        }
        goToLine(mSelectedLine)
    }

    fun updateUIAfterVisible(args: Array<String>) {
        if (mShowLogCombo.selectedIndex >= 0 && (mShowLogComboStyle == FilterComboBox.Mode.MULTI_LINE || mShowLogComboStyle == FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT)) {
            val selectedItem = mShowLogCombo.selectedItem
            mShowLogCombo.selectedItem = ""
            mShowLogCombo.selectedItem = selectedItem
            mShowLogCombo.parent.revalidate()
            mShowLogCombo.parent.repaint()
        }
        if (mShowTagCombo.selectedIndex >= 0 && (mShowTagComboStyle == FilterComboBox.Mode.MULTI_LINE || mShowTagComboStyle == FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT)) {
            val selectedItem = mShowTagCombo.selectedItem
            mShowTagCombo.selectedItem = ""
            mShowTagCombo.selectedItem = selectedItem
            mShowTagCombo.parent.revalidate()
            mShowTagCombo.parent.repaint()
        }
        if (mBoldLogCombo.selectedIndex >= 0 && (mBoldLogComboStyle == FilterComboBox.Mode.MULTI_LINE || mBoldLogComboStyle == FilterComboBox.Mode.MULTI_LINE_HIGHLIGHT)) {
            val selectedItem = mBoldLogCombo.selectedItem
            mBoldLogCombo.selectedItem = ""
            mBoldLogCombo.selectedItem = selectedItem
            mBoldLogCombo.parent.revalidate()
            mBoldLogCombo.parent.repaint()
        }
        mColorManager.applyFilterStyle()

        mShowLogCombo.mEnabledTfTooltip = true
        mShowTagCombo.mEnabledTfTooltip = true
        mShowPidCombo.mEnabledTfTooltip = true
        mShowTidCombo.mEnabledTfTooltip = true

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
        private var mPrevText = ""
        override fun getToolTipText(event: MouseEvent?): String? {
            val textTrimmed = text.trim()
            if (mPrevText != textTrimmed && textTrimmed.isNotEmpty()) {
                mPrevText = textTrimmed
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
        var mSearchCombo: FilterComboBox
        var mSearchMatchCaseToggle: ColorToggleButton
        private var mTargetLabel: JLabel
        private var mUpBtn: ColorButton
        private var mDownBtn: ColorButton
        var mCloseBtn: ColorButton

        var mTargetView = true  // true : filter view, false : full view

        private val mSearchActionHandler = SearchActionHandler()
        private val mSearchKeyHandler = SearchKeyHandler()
        private val mSearchPopupMenuHandler = SearchPopupMenuHandler()

        init {
            mSearchCombo = FilterComboBox(FilterComboBox.Mode.SINGLE_LINE_HIGHLIGHT, false)
            mSearchCombo.preferredSize = Dimension(700, mSearchCombo.preferredSize.height)
            if (ConfigManager.LaF == CROSS_PLATFORM_LAF) {
                mSearchCombo.border = BorderFactory.createEmptyBorder(3, 0, 3, 5)
            }

            mSearchCombo.toolTipText = TooltipStrings.SEARCH_COMBO
            mSearchCombo.mEnabledTfTooltip = false
            mSearchCombo.isEditable = true
            mSearchCombo.renderer = FilterComboBox.ComboBoxRenderer()
            mSearchCombo.editor.editorComponent.addKeyListener(mSearchKeyHandler)
            mSearchCombo.addPopupMenuListener(mSearchPopupMenuHandler)

            mSearchMatchCaseToggle = ColorToggleButton("Aa")
            mSearchMatchCaseToggle.toolTipText = TooltipStrings.SEARCH_CASE_TOGGLE
            mSearchMatchCaseToggle.margin = Insets(0, 0, 0, 0)
            mSearchMatchCaseToggle.addItemListener(SearchItemHandler())
            mSearchMatchCaseToggle.background = background
            mSearchMatchCaseToggle.border = BorderFactory.createEmptyBorder()

            mUpBtn = ColorButton("▲") //△ ▲ ▽ ▼
            mUpBtn.toolTipText = TooltipStrings.SEARCH_PREV_BTN
            mUpBtn.margin = Insets(0, 7, 0, 7)
            mUpBtn.addActionListener(mSearchActionHandler)
            mUpBtn.background = background
            mUpBtn.border = BorderFactory.createEmptyBorder()

            mDownBtn = ColorButton("▼") //△ ▲ ▽ ▼
            mDownBtn.toolTipText = TooltipStrings.SEARCH_NEXT_BTN
            mDownBtn.margin = Insets(0, 7, 0, 7)
            mDownBtn.addActionListener(mSearchActionHandler)
            mDownBtn.background = background
            mDownBtn.border = BorderFactory.createEmptyBorder()

            mTargetLabel = if (mTargetView) {
                JLabel("${Strings.FILTER} ${Strings.LOG}")
            } else {
                JLabel("${Strings.FULL} ${Strings.LOG}")
            }
            mTargetLabel.toolTipText = TooltipStrings.SEARCH_TARGET_LABEL

            mCloseBtn = ColorButton("X")
            mCloseBtn.toolTipText = TooltipStrings.SEARCH_CLOSE_BTN
            mCloseBtn.margin = Insets(0, 0, 0, 0)
            mCloseBtn.addActionListener(mSearchActionHandler)
            mCloseBtn.background = background
            mCloseBtn.border = BorderFactory.createEmptyBorder()


            val searchPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
            searchPanel.add(mSearchCombo)
            searchPanel.add(mSearchMatchCaseToggle)
            searchPanel.add(mUpBtn)
            searchPanel.add(mDownBtn)

            val statusPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 2))
            statusPanel.add(mTargetLabel)
            statusPanel.add(mCloseBtn)

            layout = BorderLayout()
            add(searchPanel, BorderLayout.WEST)
            add(statusPanel, BorderLayout.EAST)
        }

        override fun setVisible(aFlag: Boolean) {
            super.setVisible(aFlag)

            if (!IsCreatingUI) {
                if (aFlag) {
                    mSearchCombo.requestFocus()
                    mSearchCombo.editor.selectAll()

                    mFilteredTableModel.mFilterSearchLog = mSearchCombo.selectedItem!!.toString()
                } else {
                    mFilteredTableModel.mFilterSearchLog = ""
                }
            }
        }

        fun setTargetView(aFlag: Boolean) {
            mTargetView = aFlag
            if (mTargetView) {
                mTargetLabel.text = "${Strings.FILTER} ${Strings.LOG}"
            } else {
                mTargetLabel.text = "${Strings.FULL} ${Strings.LOG}"
            }
        }

        fun moveToNext() {
            if (mTargetView) {
                mFilteredTableModel.moveToNextSearch()
            }
            else {
                mFullTableModel.moveToNextSearch()
            }
        }

        fun moveToPrev() {
            if (mTargetView) {
                mFilteredTableModel.moveToPrevSearch()
            }
            else {
                mFullTableModel.moveToPrevSearch()
            }
        }

        internal inner class SearchActionHandler : ActionListener {
            override fun actionPerformed(p0: ActionEvent?) {
                when (p0?.source) {
                    mUpBtn -> {
                        moveToPrev()
                    }
                    mDownBtn -> {
                        moveToNext()
                    }
                    mCloseBtn -> {
                        mSearchPanel.isVisible = false
                        mItemSearch.state = mSearchPanel.isVisible
                    }
                }
            }
        }

        internal inner class SearchKeyHandler : KeyAdapter() {
            override fun keyReleased(p0: KeyEvent?) {
                if (KeyEvent.VK_ENTER == p0?.keyCode) {
                    when (p0.source) {
                        mSearchCombo.editor.editorComponent -> {
                            val item = mSearchCombo.selectedItem!!.toString()
                            resetComboItem(mSearchCombo, item)
                            mFilteredTableModel.mFilterSearchLog = item
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
            private var mIsCanceled = false
            override fun popupMenuWillBecomeInvisible(p0: PopupMenuEvent?) {
                if (mIsCanceled) {
                    mIsCanceled = false
                    return
                }
                when (p0?.source) {
                    mSearchCombo -> {
                        if (mSearchCombo.selectedIndex < 0) {
                            return
                        }
                        val item = mSearchCombo.selectedItem!!.toString()
                        resetComboItem(mSearchCombo, item)
                        mFilteredTableModel.mFilterSearchLog = item
                        mSearchCombo.updateTooltip()
                    }
                }
            }

            override fun popupMenuCanceled(p0: PopupMenuEvent?) {
                mIsCanceled = true
            }

            override fun popupMenuWillBecomeVisible(p0: PopupMenuEvent?) {
                val box = p0?.source as JComboBox<*>
                val comp = box.ui.getAccessibleChild(box, 0) as? JPopupMenu ?: return
                val scrollPane = comp.getComponent(0) as JScrollPane
                scrollPane.verticalScrollBar?.setUI(BasicScrollBarUI())
                scrollPane.horizontalScrollBar?.setUI(BasicScrollBarUI())
                mIsCanceled = false
            }
        }

        internal inner class SearchItemHandler : ItemListener {
            override fun itemStateChanged(p0: ItemEvent?) {
                if (IsCreatingUI) {
                    return
                }
                when (p0?.source) {
                    mSearchMatchCaseToggle -> {
                        mFilteredTableModel.mSearchMatchCase = mSearchMatchCaseToggle.isSelected
                        mConfigManager.saveItem(ConfigManager.ITEM_SEARCH_MATCH_CASE, mSearchMatchCaseToggle.isSelected.toString())
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
                mSearchPanel.isVisible = false
                mItemSearch.state = mSearchPanel.isVisible
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK)
        actionMapKey = javaClass.name + ":SEARCH_OPENING"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                mSearchPanel.isVisible = true
                mItemSearch.state = mSearchPanel.isVisible
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)
        actionMapKey = javaClass.name + ":SEARCH_MOVE_PREV"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                if (mSearchPanel.isVisible) {
                    mSearchPanel.moveToPrev()
                }
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0)
        actionMapKey = javaClass.name + ":SEARCH_MOVE_NEXT"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                if (mSearchPanel.isVisible) {
                    mSearchPanel.moveToNext()
                }
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)
    }

    fun showSearchResultTooltip(isNext: Boolean, result: String) {
        val targetPanel = if (mSearchPanel.mTargetView) {
            mFilteredLogPanel
        }
        else {
            mFullLogPanel
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
        val mIsFilter = isFilter
        override fun focusGained(e: FocusEvent?) {
            super.focusGained(e)
            mSearchPanel.setTargetView(mIsFilter)
        }
    }
}

