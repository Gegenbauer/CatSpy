package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.log.datasource.LogProducer.State
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.metadata.toParseMetadata
import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.log.ui.LogConfiguration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class BaseLogProducer(
    protected val logConfiguration: LogConfiguration,
) : LogProducer {

    override val state: StateFlow<State>
        get() = sync.read { _state }

    protected val name: String = this.javaClass.simpleName

    protected val logNum = AtomicInteger(0)
    protected val suspender = CoroutineSuspender(name)

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.CREATED)

    private val sync = ReentrantReadWriteLock()

    private val logMetadata: LogMetadata
        get() = logConfiguration.logMetaData
    protected open val logParser: LogParser
        get() = logMetadata.parser

    protected open fun parseLog(line: String): List<String> {
        return logParser.parse(line, logMetadata.toParseMetadata())
    }

    override fun pause() {
        suspender.enable()
        moveToState(State.PAUSED)
    }

    override fun resume() {
        suspender.disable()
        moveToState(State.RUNNING)
    }

    override fun cancel() {
        suspender.disable()
        moveToState(State.CANCELED)
    }

    override fun destroy() {
        suspender.disable()
    }

    override fun moveToState(state: State) {
        sync.write {
            when (this.state.value) {
                State.CREATED -> {
                    if (state == State.RUNNING) {
                        this._state.value = State.RUNNING
                    }
                }

                State.RUNNING -> {
                    if (state in setOf(State.PAUSED, State.CANCELED, State.COMPLETE)) {
                        this._state.value = state
                    }
                }

                State.PAUSED -> {
                    if (state in setOf(State.RUNNING, State.CANCELED, State.COMPLETE)) {
                        this._state.value = state
                    }
                }

                State.CANCELED -> {
                    if (state == State.COMPLETE) {
                        this._state.value = state
                    }
                }

                State.COMPLETE -> {
                    // no-op
                }
            }
        }
    }
}