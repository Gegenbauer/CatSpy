package me.gegenbauer.catspy.log.ui.tab

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.ErrorEvent
import me.gegenbauer.catspy.configuration.Rotation
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.customProperty
import me.gegenbauer.catspy.databinding.property.support.dividerProperty
import me.gegenbauer.catspy.file.fileName
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.log.datasource.LogViewModel
import me.gegenbauer.catspy.log.datasource.TaskState
import me.gegenbauer.catspy.log.event.FullLogWindowModeChangedEvent
import me.gegenbauer.catspy.log.filter.FilterProperty
import me.gegenbauer.catspy.log.filter.FilterRecord
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.log.ui.customize.CenteredDualDirectionPanel
import me.gegenbauer.catspy.log.ui.search.ISearchPanel
import me.gegenbauer.catspy.log.ui.table.FilteredLogTableModel
import me.gegenbauer.catspy.log.ui.table.GoToDialog
import me.gegenbauer.catspy.log.ui.table.LogDetailDialog
import me.gegenbauer.catspy.log.ui.table.LogTableDialog
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.log.ui.table.SplitLogPane
import me.gegenbauer.catspy.log.ui.table.nextRotation
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.event.EventManager
import me.gegenbauer.catspy.utils.ui.Key
import me.gegenbauer.catspy.utils.ui.findFrameFromParent
import me.gegenbauer.catspy.utils.ui.registerStroke
import me.gegenbauer.catspy.view.button.IconBarButton
import me.gegenbauer.catspy.view.dialog.FileSaveHandler
import me.gegenbauer.catspy.view.panel.StatusBar
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.panel.VerticalFlexibleWidthLayout
import me.gegenbauer.catspy.view.state.ListState
import me.gegenbauer.catspy.view.state.StatefulPanel
import me.gegenbauer.catspy.view.tab.BaseTabPanel
import raven.toast.Notifications
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel

abstract class BaseLogMainPanel : BaseTabPanel() {

    private val topPanel = JPanel()
    protected val logToolBar = CenteredDualDirectionPanel(4)
    private val resetFilterBtn = IconBarButton(GIcons.Action.ResetFilter.get(), STRINGS.toolTip.resetFilters)
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
    protected val saveBtn = IconBarButton(GIcons.Action.SaveFile.get(), STRINGS.toolTip.saveBtn)
    private val rotateLogPanelBtn = IconBarButton(GIcons.Action.Rotate.get(), STRINGS.toolTip.rotationBtn)
    private val clearViewsBtn = IconBarButton(GIcons.Action.Clear.get(), STRINGS.toolTip.clearBtn)

    protected val splitLogWithStatefulPanel = StatefulPanel()
    protected open val emptyStateContent: JComponent = JPanel()
    private val emptyStateContentContainer = JPanel()
    protected val splitLogPane by lazy {
        SplitLogPane(fullTableModel, filteredTableModel)
    }
    private val actionHandler = ActionHandler()

    protected var logStatus: StatusBar.LogStatus = StatusBar.LogStatus.NONE
        set(value) {
            field = value
            afterLogStatusChanged(value)
        }
    private val statusBar = ServiceManager.getContextService(StatusPanel::class.java)

    protected val logMainBinding by lazy { ServiceManager.getContextService(this, LogMainBinding::class.java) }
    protected val eventManager by lazy { ServiceManager.getContextService(this, EventManager::class.java) }
    protected var taskState: TaskUIState = TaskNone()
        set(value) {
            field = value
            value.updateUI()
            afterTaskStateChanged(value)
        }
    protected val logViewModel = LogViewModel()
    protected open val fullTableModel = LogTableModel(logViewModel)
    protected open val filteredTableModel = FilteredLogTableModel(logViewModel)
    protected val logConf = LogConfiguration()

    protected val isLogTableEmpty: Boolean
        get() = logViewModel.fullLogObservables.itemsFlow.value.isEmpty()

    protected val scope = MainScope()

    private var updateSearchFilterJob: Job? = null

