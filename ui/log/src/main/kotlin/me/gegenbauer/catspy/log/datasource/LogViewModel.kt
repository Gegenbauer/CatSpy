package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.CPU
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.java.ext.EmptyEvent
import me.gegenbauer.catspy.java.ext.ErrorEvent
import me.gegenbauer.catspy.java.ext.Event
import me.gegenbauer.catspy.java.ext.formatDuration
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.model.LogFilter
import me.gegenbauer.catspy.log.model.LogcatFilter
import me.gegenbauer.catspy.log.model.LogcatItem
import me.gegenbauer.catspy.log.ui.panel.BaseLogMainPanel
import me.gegenbauer.catspy.log.ui.panel.LogPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.writeLinesWithProgress
import me.gegenbauer.catspy.view.filter.FilterCache
import me.gegenbauer.catspy.view.filter.toFilterKey
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.panel.Task
import me.gegenbauer.catspy.view.panel.TaskHandle
import me.gegenbauer.catspy.view.state.ListState
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.system.measureTimeMillis


open class LogViewModel(override val contexts: Contexts = Contexts.default) :
    Context, LogProducerManager<LogcatItem>, LogFilterable {
    override val eventFlow: StateFlow<Event>
        get() = _eventFlow

    override val fullLogItemsFlow: StateFlow<List<LogcatItem>>
        get() = fullLogRepo.logItemsFlow

    override val filteredLogItemsFlow: StateFlow<List<LogcatItem>>
        get() = filteredLogRepo.logItemsFlow

    override val fullLogListState: StateFlow<ListState>
        get() = fullLogRepo.listState

    override val filteredLogListState: StateFlow<ListState>
        get() = filteredLogRepo.listState


    override var logcatFilter: LogcatFilter = LogcatFilter.EMPTY_FILTER

    override var fullMode: Boolean = false

    override var bookmarkMode: Boolean = false

    override var fullTableSelectedRows: List<Int> = emptyList()

    override var filteredTableSelectedRows: List<Int> = emptyList()

    override val processValueExtractor: (LogcatItem) -> String = {
        if (logProducer is DeviceLogProducer) {
            it.packageName
        } else {
            it.pid
        }
    }

    override val tempLogFile: File
        get() = logProducer.tempFile

    override val device: String
        get() = (logProducer as? DeviceLogProducer)?.device ?: ""

    private var logProducer: LogProducer = EmptyLogProducer

    private val _eventFlow = MutableStateFlow<Event>(EmptyEvent)
    private val fullLogRepo = FullLogRepo()
    private val filteredLogRepo = FilteredLogRepo()

    private val scope = ViewModelScope()
    private val produceCompanyJobs = mutableListOf<Job>()
    private var updateFilterCompanyJobs = mutableListOf<Job>()

    private lateinit var bookmarkManager: BookmarkManager

    private val fullModeObserver = Observer<Boolean> {
        fullMode = it ?: false
        updateFilter(logcatFilter, true)
    }
    private val bookmarkModeObserver = Observer<Boolean> {
        bookmarkMode = it ?: false
        updateFilter(logcatFilter, true)
    }
    private val updateFullLogTaskSuspender = CoroutineSuspender("updateFullLogTaskSuspender")
    private val updateFilteredLogTaskSuspender = CoroutineSuspender("updateFilteredLogTaskSuspender")
    private val processFetcher = AndroidProcessFetcher("")
    private val globalStatus = ServiceManager.getContextService(StatusPanel::class.java)

    private val updatingFullLogTriggerCount = MutableStateFlow(0)
    private val updatingFilteredLogTriggerCount = MutableStateFlow(0)
    private val updateTriggerCountLock = ReentrantReadWriteLock()
    private var notifyDisplayedLogTask: Job? = null
    private var hasPendingUpdateFilterTask = false
    private var updateFilterTask: Job? = null

    private var paused = false

    private fun ensureUpdateLogTaskRunning() {
        if (notifyDisplayedLogTask.isActive) {
            return
        }
        notifyDisplayedLogTask = scope.launch {
            while (isActive) {
                updateFilteredLogTaskSuspender.checkSuspend()
                delay(UPDATE_LOG_ITEMS_DELAY)
                fullLogRepo.submitLogItems()
                filteredLogRepo.submitLogItems()
            }
        }
        scope.launch {
            updatingFullLogTriggerCount.collect { count ->
                Log.d(TAG, "[onFullLogTriggerCountChanged] count=$count")
                if (count == 0) {
                    fullLogRepo.submitLogItems()
                    updateFullLogTaskSuspender.enable()
                } else {
                    updateFullLogTaskSuspender.disable()
                }
            }
        }
        scope.launch {
            updatingFilteredLogTriggerCount.collect { count ->
                Log.d(TAG, "[onFilteredLogTriggerCountChanged] count=$count")
                if (count == 0) {
                    filteredLogRepo.submitLogItems()
                    updateFilteredLogTaskSuspender.enable()
                } else {
                    updateFilteredLogTaskSuspender.disable()
                }
            }
        }
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        getBindings()?.fullMode?.addObserver(fullModeObserver)
        getBindings()?.bookmarkMode?.addObserver(bookmarkModeObserver)
        val logTabPanel = contexts.getContext(BaseLogMainPanel::class.java)
        logTabPanel?.let {
            bookmarkManager = ServiceManager.getContextService(logTabPanel, BookmarkManager::class.java)
        }
    }

    private fun getBindings(): LogPanel.LogPanelBinding? {
        return contexts.getContext(LogPanel::class.java)?.binding
    }

    override fun startProduceDeviceLog(device: String) {
        processFetcher.device = device
        processFetcher.start()
        startProduce(DeviceLogProducer(device, processFetcher))
    }

    override fun startProduceFileLog(file: String) {
        startProduce(FileLogProducer(file))
    }

    private fun startProduce(logProducer: LogProducer) {
        setPaused(false)
        ensureUpdateLogTaskRunning()
        playLoadingAnimation(fullLogRepo)
        scope.launch {

            Log.d(TAG, "[startProduce] cancel previous jobs")
            produceCompanyJobs.toList().forEach { it.cancelAndJoin() }
            produceCompanyJobs.clear()

            this@LogViewModel.logProducer = logProducer

            Log.d(TAG, "[startProduce]")
            updateTriggerCountLock.write {
                updatingFullLogTriggerCount.value += 1
                updatingFilteredLogTriggerCount.value += 1
            }

            val observeLogProducerStateTask = scope.launch {
                observeLogProducerState(logProducer)
            }

            if (logProducer is DeviceLogProducer) {
                logProducer.moveToState(LogProducer.State.RUNNING)
                delay(1000)
            }

            val realStartProducerTask = scope.launch { startProduceInternal() }

            produceCompanyJobs.add(observeLogProducerStateTask)
            produceCompanyJobs.add(realStartProducerTask)

            realStartProducerTask.invokeOnCompletion {
                Log.d(TAG, "[startProduce] realStartProducerTask completed")
                _eventFlow.value = TaskState.IDLE
                updateTriggerCountLock.write {
                    updatingFullLogTriggerCount.value -= 1
                    updatingFilteredLogTriggerCount.value -= 1
                }
                observeLogProducerStateTask.cancel()
            }
        }
    }

    private fun playLoadingAnimation(logRepo: LogRepo) {
        scope.launch {
            logRepo.onLoadingStart()
            delay(800)
            logRepo.onLoadingEnd()
        }
    }

    private suspend fun observeLogProducerState(producer: LogProducer) {
        Log.d(TAG, "[observeLogProducerState]")
        producer.state.collect { state ->
            Log.d(TAG, "[observeLogProducerState] state=$state")
            _eventFlow.value = state.toTaskState()
        }
    }

    private fun LogProducer.State.toTaskState(): TaskState {
        return when (this) {
            LogProducer.State.RUNNING -> TaskState.RUNNING
            LogProducer.State.PAUSED -> TaskState.PAUSED
            LogProducer.State.COMPLETE -> TaskState.IDLE
            else -> TaskState.IDLE
        }
    }

    override fun onLogProduceError(error: Throwable) {
        Log.e(TAG, "[onLogProduceError]", error)
        _eventFlow.value = ErrorEvent(error)
    }

    override fun onLogItemReceived(logItem: LogcatItem) {
        fullLogRepo.onReceiveLogItem(logItem, getLogFilter())
        filteredLogRepo.onReceiveLogItem(logItem, getLogFilter())
    }

    private suspend fun startProduceInternal() {
        Log.d(TAG, "[startProduceInternal] start")
        val cost = measureTimeMillis {
            logProducer.start().collect { result ->
                val exception = result.exceptionOrNull()
                val item = result.getOrNull()

                exception?.let {
                    onLogProduceError(it)
                    return@collect
                }

                item?.let { onLogItemReceived(it) }
            }
        }
        Log.d(TAG, "[startProduceInternal] end, cost=${formatDuration(cost)}")
    }

    override fun updateFilter(filter: LogcatFilter, force: Boolean) {
        if (!force && filter == logcatFilter) {
            return
        }
        hasPendingUpdateFilterTask = false
        logcatFilter = filter
        val composedFilter = getLogFilter(filter)
        playLoadingAnimation(filteredLogRepo)
        val lastTask = updateFilterTask
        updateFilterTask = scope.launch {
            lastTask?.cancelAndJoin()

            Log.d(TAG, "[updateFilter] start")
            updateFilterCompanyJobs.toList().forEach { it.cancelAndJoin() }
            updateFilterCompanyJobs.clear()
            filteredLogRepo.clear()

            updateTriggerCountLock.write {
                updatingFilteredLogTriggerCount.value += 1
            }

            val producerRunning = AtomicBoolean(false)
            producerRunning.set(logProducer.isRunning)

            val realUpdateFilterJob = scope.launch {
                logProducer.pause()
                updateFilterInternal(composedFilter)
            }

            updateFilterCompanyJobs.add(realUpdateFilterJob)

            realUpdateFilterJob.invokeOnCompletion {
                Log.d(TAG, "[updateFilter] realUpdateFilterJob completed")
                updateTriggerCountLock.write {
                    updatingFilteredLogTriggerCount.value -= 1
                }
                updateFilterCompanyJobs.remove(realUpdateFilterJob)
                logProducer.takeIf { producerRunning.get() }?.resume()
            }
        }
    }

    override fun updateFilter(filter: LogcatFilter) {
        updateFilter(filter, false)
    }

    /**
     * save full log to file
     */
    suspend fun saveLog(targetFile: File): Result<File?> {
        return withContext(Dispatchers.GIO) {
            val logs = fullLogRepo.readLogItems { it.toList() }
            Log.d(TAG, "[saveLog] targetLogFile=${targetFile.absolutePath}, logSize=${logs.size}")
            val taskName = STRINGS.ui.exportFileTaskTitle.get(targetFile.absolutePath)
            val task = Task(taskName, object : TaskHandle {
                override fun cancel() {
                    coroutineContext.job.cancel()
                }
            })

            globalStatus.addTask(task)

            task.notifyTaskStarted()
            runCatching {
                writeLinesWithProgress(targetFile, logs.size, { index: Int -> logs[index].toFileLogLine() }) {
                    task.notifyProgressChanged(it)
                }
                task.notifyTaskFinished()
                Log.d(TAG, "[saveLog] task finished")
                Result.success(targetFile)
            }.onFailure {
                if (it is CancellationException) {
                    Log.d(TAG, "[saveLog] task canceled")
                    task.notifyTaskCancelled()
                } else {
                    Log.e(TAG, "[saveLog] task failed", it)
                    task.notifyTaskFailed(it)
                }
                Result.failure<File>(it)
            }.getOrDefault(Result.failure(Exception(STRINGS.ui.unknownError)))
        }
    }

    private suspend fun updateFilterInternal(filter: LogFilter<LogcatItem>) {
        withContext(Dispatchers.CPU) {
            fullLogRepo.readLogItems {
                it.forEach { item ->
                    ensureActive()
                    filteredLogRepo.onReceiveLogItem(item, filter)
                }
            }
        }
    }

    protected open fun getLogFilter(filter: LogcatFilter = logcatFilter): LogFilter<LogcatItem> {
        return LogFilter {
            return@LogFilter when {
                fullMode -> true
                bookmarkMode -> bookmarkManager.isBookmark(it.num)
                filter.match(it) -> true
                else -> false
            }
        }
    }

    suspend fun preCacheFilters() {
        withContext(Dispatchers.CPU) {
            val filterCache = ServiceManager.getContextService(FilterCache::class.java)
            currentSettings.logSettings.apply {
                (filterHistory.logFilterHistory + filterHistory.tagFilterHistory + search.searchHistory + filterHistory.highlightHistory).forEach {
                    filterCache[it.toFilterKey(true)]
                    filterCache[it.toFilterKey(false)]
                }
            }
        }
    }

    override fun pause() {
        Log.d(TAG, "[pause]")
        if (updateFilterCompanyJobs.any { it.isActive }) {
            Log.d(TAG, "[pause] has active updateFilterCompanyJobs")
            hasPendingUpdateFilterTask = true
        }
        updateFilterCompanyJobs.forEach(Job::cancel)
        logProducer.pause()
        processFetcher.pause()
    }

    override fun resume() {
        Log.d(TAG, "[resume]")
        logProducer.resume()
        processFetcher.resume()
        if (hasPendingUpdateFilterTask) {
            Log.d(TAG, "[resume] restart pending updateFilterCompanyJobs")
            updateFilter(logcatFilter, true)
        }
    }

    override fun setPaused(paused: Boolean) {
        Log.d(TAG, "[setPaused] pause=$paused")
        this.paused = paused
        if (paused) {
            pause()
        } else {
            resume()
        }
    }

    override fun isPaused(): Boolean {
        return paused
    }

    override fun clear() {
        fullLogRepo.clear()
        filteredLogRepo.clear()
    }

    override fun cancel() {
        setPaused(true)
        logProducer.cancel()
        processFetcher.cancel()
    }

    override fun destroy() {
        cancel()
        scope.cancel()
        updateFullLogTaskSuspender.disable()
        updateFilteredLogTaskSuspender.disable()
        getBindings()?.fullMode?.removeObserver(fullModeObserver)
        getBindings()?.bookmarkMode?.removeObserver(bookmarkModeObserver)
    }

    private interface LogRepo {
        val logItemsFlow: StateFlow<List<LogcatItem>>

        val listState: StateFlow<ListState>

        val selectedRow: Int

        val logItems: MutableList<LogcatItem>

        fun <T> readLogItems(readAction: (List<LogcatItem>) -> T): T

        fun <T> writeLogItems(writeAction: (MutableList<LogcatItem>) -> T): T

        fun clear()

        fun submitLogItems(force: Boolean)

        fun submitLogItems()

        fun onLoadingStart()

        fun onLoadingEnd()

        fun onReceiveLogItem(logItem: LogcatItem, logFilter: LogFilter<LogcatItem>)
    }

    abstract class BaseLogRepo : LogRepo {
        override val logItemsFlow: StateFlow<List<LogcatItem>>
            get() = _logItemsFlow
        override val listState: StateFlow<ListState>
            get() = _listState
        override val selectedRow: Int = 0
        override val logItems: MutableList<LogcatItem> = mutableListOf()

        abstract val name: String

        private val _logItemsFlow = MutableStateFlow(emptyList<LogcatItem>())
        private val _listState = MutableStateFlow(ListState.EMPTY)
        private val logItemsAccessLock = ReentrantReadWriteLock()

        override fun <T> readLogItems(readAction: (List<LogcatItem>) -> T): T {
            return logItemsAccessLock.read { readAction(logItems.toList()) }
        }

        override fun <T> writeLogItems(writeAction: (MutableList<LogcatItem>) -> T): T {
            return logItemsAccessLock.write { writeAction(logItems) }
        }

        override fun clear() {
            logItemsAccessLock.write { logItems.clear() }
            _logItemsFlow.value = emptyList()
            _listState.value = ListState.EMPTY
        }

        override fun submitLogItems(force: Boolean) {
            readLogItems {
                if (logItemsChanged() || force) {
                    _logItemsFlow.value = it
                    if (it.isNotEmpty()) {
                        onLoadingEnd()
                    }
                }
            }
        }

        override fun submitLogItems() {
            submitLogItems(false)
        }

        override fun onLoadingStart() {
            _listState.value = ListState.LOADING
        }

        override fun onLoadingEnd() {
            _listState.value = ListState.NORMAL
        }

        private fun logItemsChanged(): Boolean {
            return readLogItems {
                _logItemsFlow.value.count() != it.count()
                        || _logItemsFlow.value.lastOrNull()?.num != it.lastOrNull()?.num
            }
        }
    }

    private class FilteredLogRepo : BaseLogRepo() {

        override val name: String = "FilteredLogRepo"

        override fun onReceiveLogItem(logItem: LogcatItem, logFilter: LogFilter<LogcatItem>) {
            writeLogItems {
                if (logFilter.match(logItem)) {
                    it.add(logItem)
                }
            }
        }

    }

    private class FullLogRepo : BaseLogRepo() {

        override val name: String = "FullLogRepo"

        override fun onReceiveLogItem(logItem: LogcatItem, logFilter: LogFilter<LogcatItem>) {
            writeLogItems { it.add(logItem) }
        }

    }

    companion object {
        private const val TAG = "LogViewModel"
        private const val UPDATE_LOG_ITEMS_DELAY = 100L

        private val Job?.isActive: Boolean
            get() = this?.isActive == true
    }
}