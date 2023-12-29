package me.gegenbauer.catspy.log.ui.panel

import com.github.weisj.darklaf.iconset.AllIcons
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
import me.gegenbauer.catspy.databinding.bind.withName
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
import me.gegenbauer.catspy.view.filter.getOrCreateFilterItem
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
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

abstract class BaseLogPanel(override val contexts: Contexts = Contexts.default) : JPanel(), TabPanel {

    abstract val tag: String
    override val tabName: String = STRINGS.ui.tabLogFile
    override val tabIcon: Icon = GIcons.Tab.FileLog.get(TAB_ICON_SIZE, TAB_ICON_SIZE)

    protected abstract val showProcessToggle: ColorToggleButton

    protected abstract val showProcessCombo: FilterComboBox

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

    private val showLogToggle = ColorToggleButton(GlobalStrings.LOG, STRINGS.toolTip.logToggle)
    protected val showLogCombo = filterComboBox(tooltip = STRINGS.toolTip.logToggle) withName GlobalStrings.LOG

    private val showTagToggle = ColorToggleButton(GlobalStrings.TAG, STRINGS.toolTip.tagToggle)
    protected val showTagCombo = filterComboBox(tooltip = STRINGS.toolTip.tagToggle) withName GlobalStrings.TAG

    private val showTidToggle = ColorToggleButton(GlobalStrings.TID, STRINGS.toolTip.tidToggle)
    protected val showTidCombo = filterComboBox(tooltip = STRINGS.toolTip.tidToggle) withName GlobalStrings.TID

    private val logLevelToggle = ColorToggleButton(STRINGS.ui.logLevel, STRINGS.toolTip.logLevelToggle)
    private val logLevelCombo = readOnlyComboBox(STRINGS.toolTip.logLevelCombo) withName STRINGS.ui.logLevel

    private val boldLogToggle = ColorToggleButton(STRINGS.ui.bold, STRINGS.toolTip.boldToggle)
    protected val boldLogCombo = filterComboBox(tooltip = STRINGS.toolTip.boldToggle)

    private val matchCaseToggle = ColorToggleButton(GlobalStrings.MATCH_CASE, STRINGS.toolTip.caseToggle)
    private val saveFilterToHistoryScheduler = IgnoreFastCallbackScheduler(Dispatchers.UI, 5 * 1000)
    //endregion

