package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.log.datasource.LogProducer.State
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class BaseLogProducer : LogProducer {

    override val state: StateFlow<State>
        get() = sync.read { _state }

    protected val name: String = this.javaClass.simpleName

    protected val logNum = AtomicInteger(0)
    protected val suspender = CoroutineSuspender(name)

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Created)

    private val sync = ReentrantReadWriteLock()
    private var lastState: State = State.Created

    override fun pause() {
        suspender.enable()
        lastState = state.value
        moveToState(State.Paused)
    }

    override fun resume() {
        suspender.disable()
        moveToState(lastState)
    }

    override fun cancel() {
        suspender.disable()
        moveToState(State.Canceled)
    }

    override fun destroy() {
        suspender.disable()
    }

    override fun moveToState(state: State) {
        sync.write {
            when (this.state.value) {
                State.Created -> {
                    if (state is State.Running) {
                        this._state.value = state
                    }
                }

                is State.Running -> {
                    if (state in setOf(State.Paused, State.Canceled, State.Complete)) {
                        this._state.value = state
                    }
                }

                State.Paused -> {
                    if (state.javaClass in
                        setOf(
                            State.Running::class.java,
                            State.Canceled::class.java,
                            State.Complete::class.java
                        )
                    ) {
                        this._state.value = state
                    }
                }

                State.Canceled -> {
                    if (state == State.Complete) {
                        this._state.value = state
                    }
                }

                State.Complete -> {
                    // no-op
                }
            }
        }
    }
}