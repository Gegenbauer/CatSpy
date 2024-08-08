package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ViewModelScope
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
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.log.ui.table.LogPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.file.writeLinesWithProgress
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

class LogViewModel(
    override val contexts: Contexts = Contexts.default
) : Context, LogProducerManager, LogFilterable {
    override val eventFlow: SharedFlow<Event>
        get() = _eventFlow

    override val fullLogObservables: LogProducerManager.LogObservables
        get() = fullLogRepo.logObservables

    override val filteredLogObservables: LogProducerManager.LogObservables
        get() = filteredLogRepo.logObservables

    private val logParser: LogParser
        get() = logConf.logMetaData.parser
    private val logConf: LogConfiguration
        get() = contexts.getContext(LogConfiguration::class.java)
            ?: throw IllegalStateException("must put LogConfiguration in context before using it")

    override var logFilter: LogFilter = LogFilter.default

    override var fullMode: Boolean = false

    override var bookmarkMode: Boolean = false

    override var fullTableSelectedRows: List<Int> = emptyList()

    override var filteredTableSelectedRows: List<Int> = emptyList()

    override val tempLogFile: File
        get() = logProducer.tempFile

    override val device: String
        get() = (logProducer as? DeviceLogProducer)?.device ?: ""

    private var logProducer: LogProducer = EmptyLogProducer

    private val _eventFlow = MutableSharedFlow<Event>(
        extraBufferCapacity = 30,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val fullLogRepo = FullLogRepo()
    private val filteredLogRepo = FilteredLogRepo()

    private val scope = ViewModelScope()

    private lateinit var bookmarkManager: BookmarkManager

    private val fullModeObserver = Observer<Boolean> {
        fullMode = it ?: false
        updateFilter(logFilter, true)
    }
    private val bookmarkModeObserver = Observer<Boolean> {
        bookmarkMode = it ?: false
        updateFilter(logFilter, true)
    }
    private val updateFilteredLogTaskSuspender = CoroutineSuspender("updateFilteredLogTaskSuspender")
    private val globalStatus = ServiceManager.getContextService(StatusPanel::class.java)

    private val logUpdater = LogUpdater(fullLogRepo, filteredLogRepo, scope)
    private var hasPendingUpdateFilterTask = false
    private var updateFilterTask: Job? = null
    private var startProducerTask: Job? = null

    private val paused = AtomicBoolean(false)

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
        startProduce(DeviceLogProducer(device, logParser))
    }

    override fun startProduceFileLog(file: String) {
        startProduce(FileLogProducer(file, logParser))
    }

    override fun startProduceCustomFileLog() {
        startProduce(CustomFileLogProducer(logConf))
    }

    override fun startProduceCustomDeviceLog() {
        startProduce(CustomDeviceLogProducer(logConf))
    }

    private fun startProduce(logProducer: LogProducer) {
        logUpdater.start()
        playLoadingAnimation(fullLogRepo)
        val lastJob = startProducerTask
        startProducerTask = scope.launch {
            clear()
            Log.d(TAG, "[startProduce] cancel previous jobs")
            lastJob?.cancelAndJoin()
            // 增加延迟，减少无意义任务的启动
            delay(START_TASK_INTERVAL)

            this@LogViewModel.logProducer = logProducer

            coroutineContext.job.invokeOnCompletion {
                Log.d(TAG, "[startProduce] logProducer job completed")
            }

            Log.d(TAG, "[startProduce]")
            logUpdater.updateFilteredLogTriggerCount(true)
            logUpdater.updateFullLogTriggerCount(true)

            launch {
                observeLogProducerState(logProducer)
            }.invokeOnCompletion {
                Log.d(TAG, "[startProduce] observeLogProducerState completed")
            }

            launch {
                startProduceInternal(logProducer)
            }.invokeOnCompletion {
                Log.d(TAG, "[startProduce] realStartProducerTask completed")
            }
        }
    }

    private fun playLoadingAnimation(logRepo: LogRepo) {
        scope.launch {
            logRepo.onLoadingStart()
            delay(100)
            logRepo.onLoadingEnd()
        }
    }

    private suspend fun observeLogProducerState(producer: LogProducer) {
        Log.d(TAG, "[observeLogProducerState]")
        producer.state.collect { state ->
            if (state == LogProducer.State.COMPLETE) {
                Log.d(TAG, "[observeLogProducerState] realStartProducerTask completed")
                _eventFlow.emit(TaskState.IDLE)
                logUpdater.updateFilteredLogTriggerCount(false)
                logUpdater.updateFullLogTriggerCount(false)
                clearCurrentProducer()
            }
            _eventFlow.emit(state.toTaskState())
            Log.d(TAG, "[observeLogProducerState] state=$state")
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

    private fun clearCurrentProducer() {
        logProducer = EmptyLogProducer
    }

    override fun onLogProduceError(error: Throwable) {
        Log.e(TAG, "[onLogProduceError]", error)
        _eventFlow.tryEmit(ErrorEvent(error))
    }

    override fun onLogItemReceived(logItem: LogItem) {
        fullLogRepo.onReceiveLogItem(logItem, getLogFilter())
        filteredLogRepo.onReceiveLogItem(logItem, getLogFilter())
    }

    private suspend fun startProduceInternal(logProducer: LogProducer) {
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

    override fun updateFilter(filter: LogFilter, force: Boolean) {
        if (!force && filter == logFilter) {
            return
        }
        hasPendingUpdateFilterTask = false
        logFilter = filter
        playLoadingAnimation(filteredLogRepo)
        val producer = logProducer
        val lastTask = updateFilterTask
        updateFilterTask = scope.launch {
            lastTask?.cancelAndJoin()

            // 增加延迟，减少无意义任务的启动
            delay(START_TASK_INTERVAL)

            Log.d(TAG, "[updateFilter] start")

            logUpdater.updateFilteredLogTriggerCount(true)

            val producerRunning = AtomicBoolean(false)
            producerRunning.set(producer.isRunning)
            Log.d(TAG, "[updateFilter] producerRunning=${producerRunning.get()}")
            launch {
                producer.pause()
                val composedFilter = getLogFilter(filter)
                filteredLogRepo.writeLogItems { filteredItems ->
                    filteredItems.clear()
                    fullLogRepo.readLogItems { fullItems ->
                        fullItems.forEach { item ->
                            ensureActive()
                            filteredLogRepo.onReceiveLogItem(item, composedFilter)
                        }
                    }
                }
            }.invokeOnCompletion {
                Log.d(TAG, "[updateFilter] realUpdateFilterJob completed")
                logUpdater.updateFilteredLogTriggerCount(false)
                producer.takeIf { producerRunning.get() }?.resume()
            }
        }
    }

    override fun updateFilter(filter: LogFilter) {
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
                writeLinesWithProgress(targetFile, logs.size, { index: Int -> logs[index].logLine }) {
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

    private fun getLogFilter(filter: LogFilter = logFilter): LogFilter {
        return LogFilter {
            return@LogFilter when {
                fullMode -> true
                bookmarkMode -> bookmarkManager.isBookmark(it.num)
                filter.match(it) -> true
                else -> false
            }
        }
    }

    override fun pause() {
        Log.d(TAG, "[pause]")
        if (updateFilterTask?.isCompleted == false) {
            Log.d(TAG, "[pause] has active updateFilterCompanyJobs")
            hasPendingUpdateFilterTask = true
        }
        updateFilterTask?.cancel()
        logProducer.pause()
    }

    override fun resume() {
        Log.d(TAG, "[resume]")
        logProducer.resume()
        if (hasPendingUpdateFilterTask) {
            Log.d(TAG, "[resume] restart pending updateFilterCompanyJobs")
            updateFilter(logFilter, true)
        }
    }

    override fun setPaused(paused: Boolean) {
        Log.d(TAG, "[setPaused] pause=$paused")
        this.paused.set(paused)
        if (paused) {
            pause()
        } else {
            resume()
        }
    }

    override fun isPaused(): Boolean {
        return paused.get()
    }

    override fun clear() {
        fullLogRepo.clear()
        filteredLogRepo.clear()
    }

    override fun cancel() {
        setPaused(true)
        logProducer.cancel()
    }

    override fun destroy() {
        cancel()
        scope.cancel()
        updateFilteredLogTaskSuspender.disable()
        getBindings()?.fullMode?.removeObserver(fullModeObserver)
        getBindings()?.bookmarkMode?.removeObserver(bookmarkModeObserver)
    }

    private interface LogRepo {

        val logObservables: LogProducerManager.LogObservables

        fun <T> readLogItems(readAction: (List<LogItem>) -> T): T

        fun <T> writeLogItems(writeAction: (MutableList<LogItem>) -> T): T

        fun clear()

        fun submitLogItems(force: Boolean)

        fun submitLogItems()

        fun onLoadingStart()

        fun onLoadingEnd()

        fun getLogItemCount(): Int

        fun onReceiveLogItem(logItem: LogItem, logFilter: LogFilter)
    }

    abstract class BaseLogRepo : LogRepo {
        override val logObservables: LogProducerManager.LogObservables
            get() = _logObservables

        private val logItems: MutableList<LogItem> = mutableListOf()

        abstract val name: String

        private val _logItemsFlow = MutableStateFlow(emptyList<LogItem>())
        private val _listState = MutableStateFlow(ListState.EMPTY)
        private val _logRepaintEventFlow = MutableSharedFlow<Event>(extraBufferCapacity = 1)
        private val _logObservables = LogProducerManager.LogObservables(_logItemsFlow, _listState, _logRepaintEventFlow)
        private val logItemsAccessLock = ReentrantReadWriteLock()

        override fun <T> readLogItems(readAction: (List<LogItem>) -> T): T {
            return logItemsAccessLock.read { readAction(logItems) }
        }

        override fun <T> writeLogItems(writeAction: (MutableList<LogItem>) -> T): T {
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
                    _logItemsFlow.value = it.toList()
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
            _logRepaintEventFlow.tryEmit(EmptyEvent)
        }

        override fun getLogItemCount(): Int {
            return readLogItems { it.size }
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

        override fun onReceiveLogItem(logItem: LogItem, logFilter: LogFilter) {
            if (logFilter.match(logItem)) {
                writeLogItems {
                    it.add(logItem)
                }
            }
        }

    }

    private class FullLogRepo : BaseLogRepo() {

        override val name: String = "FullLogRepo"

        override fun onReceiveLogItem(logItem: LogItem, logFilter: LogFilter) {
            writeLogItems { it.add(logItem) }
        }

    }

    /**
     * Determine whether to execute the periodic log update task based on the number of triggers that cause log data changes.
     * The log source may come from the producer or an ongoing filtering task.
     * If the number of triggers is 0, pause the log update. If the number of triggers is greater than 0, continue the log update task.
     */
    private class LogUpdater(
        private val fullLogRepo: FullLogRepo,
        private val filteredLogRepo: FilteredLogRepo,
        private val scope: CoroutineScope
    ) {
        private val updateLogTaskSuspender = CoroutineSuspender("LogUpdater")
        private var updatingFullLogTriggerCount = 0
        private var updatingFilteredLogTriggerCount = 0
        private val updateTriggerCountLock = Any()
        private var notifyDisplayedLogTask: Job? = null

        fun start() {
            if (notifyDisplayedLogTask.isActive) {
                return
            }
            notifyDisplayedLogTask = scope.launch {
                while (isActive) {
                    updateLogTaskSuspender.checkSuspend()
                    delay(UPDATE_LOG_ITEMS_DELAY)
                    fullLogRepo.submitLogItems(true)
                    filteredLogRepo.submitLogItems(true)
                }
            }
        }

        fun updateFullLogTriggerCount(increase: Boolean) {
            synchronized(updateTriggerCountLock) {
                updatingFullLogTriggerCount += if (increase) 1 else -1
                Log.d(TAG, "[updateFullLogTriggerCount] count=${updatingFullLogTriggerCount}")
                ensureSuspenderState()
                if (updatingFullLogTriggerCount == 0) {
                    fullLogRepo.submitLogItems()
                }
            }
        }

        fun updateFilteredLogTriggerCount(increase: Boolean) {
            synchronized(updateTriggerCountLock) {
                updatingFilteredLogTriggerCount += if (increase) 1 else -1
                Log.d(TAG, "[updateFilteredLogTriggerCount] count=${updatingFilteredLogTriggerCount}")
                ensureSuspenderState()
                if (updatingFilteredLogTriggerCount == 0) {
                    filteredLogRepo.submitLogItems()
                }
            }
        }

        private fun ensureSuspenderState() {
            if (updatingFilteredLogTriggerCount + updatingFullLogTriggerCount > 0) {
                updateLogTaskSuspender.disable()
            } else {
                updateLogTaskSuspender.enable()
            }
        }

        companion object {
            private const val UPDATE_LOG_ITEMS_DELAY = 100L
        }
    }

    companion object {
        private const val TAG = "LogViewModel"

        private const val START_TASK_INTERVAL = 100L

        private val Job?.isActive: Boolean
            get() = this?.isActive == true
    }
}
