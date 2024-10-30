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
        get() = state.value is State.Running

    val isActive: Boolean
        get() = state.value is State.Running || state.value == State.Paused

    val isPaused: Boolean
        get() = state.value == State.Paused

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

    sealed class State {
        object Created : State()
        class Running(val isIntermediate: Boolean = false, val progress: Int = 0, val max: Int = 100) : State()
        object Paused : State()
        object Canceled : State()
        object Complete : State()

        companion object {
            fun running(progress: Int, max: Int): Running {
                return Running(isIntermediate = false, progress = progress, max = max)
            }

            fun intermediateRunning(): Running {
                return Running(isIntermediate = true)
            }
        }
    }
}

object EmptyLogProducer : LogProducer {
    override val dispatcher: CoroutineDispatcher
        get() = throw IllegalStateException("EmptyLogProducer should not use dispatcher")

    override val state: StateFlow<LogProducer.State> = MutableStateFlow(LogProducer.State.Created)

    override val tempFile: File = File(EMPTY_STRING)

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