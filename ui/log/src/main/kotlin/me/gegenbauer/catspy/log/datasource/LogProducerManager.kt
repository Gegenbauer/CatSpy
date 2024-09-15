package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.Event
import me.gegenbauer.catspy.view.state.ListState
import java.io.File

interface LogProducerManager {

    val eventFlow: SharedFlow<Event>

    val fullLogObservables: LogObservables

    val filteredLogObservables: LogObservables

    val tempLogFile: File

    val device: String

    fun startProduceDeviceLog(device: String)

    fun startProduceFileLog(file: String)

    fun startProduceCustomFileLog()

    fun startProduceCustomDeviceLog()

    fun setPaused(paused: Boolean)

    fun isPaused(): Boolean

    fun pause()

    fun isActive(): Boolean

    fun resume()

    fun clear()

    fun cancel()

    class LogObservables(
        val itemsFlow: StateFlow<List<LogItem>>,
        val listState: StateFlow<ListState>,
        val repaintEventFlow: SharedFlow<Event>
    )
}

enum class TaskState: Event {
    IDLE, RUNNING, PAUSED
}
