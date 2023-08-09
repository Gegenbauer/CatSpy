package me.gegenbauer.catspy.log.ui

import com.github.weisj.darklaf.iconset.AllIcons
import com.github.weisj.darklaf.ui.util.DarkUIUtil
import com.malinskiy.adam.request.device.Device
import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.common.configuration.GThemeChangeListener
import me.gegenbauer.catspy.common.configuration.Rotation
import me.gegenbauer.catspy.common.configuration.ThemeManager
import me.gegenbauer.catspy.common.configuration.UIConfManager
import me.gegenbauer.catspy.common.log.FilterItem.Companion.emptyItem
import me.gegenbauer.catspy.common.log.FilterItem.Companion.rebuild
import me.gegenbauer.catspy.common.log.LogLevel
import me.gegenbauer.catspy.common.log.nameToLogLevel
import me.gegenbauer.catspy.common.ui.button.*
import me.gegenbauer.catspy.common.ui.combobox.*
import me.gegenbauer.catspy.common.ui.icon.DayNightIcon
import me.gegenbauer.catspy.common.ui.icon.iconTabFileLog
import me.gegenbauer.catspy.common.ui.state.StatefulPanel
import me.gegenbauer.catspy.common.ui.tab.TabPanel
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.GlobalContextManager
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.bind.bindLeft
import me.gegenbauer.catspy.databinding.bind.withName
import me.gegenbauer.catspy.databinding.property.support.*
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.ddmlib.device.DeviceListListener
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.log.model.LogcatLogItem
import me.gegenbauer.catspy.log.model.LogcatRealTimeFilter
import me.gegenbauer.catspy.log.repo.*
import me.gegenbauer.catspy.log.task.LogTaskManager
import me.gegenbauer.catspy.log.ui.dialog.GoToDialog
import me.gegenbauer.catspy.log.ui.dialog.LogTableDialog
import me.gegenbauer.catspy.log.ui.panel.FullLogPanel
import me.gegenbauer.catspy.log.ui.panel.SplitLogPane
import me.gegenbauer.catspy.log.ui.panel.next
import me.gegenbauer.catspy.log.ui.popup.FileOpenPopupMenu
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.log.viewmodel.LogMainViewModel
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.task.PeriodicTask
import me.gegenbauer.catspy.task.TaskListener
import me.gegenbauer.catspy.utils.*
import java.awt.*
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.text.JTextComponent

