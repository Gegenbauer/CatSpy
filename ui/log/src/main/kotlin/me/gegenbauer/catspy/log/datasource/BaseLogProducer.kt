package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.log.parse.LogParser
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class BaseLogProducer(protected val logParser: LogParser) : LogProducer {

    override val state: StateFlow<LogProducer.State>
        get() = sync.read { _state }

    protected val logNum = AtomicInteger(0)
    protected val suspender = CoroutineSuspender("LogProducer")

    private val _state: MutableStateFlow<LogProducer.State> = MutableStateFlow(LogProducer.State.CREATED)

    private val sync = ReentrantReadWriteLock()

    override fun pause() {
        suspender.enable()
        moveToState(LogProducer.State.PAUSED)
    }

    override fun resume() {
        suspender.disable()
        moveToState(LogProducer.State.RUNNING)
    }

    override fun cancel() {
        suspender.disable()
        moveToState(LogProducer.State.CANCELED)
    }

    override fun destroy() {
        suspender.disable()
    }

    override fun moveToState(state: LogProducer.State) {
        sync.write {
            when (this.state.value) {
                LogProducer.State.CREATED -> {
                    if (state == LogProducer.State.RUNNING) {
                        this._state.value = LogProducer.State.RUNNING
                    }
                }

                LogProducer.State.RUNNING -> {
                    if (state == LogProducer.State.PAUSED || state == LogProducer.State.CANCELED || state == LogProducer.State.COMPLETE) {
                        this._state.value = state
                    }
                }

                LogProducer.State.PAUSED -> {
                    if (state == LogProducer.State.RUNNING || state == LogProducer.State.CANCELED || state == LogProducer.State.COMPLETE) {
                        this._state.value = state
                    }
                }

                LogProducer.State.CANCELED -> {
                    if (state == LogProducer.State.COMPLETE) {
                        this._state.value = state
                    }
                }

                LogProducer.State.COMPLETE -> {
                    // no-op
                }
            }
        }
    }

    companion object {
        private const val TAG = "BaseLogProducer"
    }
}