    override fun onSetup(bundle: Bundle?) {
        val logMetaData =
            bundle?.get<LogMetadata>(LogMetadata.KEY) ?: throw IllegalArgumentException("LogMetaData not found")
        logConf.setLogMetadata(logMetaData)
        onInitialMetadataAcquired(logMetaData)

        createUI()

        taskState = TaskIdle(this)

        registerEvent()

        bind(logMainBinding)
    }

    protected open fun onInitialMetadataAcquired(metadata: LogMetadata) {
        // no-op
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        contexts.putContext(logConf)

        ServiceManager.getContextService(this, BookmarkManager::class.java)

        logConf.setParent(this)
        splitLogPane.setParent(this)
    }

    protected open fun createUI() {
        logToolBar.addRight(logConf.getLogBufferSelectPanel())
        getCustomToolbarComponents().forEach { logToolBar.addRight(it) }
        logToolBar.addRight(clearViewsBtn)
        logToolBar.addRight(rotateLogPanelBtn)
        logToolBar.addRight(saveBtn)
        logToolBar.addRight(stopBtn)
        logToolBar.addRight(startBtn)
        logToolBar.addRight(resetFilterBtn)
        logToolBar.border = BorderFactory.createEmptyBorder(0, 0, 0, 20)

        logConf.getLogBufferSelectPanel().isVisible = false
        logToolBar.isVisible = logConf.isPreviewMode.not()
        logConf.getFavoriteFilterPanel().isVisible = logConf.isPreviewMode.not()

        saveBtn.isVisible = false

        topPanel.border = BorderFactory.createEmptyBorder(4, 6, 4, 0)
        topPanel.layout = VerticalFlexibleWidthLayout(4)
        topPanel.add(logToolBar)
        topPanel.add(logConf.filterPanel)
        topPanel.add(logConf.getFavoriteFilterPanel())
        topPanel.add(logConf.getSearchPanel() as JPanel)

        layout = BorderLayout()

        splitLogPane.fullLogPanel.createTableBar()
        splitLogPane.filteredLogPanel.createTableBar()

        splitLogPane.isOneTouchExpandable = false

        splitLogWithStatefulPanel.setContent(splitLogPane)
        splitLogWithStatefulPanel.setEmptyContent(emptyStateContentContainer)

        add(topPanel, BorderLayout.NORTH)
        add(splitLogWithStatefulPanel, BorderLayout.CENTER)

        emptyStateContentContainer.layout = TableLayout(
            doubleArrayOf(0.25, FILL, 0.25),
            doubleArrayOf(0.3, PREFERRED, 0.25)
        )
        emptyStateContentContainer.add(emptyStateContent, "1,1")
    }

    protected open fun getCustomToolbarComponents(): List<Component> {
        return emptyList()
    }

    protected open fun registerEvent() {
        registerStrokes()

        registerComboBoxEditorEvent()

        configureLogTablePopupActions()

        configureSearchPanel()

        resetFilterBtn.addActionListener(actionHandler)
        startBtn.addActionListener(actionHandler)
        stopBtn.addActionListener(actionHandler)
        clearViewsBtn.addActionListener(actionHandler)
        rotateLogPanelBtn.addActionListener(actionHandler)
        saveBtn.addActionListener(actionHandler)

        observeFullLogListState()
        observeLogEvent()
        observeOtherEvent()
        observeViewModelValue()
    }