class LogTabPanel(override val contexts: Contexts = Contexts.default) : JPanel(), TaskListener,
    LogObservable.Observer<LogcatLogItem>, TabPanel {

    //region scoped service
    val viewModel = ServiceManager.getContextService(this, LogMainViewModel::class.java)
    private val bookmarkManager = ServiceManager.getContextService(this, BookmarkManager::class.java)
    //endregion

    //region task
    private val taskManager = ServiceManager.getContextService(this, LogTaskManager::class.java)
    private val updateLogUITask = PeriodicTask(500, "updateLogUITask")
    private val scope = MainScope()
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

    private val showLogToggle =
        ColorToggleButton(STRINGS.ui.log, STRINGS.toolTip.logToggle)
    private val showLogCombo = filterComboBox(tooltip = STRINGS.toolTip.logCombo) withName STRINGS.ui.log

    private val showTagToggle =
        ColorToggleButton(STRINGS.ui.tag, STRINGS.toolTip.tagToggle)
    private val showTagCombo = filterComboBox(tooltip = STRINGS.toolTip.tagCombo) withName STRINGS.ui.tag

    private val showPidToggle =
        ColorToggleButton(STRINGS.ui.pid, STRINGS.toolTip.pidToggle)
    private val showPidCombo = filterComboBox(tooltip = STRINGS.toolTip.pidCombo) withName STRINGS.ui.pid

    private val showTidToggle =
        ColorToggleButton(STRINGS.ui.tid, STRINGS.toolTip.tidToggle)
    private val showTidCombo = filterComboBox(tooltip = STRINGS.toolTip.tidCombo) withName STRINGS.ui.tid

    private val logLevelToggle = ColorToggleButton(STRINGS.ui.logLevel)
    private val logLevelCombo = readOnlyComboBox() withName STRINGS.ui.logLevel

    private val boldLogToggle =
        ColorToggleButton(STRINGS.ui.bold, STRINGS.toolTip.boldToggle)
    private val boldLogCombo = filterComboBox(tooltip = STRINGS.toolTip.boldCombo)

    private val matchCaseToggle = ColorToggleButton("Aa", STRINGS.toolTip.caseToggle)
    //endregion

    //region toolBarPanel
    private val toolBarPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

    //region logToolBar
    private val logToolBar = JPanel(FlowLayout(FlowLayout.LEFT)) withName "logToolBar"

    private val startBtn = StatefulButton(
        loadThemedIcon("start.svg"),
        STRINGS.ui.start,
        STRINGS.toolTip.startBtn
    )
    private val pauseToggle = StatefulToggleButton(
        loadIcon("pause_off.png"),
        DayNightIcon(loadIcon("pause_on.png"), loadIcon("pause_on_dark.png")),
        STRINGS.ui.pause,
        tooltip = STRINGS.toolTip.pauseBtn
    )
    private val stopBtn = StatefulButton(
        loadIcon("stop.png"),
        STRINGS.ui.stop,
        STRINGS.toolTip.stopBtn
    )
    private val saveBtn = StatefulButton(
        loadIcon("save.svg"),
        STRINGS.ui.save,
        STRINGS.toolTip.saveBtn
    )
    private val deviceCombo = readOnlyComboBox(STRINGS.toolTip.devicesCombo)
    private val rotateLogPanelBtn =
        StatefulButton(
            loadThemedIcon("rotate.svg"),
            STRINGS.ui.rotation,
            STRINGS.toolTip.rotationBtn
        )
    private val clearViewsBtn = StatefulButton(
        loadIcon("clear.png"),
        STRINGS.ui.clearViews,
        STRINGS.toolTip.clearBtn
    )
    //endregion
    //endregion

    //region searchPanel
    private val searchPanel by lazy { SearchPanel() }
    //endregion
    //endregion

    //region splitLogPane
    private val splitLogWithStatefulPanel = StatefulPanel()
    private val fullTableModel = LogTableModel(fullLogcatRepository)
    private val filteredTableModel = LogTableModel(filteredLogcatRepository)
    internal val splitLogPane = SplitLogPane(fullTableModel, filteredTableModel).apply {
        onFocusGained = {
            searchPanel.setTargetView(it)
        }
    }
    //endregion

    //region statusBar
    private val statusBar = JPanel(BorderLayout())

    private val statusMethod = JLabel()
    private val logFilePath = StatusTextField(STRINGS.ui.none) applyTooltip STRINGS.toolTip.savedFileTf
    //endregion

    //region menu
    private val filePopupMenu = FileOpenPopupMenu().apply {
        onFileSelected = { file ->
            openFile(file.absolutePath, false)
        }
        onFileFollowSelected = { file ->
            startFileFollow(file.absolutePath)
        }
        onFileListSelected = {
            it.forEachIndexed { index, file ->
                openFile(file.absolutePath, index == it.indices.first)
            }
        }
        onFilesAppendSelected = { it.forEach { openFile(it.absolutePath, true) } }
    }
    //endregion

    //region events
    private val logPanelMouseListener = LogPanelMouseListener()
    private val keyHandler = KeyHandler()
    private val actionHandler = ActionHandler()
    private val mouseHandler = MouseHandler()
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
        bindViewModel(viewModel)
        ThemeManager.registerThemeUpdateListener(viewModel)
    }

    private fun bindViewModel(viewModel: LogMainViewModel) {
        viewModel.apply {
            //region Toolbar
            //region Filter
            bindLogFilter(
                showLogCombo,
                showLogToggle,
                logFilterSelectedIndex,
                logFilterHistory,
                logFilterEnabled,
                logFilterCurrentContent,
                logFilterErrorMessage
            )
            bindLogFilter(
                showTagCombo,
                showTagToggle,
                tagFilterSelectedIndex,
                tagFilterHistory,
                tagFilterEnabled,
                tagFilterCurrentContent,
                tagFilterErrorMessage
            )
            bindLogFilter(
                showPidCombo,
                showPidToggle,
                pidFilterSelectedIndex,
                pidFilterHistory,
                pidFilterEnabled,
                pidFilterCurrentContent,
                pidFilterErrorMessage
            )
            bindLogFilter(
                showTidCombo,
                showTidToggle,
                tidFilterSelectedIndex,
                tidFilterHistory,
                tidFilterEnabled,
                tidFilterCurrentContent,
                tidFilterErrorMessage
            )
            bindLogFilter(
                logLevelCombo,
                logLevelToggle,
                logLevelFilterSelectedIndex,
                logLevelFilterHistory,
                logLevelFilterEnabled,
                logLevelFilterCurrentContent
            )
            bindLogFilter(
                boldLogCombo,
                boldLogToggle,
                boldSelectedIndex,
                boldHistory,
                boldEnabled,
                boldCurrentContent,
                boldErrorMessage
            )

            selectedProperty(matchCaseToggle) bindDual filterMatchCaseEnabled
            //endregion

            //region ADB
            bindNormalCombo(deviceCombo, deviceSelectedIndex, connectedDevices, currentDevice)

            selectedProperty(pauseToggle) bindDual pauseAll
            //endregion

            //region Menu
            customProperty(splitLogPane, "rotation", Rotation.ROTATION_LEFT_RIGHT) bindDual rotation
            //endregion

            //endregion

            //region SearchBar
            listProperty(searchPanel.searchCombo) bindDual searchHistory
            selectedIndexProperty(searchPanel.searchCombo) bindLeft searchSelectedIndex
            textProperty(searchPanel.searchCombo.editorComponent) bindDual searchCurrentContent
            customProperty(searchPanel.searchCombo, "errorMsg", "") bindDual searchErrorMessage

            visibilityProperty(searchPanel) bindDual searchPanelVisible

            selectedProperty(searchPanel.searchMatchCaseToggle) bindDual searchMatchCase
            //endregion

            //region LogPanel
            dividerProperty(splitLogPane) bindDual splitPanelDividerLocation
            //endregion

            //region status bar
            textProperty(statusMethod) bindDual status
            textProperty(logFilePath) bindDual filePath
            //endregion

            //region Style
            bindWithButtonDisplayMode(startBtn, stopBtn, pauseToggle, saveBtn, clearViewsBtn)
            //endregion

            logLevelFilterCurrentContent.addObserver {
                logLevel.updateValue(nameToLogLevel[it] ?: LogLevel.VERBOSE)
            }

            syncGlobalConfWithMainViewModel()
        }
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        splitLogPane.setContexts(contexts)
        filePopupMenu.setContexts(contexts)

        ServiceManager.getContextService(AdamDeviceManager::class.java).registerDevicesListener(devicesChangeListener)
        registerSearchStroke()
    }

    private fun observeViewModelValue() {
        viewModel.apply {
            pauseAll.addObserver {
                taskManager.updatePauseState(it == true)
            }
            filterMatchCaseEnabled.addObserver { updateLogFilter() }
            logLevel.addObserver { updateLogFilter() }
            logFilterEnabled.addObserver { updateLogFilter() }
            tagFilterEnabled.addObserver { updateLogFilter() }
            pidFilterEnabled.addObserver { updateLogFilter() }
            tidFilterEnabled.addObserver { updateLogFilter() }
            logLevelFilterEnabled.addObserver { updateLogFilter() }
            boldEnabled.addObserver { updateLogFilter() }
            searchMatchCase.addObserver { filteredTableModel.searchMatchCase = it == true }
            searchMatchCase.addObserver { updateLogFilter() }
            filePath.addObserver { updateTitleBar(status.value?.trim() ?: "") }
            logFont.addObserver {
                it ?: return@addObserver
                splitLogPane.filteredLogPanel.table.font = it
                splitLogPane.fullLogPanel.table.font = it
            }
        }
    }

    private fun refreshDevices(devices: List<Device>) {
        viewModel.connectedDevices.updateValue((devices.map { it.serial }).toHistoryItemList())
        viewModel.currentDevice.updateValue(devices.firstOrNull()?.serial)
    }

    private fun saveConfiguration() {
        UIConfManager.uiConf.logFontSize = customFont.size
        UIConfManager.uiConf.logFontName = customFont.name
        UIConfManager.uiConf.logFontStyle = customFont.style
        UIConfManager.saveUI()
    }

    private fun createUI() {
        boldLogCombo.enabledTfTooltip = false

        deviceCombo.setWidth(150)

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
        logFilePath.isEditable = false
        logFilePath.border = BorderFactory.createEmptyBorder()

        statusBar.add(statusMethod, BorderLayout.WEST)
        statusBar.add(logFilePath, BorderLayout.CENTER)

        customFont = UIConfManager.uiConf.getLogFont()
        GLog.d(TAG, "[createUI] log font: $customFont")
        splitLogPane.filteredLogPanel.customFont = customFont
        splitLogPane.fullLogPanel.customFont = customFont

        searchPanel.searchMatchCaseToggle.isSelected = UIConfManager.uiConf.searchMatchCaseEnabled
        filteredTableModel.searchMatchCase = UIConfManager.uiConf.searchMatchCaseEnabled
        fullTableModel.searchMatchCase = UIConfManager.uiConf.searchMatchCaseEnabled

        splitLogWithStatefulPanel.setContent(splitLogPane)
        splitLogWithStatefulPanel.action = {
            SwingUtilities.updateComponentTreeUI(filePopupMenu)
            filePopupMenu.show(it, it.width / 2, it.height / 2)
        }

        add(filterPanel, BorderLayout.NORTH)
        add(splitLogWithStatefulPanel, BorderLayout.CENTER)
        add(statusBar, BorderLayout.SOUTH)
    }

    private fun registerEvent() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { event ->
            when {
                event.keyCode == KeyEvent.VK_PAGE_DOWN && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> {
                    splitLogPane.filteredLogPanel.moveToLastRow()
                    splitLogPane.fullLogPanel.moveToLastRow()
                }

                event.keyCode == KeyEvent.VK_PAGE_UP && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> {
                    splitLogPane.filteredLogPanel.moveToFirstRow()
                    splitLogPane.fullLogPanel.moveToFirstRow()
                }

                event.keyCode == KeyEvent.VK_L && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> {
                    deviceCombo.requestFocus()
                }

                event.keyCode == KeyEvent.VK_G && (event.modifiersEx and KeyEvent.CTRL_DOWN_MASK) != 0 -> {
                    val goToDialog = GoToDialog(findFrameFromParent())
                    goToDialog.setLocationRelativeTo(this@LogTabPanel)
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
        filterPanel.addMouseListener(mouseHandler)
        toolBarPanel.addMouseListener(mouseHandler)

        fullTableModel.addLogTableModelListener { event ->
            splitLogWithStatefulPanel.state = if ((event.source as LogTableModel).rowCount > 0) {
                StatefulPanel.State.NORMAL
            } else {
                StatefulPanel.State.EMPTY
            }
        }
        filteredTableModel.state.addObserver {
            splitLogPane.filterStatefulPanel.state = it ?: StatefulPanel.State.NONE
        }
        splitLogPane.fullLogPanel.table.selectionModel.addListSelectionListener {
            fullLogcatRepository.selectedRow = splitLogPane.fullLogPanel.table.selectedRow.takeIf { it >= 0 } ?: -1
        }
        splitLogPane.filteredLogPanel.table.selectionModel.addListSelectionListener {
            filteredLogcatRepository.selectedRow =
                splitLogPane.filteredLogPanel.table.selectedRow.takeIf { it >= 0 } ?: -1
        }
        logProvider.addObserver(this)
    }

    private fun registerComboBoxEditorEvent() {
        showLogCombo.keyListener = keyHandler
        showLogCombo.keyListener = keyHandler
        boldLogCombo.keyListener = keyHandler
        showTagCombo.keyListener = keyHandler
        showPidCombo.keyListener = keyHandler
        showTidCombo.keyListener = keyHandler
        deviceCombo.keyListener = keyHandler
        showLogCombo.mouseListener = mouseHandler
        boldLogCombo.mouseListener = mouseHandler
        showTagCombo.mouseListener = mouseHandler
        showPidCombo.mouseListener = mouseHandler
        showTidCombo.mouseListener = mouseHandler
        deviceCombo.mouseListener = mouseHandler
        searchPanel.registerComboBoxEditorEvent()
    }

    private fun updateTitleBar(statusMethod: String) {
        val frame = DarkUIUtil.getParentOfType(JFrame::class.java, this)
        frame?.title = when (statusMethod) {
            STRINGS.ui.open, STRINGS.ui.follow, "${STRINGS.ui.follow} ${STRINGS.ui.stop}" -> {
                val path: Path = Paths.get(logFilePath.text)
                path.fileName.toString()
            }

            STRINGS.ui.adb, STRINGS.ui.cmd, "${STRINGS.ui.adb} ${STRINGS.ui.stop}", "${STRINGS.ui.cmd} ${STRINGS.ui.stop}" -> {
                (viewModel.currentDevice.value ?: "").ifEmpty { STRINGS.ui.app }
            }

            else -> {
                STRINGS.ui.app
            }
        }
    }

    private fun detachLogPanel(logPanel: FullLogPanel) {
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

    private fun attachLogPanel(logPanel: FullLogPanel) {
        logPanel.isWindowedMode = false
        splitLogPane.resetWithCurrentRotation()
    }

    fun openFile(path: String, isAppend: Boolean) {
        GLog.d(TAG, "[openFile] Opening: $path, $isAppend")
        viewModel.status.updateValue(" ${STRINGS.ui.open} ")
        logProvider.clear()
        updateLogFilter()

        if (updateLogUITask.isRunning().not()) {
            taskManager.exec(updateLogUITask)
        }
        logProvider.stopCollectLog()
        logProvider.startCollectLog(FileLogCollector(taskManager, path))

        if (isAppend) {
            viewModel.filePath.updateValue(viewModel.filePath.value + path)
        } else {
            viewModel.filePath.updateValue(path)
        }
    }

    fun startAdbScan() {
        viewModel.status.updateValue(" ${STRINGS.ui.adb} ")
        updateLogFilter()

        if (updateLogUITask.isRunning().not()) {
            taskManager.exec(updateLogUITask)
        }
        logProvider.stopCollectLog()
        logProvider.clear()
        logProvider.startCollectLog(RealTimeLogCollector(taskManager, viewModel.currentDevice.value ?: ""))
        viewModel.pauseAll.updateValue(false)

        viewModel.filePath.updateValue(logProvider.logTempFile?.absolutePath ?: "")
    }

    private fun stopAll() {
        viewModel.status.updateValue(" ${STRINGS.ui.adb} ${STRINGS.ui.stop} ")

        if (!logProvider.isCollecting()) {
            GLog.d(TAG, "stopAdbScan : not adb scanning mode")
            return
        }

        logProvider.stopCollectLog()
        taskManager.cancelAll()
    }

    private fun startFileFollow(filePath: String) {
        viewModel.filePath.updateValue(filePath)
        viewModel.status.updateValue(" ${STRINGS.ui.follow} ")
    }

    private inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {

                startBtn -> {
                    startAdbScan()
                }

                stopBtn -> {
                    stopScan()
                }

                rotateLogPanelBtn -> {
                    viewModel.rotation.value?.let {
                        viewModel.rotation.updateValue(it.next())
                    }
                }

                clearViewsBtn -> {
                    clearViews()
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

    fun stopScan() {
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
                    val anchor = invoker as JComponent
                    val logMainUI = DarkUIUtil.getParentOfType(LogTabPanel::class.java, anchor)
                    logMainUI.viewModel.buttonDisplayMode.updateValue(
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
        private val actionHandler = ActionHandler()

        init {
            add(selectAllItem)
            add(copyItem)
            add(pasteItem)
            selectAllItem.addActionListener(actionHandler)
            copyItem.addActionListener(actionHandler)
            pasteItem.addActionListener(actionHandler)
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

    fun clearAdbLog() {
        clearViews()
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
        resetComboItem(viewModel.logFilterHistory, viewModel.logFilterCurrentContent.value ?: "")
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
                }
            }
            super.keyReleased(event)
        }
    }

    private fun updateComboBox() {
        resetComboItem(viewModel.logFilterHistory, viewModel.logFilterCurrentContent.value ?: "")
        resetComboItem(viewModel.tagFilterHistory, viewModel.tagFilterCurrentContent.value ?: "")
        resetComboItem(viewModel.pidFilterHistory, viewModel.pidFilterCurrentContent.value ?: "")
        resetComboItem(viewModel.tidFilterHistory, viewModel.tidFilterCurrentContent.value ?: "")
        resetComboItem(viewModel.boldHistory, viewModel.boldCurrentContent.value ?: "")
    }

    private fun updateLogFilter() {
        scope.launch {
            viewModel.apply {
                val matchCase = filterMatchCaseEnabled.getValueNonNull()
                LogcatRealTimeFilter(
                    if (logFilterEnabled.getValueNonNull()) showLogCombo.filterItem.rebuild(matchCase) else emptyItem,
                    if (tagFilterEnabled.getValueNonNull()) showTagCombo.filterItem.rebuild(matchCase) else emptyItem,
                    if (pidFilterEnabled.getValueNonNull()) showPidCombo.filterItem.rebuild(matchCase) else emptyItem,
                    if (tidFilterEnabled.getValueNonNull()) showTidCombo.filterItem.rebuild(matchCase) else emptyItem,
                    if (logLevelFilterEnabled.getValueNonNull()) viewModel.logLevel.getValueNonNull() else LogLevel.VERBOSE,
                    viewModel.filterMatchCaseEnabled.getValueNonNull()
                ).apply {
                    filteredLogcatRepository.logFilter = this
                    fullLogcatRepository.logFilter = this
                }
                filteredTableModel.highlightFilterItem =
                    if (boldEnabled.getValueNonNull()) boldLogCombo.filterItem.rebuild(matchCase) else emptyItem
            }
        }
    }

    private fun updateSearchFilter() {
        filteredTableModel.searchFilterItem = searchPanel.searchCombo.filterItem
        fullTableModel.searchFilterItem = searchPanel.searchCombo.filterItem
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

    private fun goToLine(lineNumber: Int) {
        GLog.d(TAG, "[goToLine] $lineNumber")

        lineNumber.takeIf { it > 0 } ?: return

        splitLogPane.filteredLogPanel.goToLineIndex(filteredTableModel.getRowIndex(lineNumber))
        splitLogPane.fullLogPanel.goToLineIndex(fullTableModel.getRowIndex(lineNumber))
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
        val closeBtn = GButton(loadDarklafThemedIcon("navigation/close.svg")) applyTooltip STRINGS.toolTip.searchCloseBtn
        val searchCombo: FilterComboBox = filterComboBox() applyTooltip STRINGS.toolTip.searchCombo
        val searchMatchCaseToggle: ColorToggleButton =
            ColorToggleButton("Aa") applyTooltip STRINGS.toolTip.searchCaseToggle
        var isInternalTargetView = true  // true : filter view, false : full view

        private var targetLabel: JLabel = if (isInternalTargetView) {
            JLabel("${STRINGS.ui.filter} ${STRINGS.ui.log}")
        } else {
            JLabel("${STRINGS.ui.full} ${STRINGS.ui.log}")
        } applyTooltip STRINGS.toolTip.searchTargetLabel
        private val upBtn = GButton(AllIcons.Arrow.Thick.Up.get()) applyTooltip STRINGS.toolTip.searchPrevBtn //△ ▲ ▽ ▼
        private val downBtn = GButton(AllIcons.Arrow.Thick.Down.get()) applyTooltip STRINGS.toolTip.searchNextBtn
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
            searchCombo.keyListener = searchKeyHandler
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
                filteredTableModel.moveToNextSearchResult()
            } else {
                fullTableModel.moveToNextSearchResult()
            }
        }

        fun moveToPrev() {
            if (isInternalTargetView) {
                filteredTableModel.moveToPreviousSearchResult()
            } else {
                fullTableModel.moveToPreviousSearchResult()
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
                            resetComboItem(viewModel.searchHistory, viewModel.searchCurrentContent.value ?: "")
                            updateSearchFilter()
                            this@SearchPanel::moveToPrev.takeIf { KeyEvent.SHIFT_DOWN_MASK == event.modifiersEx }
                                ?.invoke() ?: this@SearchPanel::moveToNext.invoke()
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
                        searchCombo.selectedIndex.takeIf { it > 0 } ?: return
                        resetComboItem(viewModel.searchHistory, viewModel.searchCurrentContent.value ?: "")
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

        targetPanel.toolTipText = ""
    }

    override fun onError(error: Throwable) {
        GLog.e(TAG, "[onError]", error)
        JOptionPane.showMessageDialog(
            this,
            error,
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }

    override val tabName: String
        get() = "Log"
    override val tabIcon: Icon = iconTabFileLog
    override val tabTooltip: String?
        get() = null
    override val tabMnemonic: Char
        get() = ' '

    override fun onTabSelected() {
        taskManager.updatePauseState(false)
    }

    override fun onTabUnselected() {
        taskManager.updatePauseState(true)
    }

    override fun dispose() {
        ServiceManager.dispose(this)
        ThemeManager.unregisterThemeUpdateListener(viewModel)
        logProvider.destroy()
        clearViews()
        taskManager.cancelAll()
        saveConfiguration()
    }

    override fun getTabContent(): JComponent {
        return this
    }

    private fun registerSearchStroke() {
        var stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
        var actionMapKey = javaClass.name + ":SEARCH_CLOSING"
        var action: Action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                viewModel.searchPanelVisible.updateValue(false)
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)
        actionMapKey = javaClass.name + ":SEARCH_OPENING"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                viewModel.searchPanelVisible.updateValue(true)
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

    companion object {
        private const val TAG = "LogMainUI"
    }
}