    //region toolBarPanel
    private val toolBarPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))

    //region logToolBar
    private val logToolBar = JPanel(FlowLayout(FlowLayout.LEFT)) withName "logToolBar"

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
    private val searchPanel by lazy { SearchPanel() }
    //endregion
    //endregion

    //region splitLogPane
    protected val splitLogWithStatefulPanel = StatefulPanel()
    protected open val fullTableModel = LogTableModel(logViewModel)
    protected open val filteredTableModel = FilteredLogTableModel(logViewModel)
    private val splitLogPane by lazy {
        SplitLogPane(fullTableModel, filteredTableModel).apply {
            onFocusGained = {
                searchPanel.setTargetView(it)
            }
        }
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

            bindProcessComponents(this)

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
        registerSearchStroke()

        splitLogPane.setParent(this)
        filePopupMenu.setParent(this)
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
            searchMatchCase.addObserver { filteredTableModel.searchMatchCase = it == true }
            searchCurrentContent.addObserver { updateSearchFilter(it) }
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
                    resetComboItem(history, content ?: "")
                }
            }
        }
    }

    private fun saveConfiguration() {
        SettingsManager.updateSettings {
            logFontSize = this@BaseLogPanel.logFont.size
            logFontName = this@BaseLogPanel.logFont.name
            logFontStyle = this@BaseLogPanel.logFont.style
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
        logPanel.add(showLogToggle, "0, 0")
        logPanel.add(showLogCombo, "1, 0")
        logPanel.add(showTagToggle, "2, 0")
        logPanel.add(showTagCombo, "3, 0")
        logPanel.add(showProcessToggle, "4, 0")
        logPanel.add(showProcessCombo, "5, 0")
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
        filteredTableModel.searchMatchCase = SettingsManager.settings.searchMatchCaseEnabled
        fullTableModel.searchMatchCase = SettingsManager.settings.searchMatchCaseEnabled

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

        // fix bug: event registered for editor in ComboBox will be removed when ComboBoxUI changed
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
            this@BaseLogPanel,
            event.error.message ?: "Unknown Error",
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
    }

    private fun handleTaskStateEvent(taskState: TaskState) {
        this.taskState = when (taskState) {
            TaskState.IDLE -> {
                TaskIdle(this@BaseLogPanel)
            }

            TaskState.RUNNING -> {
                TaskStarted(this@BaseLogPanel)
            }

            TaskState.PAUSED -> {
                TaskPaused(this@BaseLogPanel)
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
            goToDialog.setLocationRelativeTo(this@BaseLogPanel)
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
    }

    private fun registerComboBoxEditorEvent() {
        showLogCombo.keyListener = keyHandler
        showLogCombo.keyListener = keyHandler
        boldLogCombo.keyListener = keyHandler
        showTagCombo.keyListener = keyHandler
        showProcessCombo.keyListener = keyHandler
        showTidCombo.keyListener = keyHandler
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
                    FileSaveHandler.Builder(this@BaseLogPanel)
                        .onFileSpecified(logViewModel::saveLog)
                        .setDefaultName(logStatus.path.getFileName())
                        .build()
                        .show()
                }
            }
        }
    }

    fun clearAllLogs() {
        logViewModel.clear()
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
                    showProcessCombo.editorComponent,
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
        resetComboItem(logMainBinding.packageFilterHistory, logMainBinding.packageFilterCurrentContent.value ?: "")
        resetComboItem(logMainBinding.tidFilterHistory, logMainBinding.tidFilterCurrentContent.value ?: "")
        resetComboItem(logMainBinding.boldHistory, logMainBinding.boldCurrentContent.value ?: "")
    }

    private fun updateLogFilter() {
        val lastUpdateJob = updateFilterJob
        updateFilterJob = scope.launch {
            lastUpdateJob?.cancelAndJoin()
            delay(10)
            logMainBinding.apply {
                val matchCase = filterMatchCaseEnabled.getValueNonNull()
                LogcatFilter(
                    if (logFilterEnabled.getValueNonNull()) showLogCombo.filterItem.rebuild(matchCase) else EMPTY_ITEM,
                    if (tagFilterEnabled.getValueNonNull()) showTagCombo.filterItem.rebuild(matchCase) else EMPTY_ITEM,
                    currentPidFilter,
                    currentPackageFilter,
                    if (tidFilterEnabled.getValueNonNull()) showTidCombo.filterItem.rebuild(matchCase) else EMPTY_ITEM,
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

    private fun updateSearchFilter(content: String?) {
        filteredTableModel.searchFilterItem = content?.getOrCreateFilterItem() ?: EMPTY_ITEM
        fullTableModel.searchFilterItem = content?.getOrCreateFilterItem() ?: EMPTY_ITEM
    }

    private fun resetComboItem(
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
        private val upBtn = IconBarButton(GIcons.Action.Up.get()) applyTooltip STRINGS.toolTip.searchPrevBtn
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
            searchCombo.setWidth(400)

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
            } else {
                searchCombo.editorComponent.text = ""
                searchCombo.hidePopup()
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
                        SettingsManager.settings.searchMatchCaseEnabled = searchMatchCaseToggle.isSelected
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

    protected open fun afterTaskStateChanged(state: TaskUIState) {}

    abstract class TaskUIState(protected val ui: BaseLogPanel?) {
        abstract fun updateUI()

        open fun onStartClicked() {}
    }

    internal class TaskNone : TaskUIState(null) {
        override fun updateUI() {
        }

        override fun onStartClicked() {
        }
    }

    internal open class TaskStarted(ui: BaseLogPanel) : TaskUIState(ui) {
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

    internal class TaskIdle(ui: BaseLogPanel) : TaskUIState(ui) {
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

    internal class TaskPaused(ui: BaseLogPanel) : TaskUIState(ui) {
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
