package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.model.LogFilter
import me.gegenbauer.catspy.log.model.LogcatFilter
import me.gegenbauer.catspy.log.model.LogcatItem
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.log.ui.panel.LogPanel
import me.gegenbauer.catspy.view.state.ListState
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


// TODO 修复实时日志模式时，打开文件日志偶现文件日志未加载完
// TODO 打开新页面时，过滤器默认清空
// TODO 增加过滤器提示，实时日志时，缓存 tag 作为 tag 输入提示
// TODO Memory Monitor
// TODO 跳转到某一行尽量跳转到中间，否则跳转到上半区域
// TODO 优化（读取设备日志时，如果最后一行不可见，则不刷新UI数据源）
// TODO 导出应用日志
// TODO 全局消息管理
open class LogViewModel(override val contexts: Contexts = Contexts.default) :
    Context, LogProducerManager<LogcatItem>, LogFilterable {
    override val fullLogItemsFlow: StateFlow<List<LogcatItem>>
        get() = fullLogRepo.logItemsFlow

    override val filteredLogItemsFlow: StateFlow<List<LogcatItem>>
        get() = filteredLogRepo.logItemsFlow

    override val errorFlow: StateFlow<Throwable?>
        get() = _errorFlow

    override val fullLogListState: StateFlow<ListState>
        get() = fullLogRepo.listState

    override val filteredLogListState: StateFlow<ListState>
        get() = filteredLogRepo.listState
    override val taskState: StateFlow<TaskState>
        get() = _taskState

    override var logcatFilter: LogcatFilter = LogcatFilter.EMPTY_FILTER

    override var fullMode: Boolean = false

    override var bookmarkMode: Boolean = false

    override var fullTableSelectedRows: List<Int> = emptyList()

    override var filteredTableSelectedRows: List<Int> = emptyList()

    private var logProducer: LogProducer = EmptyLogProducer

    private val _taskState = MutableStateFlow(TaskState.IDLE)
    private val _errorFlow = MutableStateFlow<Throwable?>(null)

    private val fullLogRepo = FullLogRepo()
    private val filteredLogRepo = FilteredLogRepo()

    private val scope = ViewModelScope()
    private val produceCompanyJobs = mutableListOf<Job>()
    private var updateFilterCompanyJobs = mutableListOf<Job>()

    private lateinit var bookmarkManager: BookmarkManager

    private val fullModeObserver = Observer<Boolean> {
        fullMode = it ?: false
        updateFilter(logcatFilter)
    }
    private val bookmarkModeObserver = Observer<Boolean> {
        bookmarkMode = it ?: false
        updateFilter(logcatFilter)
    }
    private val updateFullLogTaskSuspender = CoroutineSuspender("updateFullLogTaskSuspender")
    private val updateFilteredLogTaskSuspender = CoroutineSuspender("updateFilteredLogTaskSuspender")

    private val updatingFullLogTriggerCount = MutableStateFlow(0)
    private val updatingFilteredLogTriggerCount = MutableStateFlow(0)
    private var notifyDisplayedLogTask: Job? = null

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
            async {
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
            async {
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
            updatingFullLogTriggerCount.value += 1
            updatingFilteredLogTriggerCount.value += 1
            val observeLogProducerStateTask = async { observeLogProducerState(logProducer) }
            val realStartProducerTask = async { startProduceInternal() }

            produceCompanyJobs.add(observeLogProducerStateTask)
            produceCompanyJobs.add(realStartProducerTask)

            realStartProducerTask.invokeOnCompletion {
                Log.d(TAG, "[startProduce] realStartProducerTask completed")
                updatingFullLogTriggerCount.value -= 1
                updatingFilteredLogTriggerCount.value -= 1
                observeLogProducerStateTask.cancel()
            }
        }
    }

    private fun playLoadingAnimation(logRepo: LogRepo) {
        scope.launch {
            logRepo.onLoadingStart()
            delay(200)
            logRepo.onLoadingEnd()
        }
    }

    private suspend fun observeLogProducerState(producer: LogProducer) {
        Log.d(TAG, "[observeLogProducerState]")
        producer.state.collect { state ->
            Log.d(TAG, "[observeLogProducerState] state=$state")
            _taskState.value = state.toTaskState()
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
        _errorFlow.value = error
    }

    override fun onLogItemReceived(logItem: LogcatItem) {
        fullLogRepo.onReceiveLogItem(logItem, getLogFilter())
        filteredLogRepo.onReceiveLogItem(logItem, getLogFilter())
    }

    private suspend fun startProduceInternal() {
        Log.d(TAG, "[startProduceInternal] start")
        logProducer.start().collect { result ->
            val exception = result.exceptionOrNull()
            val item = result.getOrNull()

            exception?.let {
                onLogProduceError(it)
                return@collect
            }

            item?.let { onLogItemReceived(it) }
        }
        logProducer.moveToState(LogProducer.State.COMPLETE)
        Log.d(TAG, "[startProduceInternal] end")
    }

    override fun updateFilter(filter: LogcatFilter) {
        if (filter == logcatFilter) {
            Log.d(TAG, "[updateFilter] filter not changed")
            return
        }
        logcatFilter = filter
        val composedFilter = getLogFilter(filter)
        if (fullLogRepo.logItems.isEmpty()) {
            return
        }
        playLoadingAnimation(filteredLogRepo)
        scope.launch {
            updateFilterCompanyJobs.toList().forEach { it.cancelAndJoin() }
            updateFilterCompanyJobs.clear()

            updatingFilteredLogTriggerCount.value += 1
            val realUpdateFilterJob = async { updateFilterInternal(composedFilter) }

            updateFilterCompanyJobs.add(realUpdateFilterJob)

            realUpdateFilterJob.invokeOnCompletion {
                Log.d(TAG, "[updateFilter] realUpdateFilterJob completed")
                updatingFilteredLogTriggerCount.value -= 1
            }
        }
    }

    /**
     * save full log to file
     */
    fun saveLog(file: File) {
        scope.launch {
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
        withContext(Dispatchers.Default) {
            Log.d(TAG, "[updateFilterInternal] start")
            val producerRunning = AtomicBoolean(false)
            producerRunning.set(logProducer.isRunning)
            coroutineContext.job.invokeOnCompletion {
                logProducer.takeIf { producerRunning.get() }?.resume()
            }
            logProducer.pause()
            filteredLogRepo.writeLogItems { filteredLogItems ->
                filteredLogItems.clear()
                fullLogRepo.readLogItems { fullLogItems ->
                    fullLogItems.forEach {
                        filteredLogRepo.onReceiveLogItem(it, filter)
                    }
                }
            }
            Log.d(TAG, "[updateFilterInternal] end")
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

    override fun pause() {
        logProducer.pause()
    }

    override fun resume() {
        logProducer.resume()
    }

    override fun clear() {
        fullLogRepo.clear()
        filteredLogRepo.clear()
    }

    override fun destroy() {
        logProducer.destroy()
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
        private const val UPDATE_LOG_ITEMS_DELAY = 500L

        private val Job?.isActive: Boolean
            get() = this?.isActive == true
    }
}