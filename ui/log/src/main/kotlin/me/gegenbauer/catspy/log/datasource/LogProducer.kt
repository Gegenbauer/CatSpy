package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.io.File

interface LogProducer {
    val dispatcher: CoroutineDispatcher

    val state: StateFlow<State>

    val tempFile: File

    val isRunning: Boolean
        get() = state.value == State.RUNNING

    val isActive: Boolean
        get() = state.value == State.RUNNING || state.value == State.PAUSED

    val isPaused: Boolean
        get() = state.value == State.PAUSED

    fun start(): Flow<Result<LogItem>>

    fun pause()

    fun resume()

    fun cancel()

    fun destroy()

    /**
     * RUNNING and PAUSED states can switch between each other.
     * Other states can only transition to the next state.
     * CREATED -> RUNNING -> PAUSED -> CANCELED -> DESTROYED
     */
    fun moveToState(state: State)

    enum class State {
        CREATED, RUNNING, PAUSED, CANCELED, COMPLETE
    }
}

class EmptyLogProducer(override val tempFile: File = File(EMPTY_STRING)) : LogProducer {
    override val dispatcher: CoroutineDispatcher
        get() = throw IllegalStateException("EmptyLogProducer should not use dispatcher")

    override val state: StateFlow<LogProducer.State> = MutableStateFlow(LogProducer.State.CREATED)

    override fun start(): Flow<Result<LogItem>> {
        return emptyFlow()
    }

    override fun pause() {
        // no-op
    }

    override fun resume() {
        // no-op
    }

    override fun cancel() {
        // no-op
    }

    override fun destroy() {
        // no-op
    }

    override fun moveToState(state: LogProducer.State) {
        // no-op
    }
}