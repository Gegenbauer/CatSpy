package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import me.gegenbauer.catspy.log.model.LogcatItem
import java.io.File

interface LogProducer {
    val dispatcher: CoroutineDispatcher

    val state: StateFlow<State>

    val tempFile: File

    val isRunning: Boolean
        get() = state.value == State.RUNNING

    val isPaused: Boolean
        get() = state.value == State.PAUSED

    fun start(): Flow<Result<LogcatItem>>

    fun pause()

    fun resume()

    fun cancel()

    fun destroy()

    /**
     * RUNNING 和 PAUSED 状态可以互相切换
     * 其余状态只能往后面的状态切换
     * CREATED -> RUNNING -> PAUSED -> CANCELED -> DESTROYED
     */
    fun moveToState(state: State)

    enum class State {
        CREATED, RUNNING, PAUSED, CANCELED, COMPLETE
    }
}

object EmptyLogProducer : LogProducer {
    override val dispatcher: CoroutineDispatcher
        get() = throw IllegalStateException("EmptyLogProducer should not use dispatcher")

    override val state: MutableStateFlow<LogProducer.State> = MutableStateFlow(LogProducer.State.CREATED)

    override val tempFile: File = File("")

    override fun start(): Flow<Result<LogcatItem>> {
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