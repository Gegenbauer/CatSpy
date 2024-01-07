package me.gegenbauer.catspy.log.ui.panel

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.IgnoreFastCallbackScheduler
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.Rotation
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.GlobalContextManager
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.bind.bindLeft
import me.gegenbauer.catspy.databinding.property.support.*
import me.gegenbauer.catspy.file.getFileName
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.java.ext.ErrorEvent
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.LogLevel
import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.log.datasource.LogViewModel
import me.gegenbauer.catspy.log.datasource.TaskState
import me.gegenbauer.catspy.log.model.LogcatFilter
import me.gegenbauer.catspy.log.nameToLogLevel
import me.gegenbauer.catspy.log.ui.dialog.GoToDialog
import me.gegenbauer.catspy.log.ui.dialog.LogTableDialog
import me.gegenbauer.catspy.log.ui.popup.FileOpenPopupMenu
import me.gegenbauer.catspy.log.ui.table.FilteredLogTableModel
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.strings.GlobalStrings
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.*
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.button.IconBarButton
import me.gegenbauer.catspy.view.combobox.FilterComboBox
import me.gegenbauer.catspy.view.combobox.HistoryItem
import me.gegenbauer.catspy.view.combobox.filterComboBox
import me.gegenbauer.catspy.view.combobox.readOnlyComboBox
import me.gegenbauer.catspy.view.dialog.FileSaveHandler
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.EMPTY_ITEM
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.rebuild
import me.gegenbauer.catspy.view.panel.StatusBar
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.state.StatefulPanel
import me.gegenbauer.catspy.view.tab.TabPanel
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.*
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*

abstract class BaseLogMainPanel(override val contexts: Contexts = Contexts.default) : JPanel(), TabPanel {

    abstract val tag: String
    override val tabName: String = STRINGS.ui.tabLogFile
    override val tabIcon: Icon = GIcons.Tab.FileLog.get(TAB_ICON_SIZE, TAB_ICON_SIZE)

    protected abstract val processFilterToggle: ColorToggleButton

    protected abstract val processFilterCombo: FilterComboBox

    protected abstract val currentPidFilter: FilterItem

    protected abstract val currentPackageFilter: FilterItem

    protected open val processComboWidthWeight: Double = 0.10

    //region scoped service
    val logMainBinding by lazy { ServiceManager.getContextService(this, LogMainBinding::class.java) }
    //endregion

    //region task
    protected val scope = MainScope()
    private var taskState: TaskUIState = TaskNone()
        set(value) {
            field = value
            value.updateUI()
            afterTaskStateChanged(value)
        }
    //endregion

    //region log data
    protected val logViewModel = LogViewModel()
    //endregion

    //region filterPanel
    private val filterPanel = JPanel()

    //region logPanel
    private val logPanel = JPanel()

    private val messageFilterToggle = ColorToggleButton(GlobalStrings.LOG, STRINGS.toolTip.logToggle)
    protected val messageFilterCombo = filterComboBox(tooltip = STRINGS.toolTip.logToggle)

    private val tagFilterToggle = ColorToggleButton(GlobalStrings.TAG, STRINGS.toolTip.tagToggle)
    protected val tagFilterCombo = filterComboBox(tooltip = STRINGS.toolTip.tagToggle)

    private val tidFilterToggle = ColorToggleButton(GlobalStrings.TID, STRINGS.toolTip.tidToggle)
    protected val tidFilterCombo = filterComboBox(tooltip = STRINGS.toolTip.tidToggle)

    private val logLevelFilterToggle = ColorToggleButton(STRINGS.ui.logLevel, STRINGS.toolTip.logLevelToggle)
    private val logLevelFilterCombo = readOnlyComboBox(STRINGS.toolTip.logLevelCombo)

    private val boldLogToggle = ColorToggleButton(STRINGS.ui.bold, STRINGS.toolTip.boldToggle)
    protected val boldLogCombo = filterComboBox(tooltip = STRINGS.toolTip.boldToggle)

