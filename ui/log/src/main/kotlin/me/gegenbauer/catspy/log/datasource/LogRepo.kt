package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import me.gegenbauer.catspy.java.ext.EmptyEvent
import me.gegenbauer.catspy.java.ext.Event
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.view.state.ListState
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal interface LogRepo {

    val logObservables: LogProducerManager.LogObservables

    fun <T> readLogItems(readAction: (List<LogItem>) -> T): T

    fun <T> writeLogItems(writeAction: (MutableList<LogItem>) -> T): T

    fun clear()

    fun reset()

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

    override fun reset() {
        logItemsAccessLock.write { logItems.clear() }
        _logItemsFlow.value = emptyList()
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
                    _logRepaintEventFlow.tryEmit(EmptyEvent)
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

class FilteredLogRepo : BaseLogRepo() {

    override val name: String = "FilteredLogRepo"

    override fun onReceiveLogItem(logItem: LogItem, logFilter: LogFilter) {
        if (logFilter.match(logItem)) {
            writeLogItems {
                it.add(logItem)
            }
        }
    }

}

class FullLogRepo : BaseLogRepo() {

    override val name: String = "FullLogRepo"

    override fun onReceiveLogItem(logItem: LogItem, logFilter: LogFilter) {
        writeLogItems { it.add(logItem) }
    }

}