    protected open fun registerStrokes() {
        registerStroke(Key.C_PAGE_DOWN, "Log Move To Last Row") {
            splitLogPane.filteredLogPanel.moveToLastRow()
            splitLogPane.fullLogPanel.moveToLastRow()
        }
        registerStroke(Key.C_PAGE_UP, "Log Move To First Row") {
            splitLogPane.filteredLogPanel.moveToFirstRow()
            splitLogPane.fullLogPanel.moveToFirstRow()
        }
        registerStroke(Key.C_G, "Go To Target Log Line") {
            val goToDialog = GoToDialog(findFrameFromParent())
            goToDialog.setLocationRelativeTo(this)
            goToDialog.isVisible = true
            if (goToDialog.line > 0) {
                GLog.d(tag, "[KeyEventDispatcher] Cancel Goto Line ${goToDialog.line}")
                goToLine(goToDialog.line)
            } else {
                GLog.d(tag, "[KeyEventDispatcher] Cancel Goto Line")
            }
        }
        registerStroke(Key.C_K, "Stop All Log Task") { stopAll() }
        registerStroke(Key.C_P, "Start Parsing Log") { startBtn.doClick() }
        registerStroke(Key.C_DELETE, "Clear All Logs") { clearAllLogs() }
        registerStroke(Key.ESCAPE, "Close Search Panel") { logConf.getSearchPanel().isVisible = false }
        registerStroke(Key.C_F, "Open Search Panel") {
            logConf.getSearchPanel().isVisible = true
            logConf.requestFocusOnSearchEditor()
        }
        registerStroke(Key.F3, "Move To Previous Search Result") {
            logConf.moveToPrevSearchResult()
        }
        registerStroke(Key.F4, "Move To Next Search Result") {
            logConf.moveToNextSearchResult()
        }
        registerStroke(Key.C_S, "Save Log") { saveLog() }
    }

    private fun goToLine(lineNumber: Int) {
        GLog.d(tag, "[goToLine] $lineNumber")

        lineNumber.takeIf { it > 0 } ?: return

        splitLogPane.filteredLogPanel.goToRowIndex(filteredTableModel.getRowIndexInAllPages(lineNumber))
        splitLogPane.fullLogPanel.goToRowIndex(fullTableModel.getRowIndexInAllPages(lineNumber))
    }

    private fun registerComboBoxEditorEvent() {
        logConf.registerKeyEvents()
    }

    protected open fun configureLogTablePopupActions() {
        splitLogPane.filteredLogPanel.table.setTablePopupActionProvider {
            getTablePopupItems()
        }
        splitLogPane.fullLogPanel.table.setTablePopupActionProvider {
            getTablePopupItems()
        }

        splitLogPane.filteredLogPanel.table.setLogDetailPopupActionsProvider {
            getLogDetailPopupActions()
        }
        splitLogPane.fullLogPanel.table.setLogDetailPopupActionsProvider {
            getLogDetailPopupActions()
        }
    }

    protected open fun getTablePopupItems(): List<Pair<String, () -> Unit>> {
        return listOf(
            STRINGS.ui.start to ::onStartClicked,
            STRINGS.ui.stop to ::stopAll,
            STRINGS.ui.clear to ::clearAllLogs,
        )
    }

    protected open fun getLogDetailPopupActions(): List<LogDetailDialog.PopupAction> {
        val actions = mutableListOf<LogDetailDialog.PopupAction>()
        val messageColumn = logConf.logMetaData.columns.filterIsInstance<Column.MessageColumn>().firstOrNull()
        if (messageColumn != null) {
            actions.add(LogDetailDialog.PopupAction(STRINGS.ui.addInclude) { selectedText ->
                addContentToFilter(logConf.getFilterProperty(messageColumn), selectedText)
            })
            actions.add(LogDetailDialog.PopupAction(STRINGS.ui.addExclude) { selectedText ->
                addContentToFilter(logConf.getFilterProperty(messageColumn), "-$selectedText")
            })
        }
        val searchFilterProperty = logConf.getSearchContentProperty()
        actions.add(LogDetailDialog.PopupAction(STRINGS.ui.addSearch) { selectedText ->
            logConf.getSearchPanel().isVisible = true
            addContentToFilter(searchFilterProperty, selectedText)
        })
        actions.add(LogDetailDialog.PopupAction(STRINGS.ui.setSearch) { selectedText ->
            logConf.getSearchPanel().isVisible = true
            searchFilterProperty.content.updateValue(selectedText)
        })
        return actions
    }

    private fun addContentToFilter(filterProperty: FilterProperty, content: String) {
        val currentContent = filterProperty.content.value ?: EMPTY_STRING
        if (currentContent.isEmpty()) {
            filterProperty.content.updateValue(content)
        } else {
            filterProperty.content.updateValue("$currentContent|$content")
        }
    }