    private val matchCaseToggle = ColorToggleButton(GlobalStrings.MATCH_CASE, STRINGS.toolTip.caseToggle)
    private val saveFilterToHistoryScheduler = IgnoreFastCallbackScheduler(Dispatchers.UI, 5 * 1000)
    //endregion

    //region toolBarPanel
    private val toolBarPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))

    //region logToolBar
    private val logToolBar = JPanel(FlowLayout(FlowLayout.LEFT))

    protected val startBtn = IconBarButton(
        GIcons.Action.Start.get(),
        STRINGS.toolTip.startBtn,
        GIcons.Action.Start.disabled()
    )
    private val stopBtn = IconBarButton(
        GIcons.Action.Stop.get(),
        STRINGS.toolTip.stopBtn,
        GIcons.Action.Stop.disabled()
    )
    protected val saveBtn = IconBarButton(GIcons.Action.Save.get(), STRINGS.toolTip.saveBtn)
    protected val deviceCombo = readOnlyComboBox(STRINGS.toolTip.devicesCombo)
    private val rotateLogPanelBtn = IconBarButton(GIcons.Action.Rotate.get(), STRINGS.toolTip.rotationBtn)
    private val clearViewsBtn = IconBarButton(GIcons.Action.Clear.get(), STRINGS.toolTip.clearBtn)
    //endregion
    //endregion

    //region searchPanel
    private val searchPanel by lazy { SearchPanel(filteredTableModel) }
    //endregion
    //endregion

    //region splitLogPane
    protected val splitLogWithStatefulPanel = StatefulPanel()
    protected open val fullTableModel = LogTableModel(logViewModel)
    protected open val filteredTableModel = FilteredLogTableModel(logViewModel)
    private val splitLogPane by lazy {
        SplitLogPane(fullTableModel, filteredTableModel, onFocusGained = {
            searchPanel.currentLogTableModel = if (it) filteredTableModel else fullTableModel
        })
    }
    private val fullTable by lazy { splitLogPane.fullLogPanel.table }
    private val filteredTable by lazy { splitLogPane.filteredLogPanel.table }
    //endregion

    //region statusBar
    private var logStatus: StatusBar.LogStatus = StatusBar.LogStatus.NONE
        set(value) {
            field = value
            statusBar.logStatus = value
            updateTitleBar(value.status)
        }
    private val statusBar = ServiceManager.getContextService(StatusPanel::class.java)
    //endregion

    //region menu
    private val filePopupMenu = FileOpenPopupMenu().apply {
        onFileSelected = { file ->
            openFile(file.absolutePath)
        }
    }
    //endregion

    //region events
    private val keyHandler = KeyHandler()
    private val actionHandler = ActionHandler()
    //endregion

    var logFont: Font = Font(
        SettingsManager.settings.logFontName,
        SettingsManager.settings.logFontStyle,
        SettingsManager.settings.logFontSize
    )
        set(value) {
            field = value
            splitLogPane.filteredLogPanel.customFont = value
            splitLogPane.fullLogPanel.customFont = value
        }

    private var updateFilterJob: Job? = null
    private var updateSearchFilterJob: Job? = null

    init {
        isVisible = false
    }

    override fun setup() {
        GlobalContextManager.register(this)
        scope.launch {
            createUI()

            registerEvent()

            observeViewModelValue()

            ThemeManager.registerThemeUpdateListener(logMainBinding)
            bind(logMainBinding)
            isVisible = true
            logViewModel.preCacheFilters()
        }
    }

    private fun bind(viewModel: LogMainBinding) {
        viewModel.apply {
            //region Toolbar
            //region Filter
            bindLogFilter(
                messageFilterCombo,
                messageFilterToggle,
                logFilterSelectedIndex,
                logFilterHistory,
                logFilterEnabled,
                logFilterCurrentContent,
                logFilterErrorMessage
            )
            bindLogFilter(
                tagFilterCombo,
                tagFilterToggle,
                tagFilterSelectedIndex,
                tagFilterHistory,
                tagFilterEnabled,
                tagFilterCurrentContent,
                tagFilterErrorMessage
            )

            bindProcessComponents(this)

            bindLogFilter(
                tidFilterCombo,
                tidFilterToggle,
                tidFilterSelectedIndex,
                tidFilterHistory,
                tidFilterEnabled,
                tidFilterCurrentContent,
                tidFilterErrorMessage
            )
            bindLogFilter(
                logLevelFilterCombo,
                logLevelFilterToggle,
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
            bindDeviceComponents(this)
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

            logLevelFilterCurrentContent.addObserver {
                logLevel.updateValue(nameToLogLevel[it] ?: LogLevel.VERBOSE)
            }

            syncGlobalConfWithMainBindings()
        }
    }

    protected abstract fun bindProcessComponents(mainBinding: LogMainBinding)

    protected open fun bindDeviceComponents(mainBinding: LogMainBinding) {
        mainBinding.apply {
            bindNormalCombo(deviceCombo, deviceSelectedIndex, connectedDevices, currentDevice)
        }
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)

        ServiceManager.getContextService(this, BookmarkManager::class.java)

        splitLogPane.setParent(this)
        filePopupMenu.setParent(this)
        searchPanel.setParent(this)
    }

    private fun observeViewModelValue() {
        logMainBinding.apply {
            pauseAll.addObserver {
                logViewModel.setPaused(it == true)
            }
            configureUpdateLogFilterTriggers(
                filterMatchCaseEnabled, logLevel, logFilterEnabled, logFilterCurrentContent,
                tagFilterEnabled, tagFilterCurrentContent, pidFilterEnabled, pidFilterCurrentContent,
                packageFilterEnabled, packageFilterCurrentContent, tidFilterEnabled,
                tidFilterCurrentContent, logLevelFilterEnabled, boldEnabled, searchMatchCase, boldCurrentContent
            )
            setAutoSaveToHistory(
                logFilterHistory to logFilterCurrentContent,
                tagFilterHistory to tagFilterCurrentContent,
                pidFilterHistory to pidFilterCurrentContent,
                packageFilterHistory to packageFilterCurrentContent,
                tidFilterHistory to tidFilterCurrentContent,
                boldHistory to boldCurrentContent,
                searchHistory to searchCurrentContent
            )
            searchCurrentContent.addObserver { updateSearchFilter() }
            logFont.addObserver {
                it ?: return@addObserver
                filteredTable.font = it
                fullTable.font = it
            }
        }
    }

    private fun configureUpdateLogFilterTriggers(vararg observableValueProperty: ObservableValueProperty<*>) {
        observableValueProperty.forEach {
            (it as? ObservableValueProperty<Any>)?.addObserver { updateLogFilter() }
        }
    }

    private fun setAutoSaveToHistory(
        vararg comboBoxPropertyPair: Pair<ObservableValueProperty<List<HistoryItem<String>>>, ObservableValueProperty<String>>
    ) {
        comboBoxPropertyPair.forEach { (history, currentContent) ->
            currentContent.addObserver { content ->
                saveFilterToHistoryScheduler.schedule {
                    if ((content?.length ?: 0) > 1) {
                        saveFilter(history, content ?: "")
                    }
                }
            }
        }
    }

    private fun saveConfiguration() {
        SettingsManager.updateSettings {
            logFontSize = this@BaseLogMainPanel.logFont.size
            logFontName = this@BaseLogMainPanel.logFont.name
            logFontStyle = this@BaseLogMainPanel.logFont.style
        }
    }

    protected open fun createUI() {
        boldLogCombo.enabledTfTooltip = false

        deviceCombo.setWidth(150)

        val p = PREFERRED
        logPanel.layout = TableLayout(
            doubleArrayOf(p, FILL, p, 0.20, p, processComboWidthWeight, p, 0.10, p, 0.10, p, 0.15, p),
            doubleArrayOf(p)
        )
        logPanel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        logPanel.add(messageFilterToggle, "0, 0")
        logPanel.add(messageFilterCombo, "1, 0")
        logPanel.add(tagFilterToggle, "2, 0")
        logPanel.add(tagFilterCombo, "3, 0")
        logPanel.add(processFilterToggle, "4, 0")
        logPanel.add(processFilterCombo, "5, 0")
        logPanel.add(tidFilterToggle, "6, 0")
        logPanel.add(tidFilterCombo, "7, 0")
        logPanel.add(logLevelFilterToggle, "8, 0")
        logPanel.add(logLevelFilterCombo, "9, 0")
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

        toolBarPanel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        toolBarPanel.add(logToolBar)

        filterPanel.add(toolBarPanel, BorderLayout.NORTH)
        filterPanel.add(searchPanel, BorderLayout.SOUTH)

        layout = BorderLayout()

        splitLogPane.fullLogPanel.updateTableBar()
        splitLogPane.filteredLogPanel.updateTableBar()

        splitLogPane.isOneTouchExpandable = false

        logFont = SettingsManager.settings.logFont
        GLog.d(tag, "[createUI] log font: $logFont")
        splitLogPane.filteredLogPanel.customFont = logFont
        splitLogPane.fullLogPanel.customFont = logFont

        searchPanel.searchMatchCaseToggle.isSelected = SettingsManager.settings.searchMatchCaseEnabled

        splitLogWithStatefulPanel.setContent(splitLogPane)
        splitLogWithStatefulPanel.action = {
            SwingUtilities.updateComponentTreeUI(filePopupMenu)
            filePopupMenu.onClickFileOpen()
        }

        add(filterPanel, BorderLayout.NORTH)
        add(splitLogWithStatefulPanel, BorderLayout.CENTER)

        taskState = TaskIdle(this)
    }

    protected open fun registerEvent() {
        registerStrokes()

        registerComboBoxEditorEvent()

        startBtn.addActionListener(actionHandler)
        stopBtn.addActionListener(actionHandler)
        clearViewsBtn.addActionListener(actionHandler)
        rotateLogPanelBtn.addActionListener(actionHandler)
        saveBtn.addActionListener(actionHandler)

        observeFullLogListState()
        observeEventFlow()
    }

    private fun observeFullLogListState() {
        scope.launch {
            logViewModel.fullLogListState.collect {
                splitLogWithStatefulPanel.listState = it
            }
        }
    }

    private fun observeEventFlow() {
        scope.launch {
            logViewModel.eventFlow.collect {
                when (it) {
                    is ErrorEvent -> {
                        handleErrorEvent(it)
                    }

                    is TaskState -> {
                        handleTaskStateEvent(it)
                    }
                }
            }
        }
    }

    private fun handleErrorEvent(event: ErrorEvent) {
        GLog.e(tag, "[handleErrorEvent] error", event.error)
        JOptionPane.showMessageDialog(
            this@BaseLogMainPanel,
            event.error.message ?: STRINGS.ui.unknownError,
            STRINGS.ui.error,
            JOptionPane.ERROR_MESSAGE
        )
    }

    private fun handleTaskStateEvent(taskState: TaskState) {
        this.taskState = when (taskState) {
            TaskState.IDLE -> {
                TaskIdle(this@BaseLogMainPanel)
            }

            TaskState.RUNNING -> {
                TaskStarted(this@BaseLogMainPanel)
            }

            TaskState.PAUSED -> {
                TaskPaused(this@BaseLogMainPanel)
            }
        }
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
            goToDialog.setLocationRelativeTo(this@BaseLogMainPanel)
            goToDialog.isVisible = true
            if (goToDialog.line > 0) {
                GLog.d(tag, "[KeyEventDispatcher] Cancel Goto Line ${goToDialog.line}")
                goToLine(goToDialog.line)
            } else {
                GLog.d(tag, "[KeyEventDispatcher] Cancel Goto Line")
            }
        }
        registerStroke(Key.C_K, "Stop All Log Task") { stopAll() }
        registerStroke(Key.C_P, "Start Logcat") { startBtn.doClick() }
        registerStroke(Key.C_O, "Select Log File") { filePopupMenu.onClickFileOpen() }
        registerStroke(Key.C_DELETE, "Clear All Logs") { clearAllLogs() }
        registerStroke(Key.ESCAPE, "Close Search Panel") { logMainBinding.searchPanelVisible.updateValue(false) }
        registerStroke(Key.C_F, "Open Search Panel") { logMainBinding.searchPanelVisible.updateValue(true) }
        registerStroke(Key.F3, "Move To Previous Search Result") {
            if (searchPanel.isVisible) {
                searchPanel.moveToPrev()
            }
        }
        registerStroke(Key.F4, "Move To Next Search Result") {
            if (searchPanel.isVisible) {
                searchPanel.moveToNext()
            }
        }
        registerStroke(Key.C_S, "Save Log") { saveLog() }
    }

    private fun registerComboBoxEditorEvent() {
        messageFilterCombo.keyListener = keyHandler
        messageFilterCombo.keyListener = keyHandler
        boldLogCombo.keyListener = keyHandler
        tagFilterCombo.keyListener = keyHandler
        processFilterCombo.keyListener = keyHandler
        tidFilterCombo.keyListener = keyHandler
        deviceCombo.keyListener = keyHandler
        searchPanel.registerComboBoxEditorEvent()
    }

    private fun updateTitleBar(statusMethod: String) {
        val frame = DarkUIUtil.getParentOfType(JFrame::class.java, this)
        frame?.title = when (statusMethod) {
            STRINGS.ui.open, STRINGS.ui.follow, "${STRINGS.ui.follow} ${STRINGS.ui.stop}" -> {
                val path: Path = Paths.get(logStatus.path)
                path.fileName.toString()
            }

            STRINGS.ui.adb, STRINGS.ui.cmd, "${STRINGS.ui.adb} ${STRINGS.ui.stop}", "${STRINGS.ui.cmd} ${STRINGS.ui.stop}" -> {
                (logMainBinding.currentDevice.value ?: "").ifEmpty { GlobalProperties.APP_NAME }
            }

            else -> {
                GlobalProperties.APP_NAME
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

    internal fun showLogPanelInWindow(logPanel: FullLogPanel) {
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

    fun openFile(path: String) {
        stopAll()

        logViewModel.clear()
        logViewModel.startProduceFileLog(path)

        GLog.d(tag, "[openFile] Opening: $path")
        updateLogFilter()

        logStatus = StatusBar.LogStatusIdle(" ${STRINGS.ui.open} ", path)
    }

    fun startLogcat() {
        stopAll()
        if (logMainBinding.connectedDevices.value.isNullOrEmpty()) return

        logViewModel.clear()
        logViewModel.startProduceDeviceLog(logMainBinding.currentDevice.value ?: "")

        GLog.d(tag, "[startLogcat] device: ${logMainBinding.currentDevice.value ?: ""}")

        updateLogFilter()

        logStatus =
            StatusBar.LogStatusRunning(" ${STRINGS.ui.adb} ", logViewModel.tempLogFile.absolutePath ?: "")
    }

    fun stopAll() {
        logStatus = StatusBar.LogStatusIdle(" ${STRINGS.ui.adb} ${STRINGS.ui.stop} ")
        logViewModel.cancel()
    }

    private inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                startBtn -> {
                    taskState.onStartClicked()
                }

                stopBtn -> {
                    stopAll()
                }

                rotateLogPanelBtn -> {
                    logMainBinding.rotation.value?.let {
                        logMainBinding.rotation.updateValue(it.nextRotation())
                    }
                }

                clearViewsBtn -> {
                    clearAllLogs()
                }

                saveBtn -> {
                    saveLog()
                }
            }
        }
    }

    private fun saveLog() {
        FileSaveHandler.Builder(this@BaseLogMainPanel)
            .onFileSpecified(logViewModel::saveLog)
            .setDefaultName(logStatus.path.getFileName())
            .build()
            .show()
    }

    fun clearAllLogs() {
        logViewModel.clear()
    }

    fun getTextShowLogCombo(): String {
        if (messageFilterCombo.selectedItem == null) {
            return ""
        }
        return messageFilterCombo.selectedItem!!.toString()
    }

    fun updateMessageFilter(text: String) {
        messageFilterCombo.selectedItem = text
        messageFilterCombo.updateTooltip()
        saveFilter(logMainBinding.logFilterHistory, logMainBinding.logFilterCurrentContent.value ?: "")
    }

    private inner class KeyHandler : KeyAdapter() {
        override fun keyReleased(event: KeyEvent) {
            if (event.keyEventInfo == Key.C_ENTER.released()) {
                val receivedTargets = listOf(
                    messageFilterCombo.editorComponent,
                    boldLogCombo.editorComponent,
                    tagFilterCombo.editorComponent,
                    processFilterCombo.editorComponent,
                    tidFilterCombo.editorComponent,
                    boldLogCombo.editorComponent,
                )
                if (event.source in receivedTargets) {
                    updateComboBox()
                    updateLogFilter()
                    event.consume()
                }
            }
        }
    }

    private fun updateComboBox() {
        saveFilter(logMainBinding.logFilterHistory, logMainBinding.logFilterCurrentContent.value ?: "")
        saveFilter(logMainBinding.tagFilterHistory, logMainBinding.tagFilterCurrentContent.value ?: "")
        saveFilter(logMainBinding.pidFilterHistory, logMainBinding.pidFilterCurrentContent.value ?: "")
        saveFilter(logMainBinding.packageFilterHistory, logMainBinding.packageFilterCurrentContent.value ?: "")
        saveFilter(logMainBinding.tidFilterHistory, logMainBinding.tidFilterCurrentContent.value ?: "")
        saveFilter(logMainBinding.boldHistory, logMainBinding.boldCurrentContent.value ?: "")
    }

    private fun updateLogFilter() {
        val lastUpdateJob = updateFilterJob
        updateFilterJob = scope.launch {
            lastUpdateJob?.cancelAndJoin()
            updateFilterComboBoxFilterItems(messageFilterCombo, tagFilterCombo, processFilterCombo, tidFilterCombo, boldLogCombo)
            logMainBinding.apply {
                val matchCase = filterMatchCaseEnabled.getValueNonNull()
                LogcatFilter(
                    if (logFilterEnabled.getValueNonNull()) messageFilterCombo.filterItem.rebuild(matchCase) else EMPTY_ITEM,
                    if (tagFilterEnabled.getValueNonNull()) tagFilterCombo.filterItem.rebuild(matchCase) else EMPTY_ITEM,
                    currentPidFilter,
                    currentPackageFilter,
                    if (tidFilterEnabled.getValueNonNull()) tidFilterCombo.filterItem.rebuild(matchCase) else EMPTY_ITEM,
                    if (logLevelFilterEnabled.getValueNonNull()) logMainBinding.logLevel.getValueNonNull() else LogLevel.NONE,
                    logMainBinding.filterMatchCaseEnabled.getValueNonNull()
                ).apply {
                    logViewModel.updateFilter(this)
                    fullTable.repaint()
                }
                filteredTableModel.highlightFilterItem =
                    if (boldEnabled.getValueNonNull()) boldLogCombo.filterItem.rebuild(matchCase) else EMPTY_ITEM
                fullTableModel.highlightFilterItem =
                    if (boldEnabled.getValueNonNull()) boldLogCombo.filterItem.rebuild(matchCase) else EMPTY_ITEM
            }
        }
    }

    private fun updateFilterComboBoxFilterItems(vararg filterComboBox: FilterComboBox) {
        filterComboBox.forEach(FilterComboBox::buildFilterItem)
    }

    private fun updateSearchFilter() {
        val lastUpdateJob = updateSearchFilterJob
        updateSearchFilterJob = scope.launch {
            lastUpdateJob?.cancelAndJoin()
            updateFilterComboBoxFilterItems(searchPanel.searchCombo)
            val searchItem = EMPTY_ITEM.takeUnless { logMainBinding.searchPanelVisible.getValueNonNull() } ?:
            searchPanel.searchCombo.filterItem.rebuild(logMainBinding.searchMatchCase.getValueNonNull())
            filteredTableModel.searchFilterItem = searchItem
            fullTableModel.searchFilterItem = searchItem
        }
    }

    private fun saveFilter(
        viewModelProperty: ObservableValueProperty<List<HistoryItem<String>>>,
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
        GLog.d(tag, "[goToLine] $lineNumber")

        lineNumber.takeIf { it > 0 } ?: return

        splitLogPane.filteredLogPanel.goToRowIndex(filteredTableModel.getRowIndexInAllPages(lineNumber))
        splitLogPane.fullLogPanel.goToRowIndex(fullTableModel.getRowIndexInAllPages(lineNumber))
    }

    fun showSearchResultTooltip(isNext: Boolean, result: String) {
        val targetPanel = if (searchPanel.currentLogTableModel is FilteredLogTableModel) {
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

    override fun onTabSelected() {
        if (logViewModel.isPaused().not()) {
            logViewModel.resume()
        }
        updateTitleBar(logStatus.status)
        statusBar.logStatus = logStatus
    }

    override fun onTabUnselected() {
        if (logViewModel.isPaused().not()) {
            logViewModel.pause()
        }
        statusBar.logStatus = StatusBar.LogStatus.NONE
    }

    override fun destroy() {
        super.destroy()
        stopAll()
        ServiceManager.dispose(this)
        ThemeManager.unregisterThemeUpdateListener(logMainBinding)
        logViewModel.destroy()
        clearAllLogs()
        saveConfiguration()
        splitLogPane.destroy()
    }

    override fun getTabContent(): JComponent {
        return this
    }

    protected open fun afterTaskStateChanged(state: TaskUIState) {
        // no-op
    }

    abstract class TaskUIState(protected val ui: BaseLogMainPanel?) {
        abstract fun updateUI()

        open fun onStartClicked() {
            // no-op
        }
    }

    internal class TaskNone : TaskUIState(null) {
        override fun updateUI() {
            // no-op
        }

        override fun onStartClicked() {
            // no-op
        }
    }

    internal open class TaskStarted(ui: BaseLogMainPanel) : TaskUIState(ui) {
        override fun updateUI() {
            ui ?: return
            ui.startBtn.icon = GIcons.Action.Pause.get()
            ui.startBtn.isEnabled = true
            ui.startBtn.toolTipText = STRINGS.toolTip.pauseBtn
            ui.stopBtn.isEnabled = true
            ui.stopBtn.icon = GIcons.Action.Stop.get()
            ui.deviceCombo.isEnabled = false
        }

        override fun onStartClicked() {
            ui ?: return
            ui.logMainBinding.pauseAll.updateValue(true)
        }
    }

    internal class TaskIdle(ui: BaseLogMainPanel) : TaskUIState(ui) {
        override fun updateUI() {
            ui ?: return
            ui.startBtn.isEnabled = ui.logMainBinding.connectedDevices.value.isNullOrEmpty().not()
            ui.startBtn.icon = GIcons.Action.Start.get()
            ui.startBtn.toolTipText = STRINGS.toolTip.startBtn
            ui.logMainBinding.pauseAll.updateValue(false)
            ui.stopBtn.isEnabled = false
            ui.deviceCombo.isEnabled = true
        }

        override fun onStartClicked() {
            ui ?: return
            ui.startLogcat()
        }
    }

    internal class TaskPaused(ui: BaseLogMainPanel) : TaskUIState(ui) {
        override fun updateUI() {
            ui ?: return
            ui.startBtn.icon = GIcons.Action.Start.get()
            ui.startBtn.toolTipText = STRINGS.toolTip.startBtn
            ui.stopBtn.isVisible = true
            ui.stopBtn.icon = GIcons.Action.Stop.get()
            ui.deviceCombo.isEnabled = false
        }

        override fun onStartClicked() {
            ui ?: return
            ui.logMainBinding.pauseAll.updateValue(false)
        }
    }
}
