package me.gegenbauer.catspy.log.datasource

import com.android.ddmlib.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.ViewModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.BookmarkManager
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


// TODO 渲染日志弹窗
// TODO Memory Monitor
// TODO 优化
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

    override var highlightFilter: LogcatFilter = LogcatFilter.EMPTY_FILTER

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

    private var updateFullLogItemsJob: Job? = null
    private var updateFilteredLogItemsJob: Job? = null
    private var observeProducerStateJob: Job? = null

    private var updateFilterJob: Job? = null
    private lateinit var bookmarkManager: BookmarkManager

    private val fullModeObserver = Observer<Boolean> {
        fullMode = it ?: false
        updateLogsOnFilterUpdate()
    }
    private val bookmarkModeObserver = Observer<Boolean> {
        bookmarkMode = it ?: false
        updateLogsOnFilterUpdate()
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
        observeProducerStateJob?.cancel()

        this.logProducer = logProducer

        GLog.d(TAG, "[startProduce]")
        observeLogProducerState(logProducer)
        startProduceInternal()
        updateFilteredLogItemsJob = startUpdateLogItems(filteredLogRepo, updateFilteredLogItemsJob)
        updateFullLogItemsJob = startUpdateLogItems(fullLogRepo, updateFullLogItemsJob)
    }

    private fun observeLogProducerState(producer: LogProducer) {
        observeProducerStateJob = scope.launch {
            producer.state.collect { state ->
                GLog.d(TAG, "[observeLogProducerState] state=$state")
                _taskState.value = state.toTaskState()
            }
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
        GLog.e(TAG, "[onLogProduceError]", error)
        _errorFlow.value = error
    }

    override fun onLogItemReceived(logItem: LogcatItem) {
        fullLogRepo.onReceiveLogItem(logItem, getLogFilter())
        filteredLogRepo.onReceiveLogItem(logItem, getLogFilter())
    }

    private fun startProduceInternal() {
        scope.launch {
            coroutineContext.job.invokeOnCompletion { t ->
                logProducer.takeIf { t != null }?.cancel()
                stopUpdateFullLog()
                stopUpdateFilteredLog()
            }
            GLog.d(TAG, "[startProduceInternal] start")
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
            GLog.d(TAG, "[startProduceInternal] end")
        }
    }

    override fun updateFilter(filter: LogcatFilter) {
        fullLogRepo.readLogItems { fullLogItems ->
            ::updateLogsOnFilterUpdate
                .takeIf { fullLogItems.isNotEmpty() && highlightFilter != filter }
                ?.invoke()
            this.highlightFilter = filter
        }
    }

    /**
     * save full log to file
     */
    fun saveLog(file: File) {
        scope.launch {
            coroutineContext.job.invokeOnCompletion { t ->
                if (t != null) {
                    GLog.e(TAG, "[saveLog] error", t)
                }
            }
            fullLogRepo.readLogItems { fullLogItems ->
                file.writeText(fullLogItems.joinToString("\n") { it.toLogLine() })
            }
        }
    }

    private fun updateLogsOnFilterUpdate() {
        updateFilteredLogItemsJob = startUpdateLogItems(filteredLogRepo, updateFilteredLogItemsJob)
        updateFilterInternal()
    }

    private fun updateFilterInternal() {
        Log.d(TAG, "[updateFilterInternal] updateFilterJob canceled=$updateFilterJob")
        updateFilterJob?.cancel()
        val producerRunning = AtomicBoolean(false)
        updateFilterJob = scope.launch {
            coroutineContext.job.invokeOnCompletion {
                logProducer.takeIf { producerRunning.get() }?.resume()
                stopUpdateFilteredLog()
            }
            producerRunning.set(logProducer.isRunning)
            logProducer.pause()
            filteredLogRepo.writeLogItems { filteredLogItems ->
                filteredLogItems.clear()
                fullLogRepo.readLogItems { fullLogItems ->
                    fullLogItems.forEach {
                        filteredLogRepo.onReceiveLogItem(it, getLogFilter())
                    }
                }
            }
            logProducer.takeIf { producerRunning.get() }?.resume()
        }
        Log.d(TAG, "[updateFilterInternal] updateFilterJob started=$updateFilterJob")
    }

    protected open fun getLogFilter(): LogFilter<LogcatItem> {
        return LogFilter {
            return@LogFilter when {
                fullMode -> true
                bookmarkMode -> bookmarkManager.isBookmark(it.num)
                highlightFilter.match(it) -> true
                else -> false
            }
        }
    }

    private fun startUpdateLogItems(repo: LogRepo, updateLogItemsJob: Job?): Job {
        if (repo is FilteredLogRepo) {
            GLog.d(TAG, "[updateLogsOnFilterUpdate] updateLogItemsJob canceled=$updateLogItemsJob ")
        }
        updateLogItemsJob?.cancel()
        return scope.launch {
            coroutineContext.job.invokeOnCompletion { t ->
                if (repo is FilteredLogRepo) {
                    GLog.d(TAG, "[updateLogsOnFilterUpdate] updateLogItemsJob completed=$updateLogItemsJob ")
                }
                repo.takeIf { t != null }?.submitLogItems()
            }
            repo.updateLogWithStateChange {
                while (isActive) {
                    delay(UPDATE_LOG_ITEMS_DELAY)
                    repo.submitLogItems()
                }
            }
        }.apply {
            if (repo is FilteredLogRepo) {
                GLog.d(TAG, "[updateLogsOnFilterUpdate] updateLogItemsJob started=$this")
            }
        }
    }

    private fun stopUpdateFilteredLog() {
        updateFilteredLogItemsJob
            ?.takeIf { updateFilterJob.isActive.not() && logProducer.isRunning.not() }
            ?.cancel()
    }

    private fun stopUpdateFullLog() {
        updateFullLogItemsJob?.takeIf { logProducer.isRunning.not() }?.cancel()
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

        fun submitLogItems()

        suspend fun updateLogWithStateChange(updateLogWork: suspend () -> Unit)

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

        override suspend fun updateLogWithStateChange(updateLogWork: suspend () -> Unit) {
            beforeUpdateLog()

            updateLogWork()

            afterUpdateLog()
        }

        private fun beforeUpdateLog() {
            GLog.d(name, "[beforeUpdateLog]")
            _listState.value = ListState.LOADING
        }

        private fun afterUpdateLog() {
            submitLogItems()
            GLog.d(name, "[afterUpdateLog]")
        }

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

        override fun submitLogItems() {
            readLogItems {
                _logItemsFlow.value = it

                if (it.isNotEmpty()) {
                    _listState.value = ListState.NORMAL
                } else {
                    _listState.value = ListState.EMPTY
                }
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