    private fun configureSearchPanel() {
        val searchPanel = logConf.getSearchPanel()
        searchPanel.setOnSearchRequestReceivedListener(object : ISearchPanel.SearchRequestReceivedListener {
            override fun moveToNextSearchResult(isFilteredLog: Boolean) {
                scope.launch {
                    val result = getTableModel(isFilteredLog).moveToNextSearchResult()
                    if (result.isNotEmpty()) {
                        showNotification(result)
                    }
                }
            }

            override fun moveToPrevSearchResult(isFilteredLog: Boolean) {
                scope.launch {
                    val result = getTableModel(isFilteredLog).moveToPreviousSearchResult()
                    if (result.isNotEmpty()) {
                        showNotification(result)
                    }
                }
            }

            private fun getTableModel(isFilteredLog: Boolean): LogTableModel {
                return if (isFilteredLog) {
                    filteredTableModel
                } else {
                    fullTableModel
                }
            }

            private fun showNotification(result: String) {
                val notifications = Notifications.getInstance()
                notifications.clearHold()
                notifications.setJFrame(findFrameFromParent())
                notifications.show(Notifications.Type.INFO, Notifications.Location.BOTTOM_CENTER, 1000, result)
            }
        })
    }

    private fun observeFullLogListState() {
        scope.launch {
            logViewModel.fullLogObservables.listState.collect(::setLogPanelState)
        }
    }

