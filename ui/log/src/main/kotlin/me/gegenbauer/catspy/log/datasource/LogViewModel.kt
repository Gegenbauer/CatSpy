package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.concurrency.ErrorEvent
import me.gegenbauer.catspy.concurrency.Event
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.java.ext.formatDuration
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.file.writeLinesWithProgress
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.panel.Task
import me.gegenbauer.catspy.view.panel.TaskHandle
import java.io.File
import java.util.HashSet
import java.util.Objects
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
        get() = logConf.getCurrentFilter()

    override val fullTableSelectedRows: MutableSet<Int> = HashSet()

    override val filteredTableSelectedRows: MutableSet<Int> = HashSet()

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
    private val scope = ModelScope()
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
    suspend fun saveLog(targetFile: File, isFilteredLog: Boolean): Result<File?> {
        return withContext(Dispatchers.GIO) {
            val repo = if (isFilteredLog) filteredLogRepo else fullLogRepo
            val logs = repo.readLogItems { it.toList() }
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
                writeLinesWithProgress(targetFile, logs.size, { index: Int -> logs[index].toString() }) {
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

    override fun isActive(): Boolean {
        return produceLogTask.logProducer.isActive
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
        var logProducer: LogProducer = EmptyLogProducer()

        private val logConf by lazy { contexts.getContext(LogConfiguration::class.java)!! }
        private val suspender = CoroutineSuspender("ProduceLogTask")
        private var startProducerTask: Job? = null

        fun startProduceDeviceLog(device: String) {
            startProduce(DeviceLogProducer(device, logConf))
        }

        fun startProduceFileLog(file: String) {
            startProduce(FileLogProducer(file, logConf))
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
                    suspender.disable()
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
                    Log.d(TAG, "[startProduce] startProduceInternal completed")
                }
                if (logConf.isPreviewMode.not()) {
                    delay(PRODUCE_LOADING_ANIM_MIN_DURATION)
                }
                fullLogRepo.onLoadingEnd()
            }
        }

        private fun clearBookmark() {
            val mainUI = contexts.getContext(BaseLogMainPanel::class.java)
            mainUI ?: run {
                Log.w(TAG, "[clearBookmark] mainUI is null")
                return
            }
            val bookmarkManager = ServiceManager.getContextService(mainUI, BookmarkManager::class.java)
            bookmarkManager.clear()
        }

        private fun onProduceStart() {
            logUpdater.updateFilteredLogTriggerCount(true)
            logUpdater.updateFullLogTriggerCount(true)
            clearBookmark()
            fullLogRepo.clear()
            filteredLogRepo.clear()
        }

        private fun onProduceEnd() {
            logUpdater.updateFilteredLogTriggerCount(false)
            logUpdater.updateFullLogTriggerCount(false)
            logProducer = EmptyLogProducer(logProducer.tempFile)
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

            fun onLogItemReceived(logItem: LogItem) {
                val logFilter = filterProvider()
                fullLogRepo.onReceiveLogItem(logItem, logFilter)
                filteredLogRepo.onReceiveLogItem(logItem, logFilter)
            }

            fun onLogProduceError(error: Throwable) {
                Log.e(TAG, "[onLogProduceError]", error)
                eventFlow.tryEmit(ErrorEvent(error))
            }

            val cost = measureTimeMillis {
                logProducer.start().collect { result ->
                    suspender.checkSuspend()
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

        override fun pause() {
            logProducer.pause()
            suspender.enable()
        }

        override fun resume() {
            suspender.disable()
            logProducer.resume()
        }

        override fun cancel() {
            suspender.disable()
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

        var logProducerProvider: () -> LogProducer = { EmptyLogProducer() }

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

                Log.d(TAG, "[updateFilter] start, filter=$composedFilter")

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
            delay(FILTER_LOADING_ANIM_MIN_DURATION)
            filteredLogRepo.submitLogItems()
            filteredLogRepo.onLoadingEnd()
        }

        private fun CoroutineScope.filterLog(filter: LogFilter) {
            val batchSize = FILTER_BATCH_SIZE
            val fullItems = fullLogRepo.readLogItems { it.toList() }
            val batches = fullItems.chunked(batchSize)

            measureTimeMillis {
                filteredLogRepo.writeLogItems { filteredItems ->
                    filteredItems.clear()
                    filteredLogRepo.submitLogItems()

                    val deferredResults = batches.map { batch ->
                        async {
                            batch.filter { item ->
                                ensureActive()
                                filter.match(item)
                            }
                        }
                    }

                    runBlocking {
                        deferredResults.forEach { deferred ->
                            val filteredBatch = deferred.await()
                            filteredItems.addAll(filteredBatch)
                        }
                        filteredLogRepo.submitLogItems()
                    }
                }
            }.let { Log.d(TAG, "[filterLog] cost=${formatDuration(it)}") }
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

        class ComposedFilter(
            val filter: LogFilter,
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
                return Objects.hash(filter, fullModeState.enabled, bookmarkModeState.enabled)
            }

            override fun toString(): String {
                return "ComposedFilter(filter=$filter, fullModeState=${fullModeState.enabled}," +
                        " bookmarkModeState=${bookmarkModeState.enabled})"
            }
        }
    }

    companion object {
        private const val TAG = "LogViewModel"

        private const val START_TASK_INTERVAL = 100L
        private const val PRODUCE_LOADING_ANIM_MIN_DURATION = 500L
        private const val FILTER_LOADING_ANIM_MIN_DURATION = 100L
        private const val FILTER_BATCH_SIZE = 100000
    }
}

