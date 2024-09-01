package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.concurrency.ErrorEvent
import me.gegenbauer.catspy.concurrency.Event
import me.gegenbauer.catspy.java.ext.formatDuration
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.file.writeLinesWithProgress
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.panel.Task
import me.gegenbauer.catspy.view.panel.TaskHandle
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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

    override val logFilter: LogFilter
        get() = logConf.getFilter()

    override var fullTableSelectedRows: List<Int> = emptyList()

    override var filteredTableSelectedRows: List<Int> = emptyList()

    override val tempLogFile: File
        get() = produceLogTask.logProducer.tempFile

    override val device: String
        get() = (produceLogTask.logProducer as? DeviceLogProducer)?.device ?: EMPTY_STRING

    private val logConf by lazy { contexts.getContext(LogConfiguration::class.java)!! }
    private val _eventFlow = MutableSharedFlow<Event>(
        extraBufferCapacity = 30,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val fullLogRepo = FullLogRepo()
    private val filteredLogRepo = FilteredLogRepo()
    private val scope = ViewModelScope()
    private val logUpdater = LogUpdater(fullLogRepo, filteredLogRepo, scope)
    private val logProcessMutex = Mutex()

    private val filterLogTask by lazy {
        FilterLogTask(
            fullLogRepo,
            filteredLogRepo,
            logUpdater,
            logProcessMutex,
            scope,
            contexts
        )
    }
    private val produceLogTask by lazy {
        ProduceLogTask(
            fullLogRepo,
            filteredLogRepo,
            logUpdater,
            _eventFlow,
            logProcessMutex,
            scope,
            contexts
        )
    }

    private val globalStatus by lazy { ServiceManager.getContextService(StatusPanel::class.java) }

    private val paused = AtomicBoolean(false)

    override fun configureContext(context: Context) {
        super.configureContext(context)
        filterLogTask.registerFilterStateObserver()
        filterLogTask.logProducerProvider = produceLogTask::logProducer
        produceLogTask.filterProvider = filterLogTask::composedFilter
        produceLogTask.filterLogTaskCancellationHandle = filterLogTask::cancelAndJoin
    }

    override fun startProduceDeviceLog(device: String) {
        produceLogTask.startProduceDeviceLog(device)
    }

    override fun startProduceFileLog(file: String) {
        produceLogTask.startProduceFileLog(file)
    }

    override fun startProduceCustomFileLog() {
        produceLogTask.startProduceCustomFileLog()
    }

    override fun startProduceCustomDeviceLog() {
        produceLogTask.startProduceCustomDeviceLog()
    }

    override fun updateFilter(filter: LogFilter) {
        filterLogTask.updateFilter(filter)
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

    override fun pause() {
        Log.d(TAG, "[pause]")
        filterLogTask.pause()
        produceLogTask.pause()
        logUpdater.pause()
    }

    override fun isRunning(): Boolean {
        return produceLogTask.logProducer.isRunning
    }

    override fun resume() {
        Log.d(TAG, "[resume]")
        produceLogTask.resume()
        filterLogTask.resume()
        logUpdater.resume()
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
        produceLogTask.cancel()
        filterLogTask.cancel()
        logUpdater.cancel()
    }

    override fun destroy() {
        cancel()
        scope.cancel()
        filterLogTask.unregisterFilterStateObserver()
    }

    private interface Task {

        fun pause()

        fun resume()

        fun cancel()

        suspend fun cancelAndJoin()
    }

    private class ProduceLogTask(
        private val fullLogRepo: LogRepo,
        private val filteredLogRepo: LogRepo,
        private val logUpdater: LogUpdater,
        private val eventFlow: MutableSharedFlow<Event>,
        private val logProcessMutex: Mutex,
        private val scope: CoroutineScope,
        private val contexts: Contexts
    ) : Task {

        var filterProvider: () -> LogFilter = { LogFilter.default }
        var filterLogTaskCancellationHandle: suspend () -> Unit = {}
        var logProducer: LogProducer = EmptyLogProducer

        private val logConf by lazy { contexts.getContext(LogConfiguration::class.java)!! }
        private var startProducerTask: Job? = null

        private val logParser: LogParser
            get() = logConf.logMetaData.parser

        fun startProduceDeviceLog(device: String) {
            startProduce(DeviceLogProducer(device, logParser))
        }

        fun startProduceFileLog(file: String) {
            startProduce(FileLogProducer(file, logParser))
        }

        fun startProduceCustomFileLog() {
            startProduce(CustomFileLogProducer(logConf))
        }

        fun startProduceCustomDeviceLog() {
            startProduce(CustomDeviceLogProducer(logConf))
        }

        private fun startProduce(logProducer: LogProducer) {
            scope.launch {

                logProcessMutex.withLock {
                    Log.d(TAG, "[startProduce] cancel previous jobs")
                    startProducerTask?.cancelAndJoin()
                    startProducerTask = coroutineContext.job

                    fullLogRepo.onLoadingStart()
                    coroutineContext.job.invokeOnCompletion {
                        fullLogRepo.onLoadingEnd()
                    }
                }

                // 增加延迟，减少无意义任务的启动
                delay(START_TASK_INTERVAL)

                logProcessMutex.withLock {
                    logConf.filterCreatedAfterMetadataChanged.compareAndSet(true, true)
                    filterLogTaskCancellationHandle()
                    this@ProduceLogTask.logProducer = logProducer
                    logUpdater.start()
                    coroutineContext.job.invokeOnCompletion {
                        Log.d(TAG, "[startProduce] completed")
                        onProduceEnd()
                    }

                    Log.d(TAG, "[startProduce]")
                    onProduceStart()
                }

                val observeJob = launch { observeLogProducerState(logProducer) }
                launch { startProduceInternal(logProducer) }.invokeOnCompletion {
                    observeJob.cancel()
                }
                delay(500)
                fullLogRepo.onLoadingEnd()
            }
        }

        private fun onProduceStart() {
            logUpdater.updateFilteredLogTriggerCount(true)
            logUpdater.updateFullLogTriggerCount(true)
            fullLogRepo.clear()
            filteredLogRepo.clear()
        }

        private fun onProduceEnd() {
            logUpdater.updateFilteredLogTriggerCount(false)
            logUpdater.updateFullLogTriggerCount(false)
            logProducer = EmptyLogProducer
            eventFlow.tryEmit(TaskState.IDLE)
        }

        private suspend fun observeLogProducerState(producer: LogProducer) {
            Log.d(TAG, "[observeLogProducerState]")
            producer.state.collect { state ->
                eventFlow.emit(state.toTaskState())
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

        private suspend fun startProduceInternal(logProducer: LogProducer) {
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

        private fun onLogProduceError(error: Throwable) {
            Log.e(TAG, "[onLogProduceError]", error)
            eventFlow.tryEmit(ErrorEvent(error))
        }

        private fun onLogItemReceived(logItem: LogItem) {
            val logFilter = filterProvider()
            fullLogRepo.onReceiveLogItem(logItem, logFilter)
            filteredLogRepo.onReceiveLogItem(logItem, logFilter)
        }

        override fun pause() {
            logProducer.pause()
        }

        override fun resume() {
            logProducer.resume()
        }

        override fun cancel() {
            logProducer.cancel()
        }

        override suspend fun cancelAndJoin() {
            // no-op
        }
    }

    private class FilterLogTask(
        private val fullLogRepo: LogRepo,
        private val filteredLogRepo: LogRepo,
        private val logUpdater: LogUpdater,
        private val logProcessMutex: Mutex,
        private val scope: CoroutineScope,
        contexts: Contexts
    ) : Task {
        var logFilter: LogFilter = LogFilter.default

        var logProducerProvider: () -> LogProducer = { EmptyLogProducer }

        private val fullModeState = FullModeFilterState(contexts)
        private val bookmarkModeState = BookmarkFilterState(contexts)

        private var hasPendingUpdateFilterTask = false
        private var updateFilterTask: Job? = null

        var composedFilter: ComposedFilter = ComposedFilter(logFilter, fullModeState, bookmarkModeState)

        fun updateFilter(filter: LogFilter) {
            updateFilter(filter, false)
        }

        fun updateFilter(filter: LogFilter, force: Boolean) {
            val newComposedFilter = ComposedFilter(filter, fullModeState, bookmarkModeState)
            if (!force && newComposedFilter == composedFilter) {
                return
            }
            hasPendingUpdateFilterTask = false
            logFilter = filter
            composedFilter = newComposedFilter
            scope.launch {
                logProcessMutex.withLock {
                    updateFilterTask?.cancelAndJoin()
                    updateFilterTask = coroutineContext.job

                    filteredLogRepo.onLoadingStart()
                    coroutineContext.job.invokeOnCompletion {
                        filteredLogRepo.onLoadingEnd()
                    }
                }

                // 增加延迟，减少无意义任务的启动
                delay(START_TASK_INTERVAL)

                Log.d(TAG, "[updateFilter] start, filter=$filter")

                val producerRunning = AtomicBoolean(false)
                val producer = AtomicReference<LogProducer>()
                logProcessMutex.withLock {
                    producer.set(logProducerProvider())
                    producerRunning.set(producer.get().isRunning)
                }

                coroutineContext.job.invokeOnCompletion {
                    Log.d(TAG, "[updateFilter] realUpdateFilterJob completed")
                    logUpdater.updateFilteredLogTriggerCount(false)
                    producer.get().takeIf { producerRunning.get() }?.resume()
                }

                launch { playFakeLoadingAnimation() }

                Log.d(TAG, "[updateFilter] producerRunning=${producerRunning.get()}")
                producer.get().pause()
                logUpdater.updateFilteredLogTriggerCount(true)
                filterLog(newComposedFilter)
            }
        }

        private suspend fun playFakeLoadingAnimation() {
            delay(100)
            filteredLogRepo.submitLogItems()
            filteredLogRepo.onLoadingEnd()
        }

        private fun CoroutineScope.filterLog(filter: LogFilter) {
            filteredLogRepo.writeLogItems { filteredItems ->
                filteredItems.clear()
                filteredLogRepo.submitLogItems()
                fullLogRepo.readLogItems { fullItems ->
                    fullItems.forEach { item ->
                        ensureActive()
                        filteredLogRepo.onReceiveLogItem(item, filter)
                    }
                }
            }
        }

        fun registerFilterStateObserver() {
            fullModeState.observe { updateFilter(logFilter, true) }
            bookmarkModeState.observe { updateFilter(logFilter, true) }
        }

        fun unregisterFilterStateObserver() {
            fullModeState.stopObserving()
            bookmarkModeState.stopObserving()
        }

        override fun pause() {
            if (updateFilterTask?.isCompleted == false) {
                Log.d(TAG, "[pause] has active updateFilterCompanyJobs")
                hasPendingUpdateFilterTask = true
            }
            updateFilterTask?.cancel()
        }

        override fun resume() {
            if (hasPendingUpdateFilterTask) {
                Log.d(TAG, "[resume] restart pending updateFilterCompanyJobs")
                updateFilter(logFilter, true)
            }
        }

        override fun cancel() {
            updateFilterTask?.cancel()
        }

        override suspend fun cancelAndJoin() {
            updateFilterTask?.cancelAndJoin()
        }

        private class ComposedFilter(
            private val filter: LogFilter,
            private val fullModeState: FullModeFilterState,
            private val bookmarkModeState: BookmarkFilterState
        ) : LogFilter {
            override fun match(item: LogItem): Boolean {
                return when {
                    fullModeState.enabled -> true
                    bookmarkModeState.enabled -> bookmarkModeState.isBookmark(item.num)
                    filter.match(item) -> true
                    else -> false
                }
            }

            override fun equals(other: Any?): Boolean {
                if (other !is ComposedFilter) return false
                return filter == other.filter
                        && fullModeState.enabled == other.fullModeState.enabled
                        && bookmarkModeState.enabled == other.bookmarkModeState.enabled
            }

            override fun hashCode(): Int {
                return filter.hashCode() + 31 * fullModeState.hashCode() + 31 * 31 * bookmarkModeState.hashCode()
            }
        }
    }

    companion object {
        private const val TAG = "LogViewModel"

        private const val START_TASK_INTERVAL = 100L
    }
}