    private fun observeLogEvent() {
        scope.launch {
            logViewModel.eventFlow.collect {
                Log.d(tag, "[observeEventFlow] event: $it")
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
            this,
            event.error.message ?: STRINGS.ui.unknownError,
            STRINGS.ui.error,
            JOptionPane.ERROR_MESSAGE
        )
    }

    private fun handleTaskStateEvent(taskState: TaskState) {
        this.taskState = when (taskState) {
            TaskState.IDLE -> {
                TaskIdle(this)
            }

            TaskState.RUNNING -> {
                TaskStarted(this)
            }

            TaskState.PAUSED -> {
                TaskPaused(this)
            }
        }
    }

    private fun observeOtherEvent() {
        scope.launch {
            eventManager.collect { event ->
                when (event) {
                    is FullLogWindowModeChangedEvent -> {
                        if (event.enabled) {
                            showLogPanelInWindow()
                        } else {
                            splitLogPane.attachFullLogPanel()
                        }
                    }
                }
            }
        }
    }

    private fun observeViewModelValue() {
        logMainBinding.apply {
            pauseAll.addObserver {
                logViewModel.setPaused(it == true)
            }
            logConf.addFilterPropertyObserver {
                updateLogFilter()
            }
            logConf.addSearchContentObserver {
                updateSearchFilter()
            }
        }
    }

    protected fun updateLogFilter() {
        scope.launch {
            logViewModel.updateFilter(logConf.generateLogFilter())
        }
    }

    private fun updateSearchFilter() {
        val lastUpdateJob = updateSearchFilterJob
        updateSearchFilterJob = scope.launch {
            lastUpdateJob?.cancelAndJoin()
            val searchItem = logConf.getSearchFilterItem()
            filteredTableModel.searchFilterItem = searchItem
            fullTableModel.searchFilterItem = searchItem
        }
    }

    protected open fun bind(binding: LogMainBinding) {
        binding.apply {
            customProperty(splitLogPane, "rotation", Rotation.ROTATION_LEFT_RIGHT) bindDual rotation

            dividerProperty(splitLogPane) bindDual splitPanelDividerLocation
        }
    }

    private fun updateTitleBar(statusMethod: String) {
        val frame = findFrameFromParent<JFrame>()
        frame.title = when (statusMethod.trim()) {
            STRINGS.ui.open -> {
                val path: Path = Paths.get(logStatus.path)
                path.fileName.toString()
            }

            STRINGS.ui.adb, STRINGS.ui.cmd, "${STRINGS.ui.adb} ${STRINGS.ui.stop}",
            "${STRINGS.ui.cmd} ${STRINGS.ui.stop}" -> {
                (logMainBinding.currentDevice.value ?: EMPTY_STRING).ifEmpty { GlobalProperties.APP_NAME }
            }

            else -> {
                GlobalProperties.APP_NAME
            }
        }
    }

    private inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                resetFilterBtn -> {
                    logConf.applyFilterRecord(FilterRecord.EMPTY)
                }

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

    protected fun stopAll() {
        logViewModel.cancel()
    }

    protected open fun clearAllLogs() {
        logViewModel.clear()
        if (logViewModel.isActive().not()) {
            setLogPanelState(ListState.EMPTY)
        }
    }

    private fun setLogPanelState(state: ListState) {
        splitLogWithStatefulPanel.listState = state
    }

    private fun saveLog() {
        FileSaveHandler.Builder(this)
            .onFileSpecified(logViewModel::saveLog)
            .setDefaultName(logStatus.path.fileName)
            .build()
            .show()
    }

    private fun showLogPanelInWindow() {
        splitLogPane.detachFullLogPanel()
        val logTableDialog = LogTableDialog(splitLogPane.fullLogPanel) {
            splitLogPane.attachFullLogPanel()
        }
        logTableDialog.isVisible = true
    }

    override fun onTabSelected() {
        if (logViewModel.isPaused().not()) {
            logViewModel.resume()
        }
        statusBar.logStatus = logStatus
        updateTitleBar(logStatus.status)
    }

    override fun onTabUnselected() {
        if (logViewModel.isPaused().not()) {
            logViewModel.pause()
        }
        statusBar.logStatus = StatusBar.LogStatus.NONE
        updateTitleBar(StatusBar.LogStatus.NONE.status)
    }

    override fun destroy() {
        super.destroy()
        stopAll()
        scope.cancel()
        logViewModel.destroy()
        clearAllLogs()
        splitLogPane.destroy()
    }

    override fun getTabContent(): JComponent {
        return this
    }

    protected open fun afterLogStatusChanged(status: StatusBar.LogStatus) {
        if (isTabSelected) {
            updateTitleBar(status.status)
            statusBar.logStatus = status
        }
    }

    protected open fun afterTaskStateChanged(state: TaskUIState) {
        // no-op
    }

    protected open fun onStartClicked() {
        // no-op
    }

    protected abstract class TaskUIState(protected val ui: BaseLogMainPanel?) {
        abstract fun updateUI()

        open fun onStartClicked() {
            // no-op
        }
    }

    private class TaskNone : TaskUIState(null) {
        override fun updateUI() {
            // no-op
        }

        override fun onStartClicked() {
            // no-op
        }
    }

    protected open class TaskStarted(ui: BaseLogMainPanel) : TaskUIState(ui) {
        override fun updateUI() {
            ui ?: return
            ui.startBtn.icon = GIcons.Action.Pause.get()
            ui.startBtn.isEnabled = true
            ui.startBtn.toolTipText = STRINGS.toolTip.pauseBtn
            ui.stopBtn.isEnabled = true
            ui.stopBtn.icon = GIcons.Action.Stop.get()
        }

        override fun onStartClicked() {
            ui ?: return
            ui.logMainBinding.pauseAll.updateValue(true)
        }
    }

    protected class TaskIdle(ui: BaseLogMainPanel) : TaskUIState(ui) {
        override fun updateUI() {
            ui ?: return
            ui.startBtn.isEnabled = ui.logMainBinding.connectedDevices.value.isNullOrEmpty().not()
            ui.startBtn.icon = GIcons.Action.Start.get()
            ui.startBtn.toolTipText = STRINGS.toolTip.startBtn
            ui.logMainBinding.pauseAll.updateValue(false)
            ui.stopBtn.isEnabled = false
        }

        override fun onStartClicked() {
            ui ?: return
            ui.onStartClicked()
        }
    }

    protected class TaskPaused(ui: BaseLogMainPanel) : TaskUIState(ui) {
        override fun updateUI() {
            ui ?: return
            ui.startBtn.icon = GIcons.Action.Start.get()
            ui.startBtn.toolTipText = STRINGS.toolTip.startBtn
            ui.stopBtn.isVisible = true
            ui.stopBtn.icon = GIcons.Action.Stop.get()
        }

        override fun onStartClicked() {
            ui ?: return
            ui.logMainBinding.pauseAll.updateValue(false)
        }
    }
}
