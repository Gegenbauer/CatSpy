package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.java.ext.Event
import me.gegenbauer.catspy.log.model.LogItem
import me.gegenbauer.catspy.log.model.LogcatItem
import me.gegenbauer.catspy.view.state.ListState
import java.io.File

interface LogProducerManager<T: LogItem> {

    val eventFlow: StateFlow<Event>

    val fullLogItemsFlow: StateFlow<List<T>>

    val filteredLogItemsFlow: StateFlow<List<T>>

    val fullLogListState: StateFlow<ListState>

    val filteredLogListState: StateFlow<ListState>

    val processValueExtractor: (LogcatItem) -> String

    val tempLogFile: File

    val device: String

    fun startProduceDeviceLog(device: String)

    fun startProduceFileLog(file: String)

    fun onLogItemReceived(logItem: T)

    fun onLogProduceError(error: Throwable)

    fun setPaused(paused: Boolean)

    fun isPaused(): Boolean

    fun pause()

    fun resume()

    fun clear()

    fun cancel()
}

enum class TaskState: Event {
    IDLE, RUNNING, PAUSED
}