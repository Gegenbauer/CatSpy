package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.CPU
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.configuration.SettingsManager
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
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.log.ui.panel.LogPanel
import me.gegenbauer.catspy.view.filter.FilterCache
import me.gegenbauer.catspy.view.filter.toFilterKey
import me.gegenbauer.catspy.view.state.ListState
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.system.measureTimeMillis


// TODO 增加过滤器提示，实时日志时，缓存 tag 作为 tag 输入提示
// TODO 优化（读取设备日志时，如果最后一行不可见，则不刷新UI数据源）
// TODO 全局消息管理
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

    private val updatingFullLogTriggerCount = MutableStateFlow(0)
    private val updatingFilteredLogTriggerCount = MutableStateFlow(0)
    private val updateTriggerCountLock = ReentrantReadWriteLock()
    private var notifyDisplayedLogTask: Job? = null
    private var hasPendingUpdateFilterTask = false
    private var updateFilterTask: Job? = null

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
        val logTabPanel = contexts.getContext(LogTabPanel::class.java)
        logTabPanel?.let {
            bookmarkManager = ServiceManager.getContextService(logTabPanel, BookmarkManager::class.java)
        }
    }

    private fun getBindings(): LogPanel.LogPanelBinding? {
        return contexts.getContext(LogPanel::class.java)?.binding
    }

    override fun startProduce(logProducer: LogProducer) {
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

            val observeLogProducerStateTask = async { observeLogProducerState(logProducer) }
            val realStartProducerTask = async { startProduceInternal() }

            produceCompanyJobs.add(observeLogProducerStateTask)
            produceCompanyJobs.add(realStartProducerTask)

            realStartProducerTask.invokeOnCompletion {
                Log.d(TAG, "[startProduce] realStartProducerTask completed")
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
            delay(500)
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

            val realUpdateFilterJob = async {
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
    suspend fun saveLog(file: File) {
        withContext(Dispatchers.GIO) {
            coroutineContext.job.invokeOnCompletion { t ->
                if (t != null) {
                    Log.e(TAG, "[saveLog] error", t)
                }
            }
            fullLogRepo.readLogItems { fullLogItems ->
                file.writeText(fullLogItems.joinToString("\n") { it.toLogLine() })
            }
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
            SettingsManager.settings.apply {
                (logFilterHistory + tagFilterHistory + searchHistory + highlightHistory).forEach {
                    filterCache[it.toFilterKey(true)]
                    filterCache[it.toFilterKey(false)]
                }
            }
        }
    }

    override fun pause() {
        if (updateFilterCompanyJobs.any { it.isActive }) {
            Log.d(TAG, "[pause] has active updateFilterCompanyJobs")
            hasPendingUpdateFilterTask = true
        }
        updateFilterCompanyJobs.forEach(Job::cancel)
        logProducer.pause()
    }

    override fun resume() {
        logProducer.resume()
        if (hasPendingUpdateFilterTask) {
            Log.d(TAG, "[resume] restart pending updateFilterCompanyJobs")
            updateFilter(logcatFilter, true)
        }
    }

    override fun clear() {
        fullLogRepo.clear()
        filteredLogRepo.clear()
    }

    override fun destroy() {
        logProducer.destroy()
        updateFullLogTaskSuspender.disable()
        updateFilteredLogTaskSuspender.disable()
        scope.cancel()
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