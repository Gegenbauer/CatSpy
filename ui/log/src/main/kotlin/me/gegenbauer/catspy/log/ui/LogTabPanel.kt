package me.gegenbauer.catspy.log.ui

import com.github.weisj.darklaf.iconset.AllIcons
import com.github.weisj.darklaf.ui.util.DarkUIUtil
import com.malinskiy.adam.request.device.Device
import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.Rotation
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.UIConfManager
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
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.LogLevel
import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.log.model.LogcatLogItem
import me.gegenbauer.catspy.log.model.LogcatRealTimeFilter
import me.gegenbauer.catspy.log.nameToLogLevel
import me.gegenbauer.catspy.log.repo.*
import me.gegenbauer.catspy.log.task.LogTask
import me.gegenbauer.catspy.log.task.LogTaskManager
import me.gegenbauer.catspy.log.ui.dialog.GoToDialog
import me.gegenbauer.catspy.log.ui.dialog.LogTableDialog
import me.gegenbauer.catspy.log.ui.panel.FullLogPanel
import me.gegenbauer.catspy.log.ui.panel.SplitLogPane
import me.gegenbauer.catspy.log.ui.panel.nextRotation
import me.gegenbauer.catspy.log.ui.popup.FileOpenPopupMenu
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.strings.Configuration
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.task.PeriodicTask
import me.gegenbauer.catspy.task.Task
import me.gegenbauer.catspy.task.TaskListener
import me.gegenbauer.catspy.utils.*
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.button.IconBarButton
import me.gegenbauer.catspy.view.combobox.*
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.emptyItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.rebuild
import me.gegenbauer.catspy.view.state.StatefulPanel
import me.gegenbauer.catspy.view.tab.TabPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.*
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class LogTabPanel(override val contexts: Contexts = Contexts.default) : JPanel(), TaskListener,
    LogObservable.Observer<LogcatLogItem>, TabPanel {

    override val tabName: String = TAB_NAME
    override val tabIcon: Icon = GIcons.Tab.FileLog.get(TAB_ICON_SIZE, TAB_ICON_SIZE)

    //region scoped service
    val logMainBinding = ServiceManager.getContextService(this, LogMainBinding::class.java)
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

    private val showLogToggle = ColorToggleButton(Configuration.LOG, STRINGS.toolTip.logToggle)
    private val showLogCombo = filterComboBox(tooltip = STRINGS.toolTip.logCombo) withName Configuration.LOG

    private val showTagToggle = ColorToggleButton(Configuration.TAG, STRINGS.toolTip.tagToggle)
    private val showTagCombo = filterComboBox(tooltip = STRINGS.toolTip.tagCombo) withName Configuration.TAG

    private val showPidToggle = ColorToggleButton(Configuration.PID, STRINGS.toolTip.pidToggle)
    private val showPidCombo = filterComboBox(tooltip = STRINGS.toolTip.pidCombo) withName Configuration.PID

    private val showTidToggle = ColorToggleButton(Configuration.TID, STRINGS.toolTip.tidToggle)
    private val showTidCombo = filterComboBox(tooltip = STRINGS.toolTip.tidCombo) withName Configuration.TID

    private val logLevelToggle = ColorToggleButton(STRINGS.ui.logLevel, STRINGS.toolTip.logLevelToggle)
    private val logLevelCombo = readOnlyComboBox(STRINGS.toolTip.logLevelCombo) withName STRINGS.ui.logLevel

    private val boldLogToggle = ColorToggleButton(STRINGS.ui.bold, STRINGS.toolTip.boldToggle)
    private val boldLogCombo = filterComboBox(tooltip = STRINGS.toolTip.boldCombo)

    private val matchCaseToggle = ColorToggleButton(Configuration.MATCH_CASE, STRINGS.toolTip.caseToggle)
    //endregion

    //region toolBarPanel
    private val toolBarPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))

    //region logToolBar
    private val logToolBar = JPanel(FlowLayout(FlowLayout.LEFT)) withName "logToolBar"

    private val startBtn = IconBarButton(
        GIcons.Action.Start.get(),
        STRINGS.toolTip.startBtn,
        GIcons.Action.Start.disabled()
    )
    private val stopBtn = IconBarButton(
        GIcons.Action.Stop.get(),
        STRINGS.toolTip.stopBtn,
        GIcons.Action.Stop.disabled()
    )
    private val saveBtn = IconBarButton(GIcons.Action.Save.get(), STRINGS.toolTip.saveBtn)
    private val deviceCombo = readOnlyComboBox(STRINGS.toolTip.devicesCombo)
    private val rotateLogPanelBtn = IconBarButton(GIcons.Action.Rotate.get(), STRINGS.toolTip.rotationBtn)
    private val clearViewsBtn = IconBarButton(GIcons.Action.Clear.get(), STRINGS.toolTip.clearBtn)
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
    private val keyHandler = KeyHandler()
    private val actionHandler = ActionHandler()
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

    private var state: TaskState? = null
        set(value) {
            if (field != value) value?.updateUI()
            field = value
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
        bind(logMainBinding)
        ThemeManager.registerThemeUpdateListener(logMainBinding)
    }

    private fun bind(viewModel: LogMainBinding) {
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

            logLevelFilterCurrentContent.addObserver {
                logLevel.updateValue(nameToLogLevel[it] ?: LogLevel.VERBOSE)
            }

            syncGlobalConfWithMainBindings()
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
        logMainBinding.apply {
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
        logMainBinding.connectedDevices.updateValue((devices.map { it.serial }).toHistoryItemList())
        logMainBinding.currentDevice.updateValue(devices.firstOrNull()?.serial)
        startBtn.isEnabled = devices.isEmpty().not()
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
        logToolBar.add(stopBtn)
        logToolBar.addVSeparator2()
        logToolBar.add(saveBtn)
        logToolBar.addVSeparator2()
        logToolBar.add(deviceCombo)
        logToolBar.addVSeparator2()
        logToolBar.add(rotateLogPanelBtn)
        logToolBar.addVSeparator2()
        logToolBar.add(clearViewsBtn)
        state = TaskIdle(this)

        toolBarPanel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        toolBarPanel.add(logToolBar)

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
        registerStrokes()

        // fix bug: event registered for editor in ComboBox will be removed when ComboBoxUI changed
        registerComboBoxEditorEvent()

        startBtn.addActionListener(actionHandler)
        stopBtn.addActionListener(actionHandler)
        clearViewsBtn.addActionListener(actionHandler)
        rotateLogPanelBtn.addActionListener(actionHandler)
        saveBtn.addActionListener(actionHandler)

        fullTableModel.addLogTableModelListener { event ->
            scope.launch(Dispatchers.UI.immediate) {
                splitLogWithStatefulPanel.state = if ((event.source as LogTableModel).rowCount > 0) {
                    StatefulPanel.State.NORMAL
                } else {
                    StatefulPanel.State.EMPTY
                }
            }
        }
        filteredTableModel.state.addObserver {
            scope.launch(Dispatchers.UI.immediate) {
                splitLogPane.filterStatefulPanel.state = it ?: StatefulPanel.State.NONE
            }
        }
        splitLogPane.fullLogPanel.table.selectionModel.addListSelectionListener {
            fullLogcatRepository.selectedRow = splitLogPane.fullLogPanel.table.selectedRow.takeIf { it >= 0 } ?: -1
        }
        splitLogPane.filteredLogPanel.table.selectionModel.addListSelectionListener {
            filteredLogcatRepository.selectedRow =
                splitLogPane.filteredLogPanel.table.selectedRow.takeIf { it >= 0 } ?: -1
        }
        logProvider.addObserver(this)
        taskManager.addListener(this)
    }

    private fun registerStrokes() {
        registerStroke(Key.C_PAGE_DOWN, "Log Move To Last Row") {
            splitLogPane.filteredLogPanel.moveToLastRow()
            splitLogPane.fullLogPanel.moveToLastRow()
        }
        registerStroke(Key.C_PAGE_UP, "Log Move To First Row") {
            splitLogPane.filteredLogPanel.moveToFirstRow()
            splitLogPane.fullLogPanel.moveToFirstRow()
        }
        registerStroke(Key.C_L, "Device Combo Request Focus") { deviceCombo.requestFocus() }
        registerStroke(Key.C_G, "Go To Target Log Line") {
            val goToDialog = GoToDialog(findFrameFromParent())
            goToDialog.setLocationRelativeTo(this@LogTabPanel)
            goToDialog.isVisible = true
            if (goToDialog.line > 0) {
                GLog.d(TAG, "[KeyEventDispatcher] Cancel Goto Line ${goToDialog.line}")
                goToLine(goToDialog.line)
            } else {
                GLog.d(TAG, "[KeyEventDispatcher] Cancel Goto Line")
            }
        }
        registerStroke(Key.C_K, "Stop All Log Task") { stopAll() }
        registerStroke(Key.C_P, "Start Logcat") {
            startBtn.doClick()
        }
        registerStroke(Key.C_O, "Select Log File") { filePopupMenu.onClickFileOpen() }
        registerStroke(Key.C_DELETE, "Clear All Logs") { clearViews() }
    }

    private fun registerComboBoxEditorEvent() {
        showLogCombo.keyListener = keyHandler
        showLogCombo.keyListener = keyHandler
        boldLogCombo.keyListener = keyHandler
        showTagCombo.keyListener = keyHandler
        showPidCombo.keyListener = keyHandler
        showTidCombo.keyListener = keyHandler
        deviceCombo.keyListener = keyHandler
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
                (logMainBinding.currentDevice.value ?: "").ifEmpty { Configuration.APP_NAME }
            }

            else -> {
                Configuration.APP_NAME
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
        stopAll()
        logProvider.stopCollectLog()
        logProvider.clear()

        GLog.d(TAG, "[openFile] Opening: $path, $isAppend")
        logMainBinding.status.updateValue(" ${STRINGS.ui.open} ")
        updateLogFilter()
        startUpdateUITask()

        logProvider.startCollectLog(FileLogCollector(taskManager, path))

        if (isAppend) {
            logMainBinding.filePath.updateValue(logMainBinding.filePath.value + path)
        } else {
            logMainBinding.filePath.updateValue(path)
        }
    }

    private fun startUpdateUITask() {
        if (updateLogUITask.isRunning.not()) {
            taskManager.exec(updateLogUITask)
        }
        updateLogUITask.resume()
        logMainBinding.pauseAll.updateValue(false)
    }

    fun startLogcat() {
        if (logMainBinding.connectedDevices.value.isNullOrEmpty()) return
        logMainBinding.status.updateValue(" ${STRINGS.ui.adb} ")
        updateLogFilter()

        startUpdateUITask()
        logProvider.stopCollectLog()
        logProvider.clear()
        logProvider.startCollectLog(RealTimeLogCollector(taskManager, logMainBinding.currentDevice.value ?: ""))
        logMainBinding.filePath.updateValue(logProvider.logTempFile?.absolutePath ?: "")
        repaint()
    }

    private fun stopAll() {
        if (state is TaskIdle) return
        logMainBinding.status.updateValue(" ${STRINGS.ui.adb} ${STRINGS.ui.stop} ")

        logProvider.stopCollectLog()
        taskManager.cancelAll { task -> task != updateLogUITask }
        if (taskManager.isAnyTaskRunning { it is LogTask }.not()) {
            state = TaskIdle(this)
        }
        updateLogUITask.pause()
    }

    private fun startFileFollow(filePath: String) {
        logMainBinding.filePath.updateValue(filePath)
        logMainBinding.status.updateValue(" ${STRINGS.ui.follow} ")
    }

    private inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {

                startBtn -> {
                    state?.onStartClicked()
                }

                stopBtn -> {
                    stopScan()
                }

                rotateLogPanelBtn -> {
                    logMainBinding.rotation.value?.let {
                        logMainBinding.rotation.updateValue(it.nextRotation())
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
        resetComboItem(logMainBinding.logFilterHistory, logMainBinding.logFilterCurrentContent.value ?: "")
    }

    private inner class KeyHandler : KeyAdapter() {
        override fun keyReleased(event: KeyEvent) {
            if (event.keyEventInfo == Key.C_ENTER.released()) {
                val receivedTargets = listOf(
                    showLogCombo.editorComponent,
                    boldLogCombo.editorComponent,
                    showTagCombo.editorComponent,
                    showPidCombo.editorComponent,
                    showTidCombo.editorComponent,
                    boldLogCombo.editorComponent,
                )
                if (event.source in receivedTargets) {
                    updateComboBox()
                    updateLogFilter()
                }
            }
            super.keyReleased(event)
        }
    }

    private fun updateComboBox() {
        resetComboItem(logMainBinding.logFilterHistory, logMainBinding.logFilterCurrentContent.value ?: "")
        resetComboItem(logMainBinding.tagFilterHistory, logMainBinding.tagFilterCurrentContent.value ?: "")
        resetComboItem(logMainBinding.pidFilterHistory, logMainBinding.pidFilterCurrentContent.value ?: "")
        resetComboItem(logMainBinding.tidFilterHistory, logMainBinding.tidFilterCurrentContent.value ?: "")
        resetComboItem(logMainBinding.boldHistory, logMainBinding.boldCurrentContent.value ?: "")
    }

    private fun updateLogFilter() {
        scope.launch {
            logMainBinding.apply {
                val matchCase = filterMatchCaseEnabled.getValueNonNull()
                LogcatRealTimeFilter(
                    if (logFilterEnabled.getValueNonNull()) showLogCombo.filterItem.rebuild(matchCase) else emptyItem,
                    if (tagFilterEnabled.getValueNonNull()) showTagCombo.filterItem.rebuild(matchCase) else emptyItem,
                    if (pidFilterEnabled.getValueNonNull()) showPidCombo.filterItem.rebuild(matchCase) else emptyItem,
                    if (tidFilterEnabled.getValueNonNull()) showTidCombo.filterItem.rebuild(matchCase) else emptyItem,
                    if (logLevelFilterEnabled.getValueNonNull()) logMainBinding.logLevel.getValueNonNull() else LogLevel.VERBOSE,
                    logMainBinding.filterMatchCaseEnabled.getValueNonNull()
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

    private fun resetComboItem(
        viewModelProperty: ObservableViewModelProperty<List<HistoryItem<String>>>,
        item: String
    ) {
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
        val closeBtn = IconBarButton(AllIcons.Navigation.Close.get()) applyTooltip STRINGS.toolTip.searchCloseBtn
        val searchCombo: FilterComboBox = filterComboBox() applyTooltip STRINGS.toolTip.searchCombo
        val searchMatchCaseToggle: ColorToggleButton =
            ColorToggleButton("Aa") applyTooltip STRINGS.toolTip.searchCaseToggle
        var isInternalTargetView = true  // true : filter view, false : full view

        private var targetLabel: JLabel = if (isInternalTargetView) {
            JLabel("${STRINGS.ui.filter} ${STRINGS.ui.log}")
        } else {
            JLabel("${STRINGS.ui.full} ${STRINGS.ui.log}")
        } applyTooltip STRINGS.toolTip.searchTargetLabel
        private val upBtn = IconBarButton(GIcons.Action.Up.get()) applyTooltip STRINGS.toolTip.searchPrevBtn //△ ▲ ▽ ▼
        private val downBtn = IconBarButton(GIcons.Action.Down.get()) applyTooltip STRINGS.toolTip.searchNextBtn
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
                        logMainBinding.searchPanelVisible.updateValue(false)
                    }
                }
            }
        }

        private inner class SearchKeyHandler : KeyAdapter() {
            override fun keyReleased(event: KeyEvent) {
                if (event.keyEventInfo == Key.ENTER.released() || event.keyEventInfo == Key.S_ENTER.released()) {
                    resetComboItem(logMainBinding.searchHistory, logMainBinding.searchCurrentContent.value ?: "")
                    updateSearchFilter()
                    this@SearchPanel::moveToPrev
                        .takeIf { KeyEvent.SHIFT_DOWN_MASK == event.modifiersEx }
                        ?.invoke() ?: this@SearchPanel::moveToNext.invoke()
                }
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
                        resetComboItem(logMainBinding.searchHistory, logMainBinding.searchCurrentContent.value ?: "")
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

    override fun onStart(task: Task) {
        super.onStart(task)
        checkCurrentState()
    }

    override fun onStop(task: Task) {
        super.onStop(task)
        checkCurrentState()
    }

    override fun onPause(task: Task) {
        super.onPause(task)
        checkCurrentState()
    }

    override fun onResume(task: Task) {
        super.onResume(task)
        checkCurrentState()
    }

    override fun onTabSelected() {
        taskManager.updatePauseState(false)
    }

    override fun onTabUnselected() {
        taskManager.updatePauseState(true)
    }

    private fun checkCurrentState() {
        state = when {
            taskManager.isAnyTaskRunning { it is LogTask }.not() -> TaskIdle(this)
            taskManager.isPaused() -> TaskPaused(this)
            else -> TaskStarted(this)
        }
    }

    override fun destroy() {
        stopAll()
        ServiceManager.dispose(this)
        ThemeManager.unregisterThemeUpdateListener(logMainBinding)
        logProvider.destroy()
        clearViews()
        taskManager.cancelAll()
        saveConfiguration()
        splitLogPane.destroy()
    }

    override fun getTabContent(): JComponent {
        return this
    }

    private fun registerSearchStroke() {
        var stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
        var actionMapKey = javaClass.name + ":SEARCH_CLOSING"
        var action: Action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                logMainBinding.searchPanelVisible.updateValue(false)
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)
        actionMapKey = javaClass.name + ":SEARCH_OPENING"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                logMainBinding.searchPanelVisible.updateValue(true)
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

    private abstract class TaskState(protected val ui: LogTabPanel) {
        abstract fun updateUI()

        open fun onStartClicked() {}
    }

    private class TaskStarted(ui: LogTabPanel) : TaskState(ui) {
        override fun updateUI() {
            ui.startBtn.icon = GIcons.Action.Pause.get()
            ui.startBtn.isEnabled = true
            ui.startBtn.toolTipText = STRINGS.toolTip.pauseBtn
            ui.stopBtn.isEnabled = true
            ui.stopBtn.icon = GIcons.Action.Stop.get()
        }

        override fun onStartClicked() {
            ui.logMainBinding.pauseAll.updateValue(true)
        }
    }

    private class TaskIdle(ui: LogTabPanel) : TaskState(ui) {
        override fun updateUI() {
            ui.startBtn.isEnabled = ui.logMainBinding.connectedDevices.value.isNullOrEmpty().not()
            ui.startBtn.icon = GIcons.Action.Start.get()
            ui.startBtn.toolTipText = STRINGS.toolTip.startBtn
            ui.stopBtn.isEnabled = false
        }

        override fun onStartClicked() {
            ui.startLogcat()
        }
    }

    private class TaskPaused(ui: LogTabPanel) : TaskState(ui) {
        override fun updateUI() {
            ui.startBtn.icon = GIcons.Action.Start.get()
            ui.startBtn.toolTipText = STRINGS.toolTip.startBtn
            ui.stopBtn.isVisible = true
            ui.stopBtn.icon = GIcons.Action.Stop.get()
        }

        override fun onStartClicked() {
            ui.logMainBinding.pauseAll.updateValue(false)
        }
    }

    companion object {
        private const val TAG = "LogMainUI"
        private const val TAB_NAME = "Log"
    }
}
