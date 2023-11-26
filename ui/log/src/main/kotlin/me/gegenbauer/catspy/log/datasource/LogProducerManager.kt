package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.java.ext.Event
import me.gegenbauer.catspy.log.model.LogItem
import me.gegenbauer.catspy.view.state.ListState

interface LogProducerManager<T: LogItem> {

    val eventFlow: StateFlow<Event>

    val fullLogItemsFlow: StateFlow<List<T>>

    val filteredLogItemsFlow: StateFlow<List<T>>

    val fullLogListState: StateFlow<ListState>

    val filteredLogListState: StateFlow<ListState>

    fun startProduce(logProducer: LogProducer)

    fun onLogItemReceived(logItem: T)

    fun onLogProduceError(error: Throwable)

    fun pause()

    fun resume()

    fun clear()
}

enum class TaskState: Event {
    IDLE, RUNNING, PAUSED
}