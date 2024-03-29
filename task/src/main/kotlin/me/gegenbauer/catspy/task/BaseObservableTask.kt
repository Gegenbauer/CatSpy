package me.gegenbauer.catspy.task

import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.GIO
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

abstract class BaseObservableTask(dispatcher: CoroutineDispatcher = Dispatchers.GIO, override val name: String) : Task {
    override val scope: CoroutineScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext = dispatcher + SupervisorJob()
    }

    override val isRunning: Boolean
        get() = _isRunning.get()

    override val isCanceled: Boolean
        get() = _isCanceled.get()

    private val _listeners = Collections.synchronizedCollection(mutableSetOf<TaskListener>())
    private val _isRunning = AtomicBoolean(false)
    private val _isCanceled = AtomicBoolean(false)

    override val listeners: List<TaskListener>
        get() = _listeners.toList()

    override fun start() {
        _isCanceled.set(false)
        scope.launch {
            TaskLog.d(name, "[start]")
            setRunning(true)
            notifyStart()
            runCatching { startInCoroutine() }
            setRunning(false)
            notifyStop()
            TaskLog.d(name, "[stop]")
        }
    }

    protected abstract suspend fun startInCoroutine()

    override fun pause() {
        TaskLog.d(name, "[pause]")
        notifyPause()
    }

    open fun isPausing(): Boolean {
        return false
    }

    private fun setRunning(running: Boolean) {
        this._isRunning.set(running)
    }

    override fun resume() {
        TaskLog.d(name, "[resume]")
        notifyResume()
    }

    override fun addListener(taskListener: TaskListener) {
        _listeners.add(taskListener)
    }

    override fun removeListener(taskListener: TaskListener) {
        _listeners.remove(taskListener)
    }

    protected fun notifyStart() {
        listeners.forEach { dispatchOrNot(it) { it.onStart(this) } }
    }

    protected fun notifyPause() {
        listeners.forEach { dispatchOrNot(it) { it.onPause(this) } }
    }

    protected fun notifyResume() {
        listeners.forEach { dispatchOrNot(it) { it.onResume(this) } }
    }

    protected fun notifyStop() {
        listeners.forEach { dispatchOrNot(it) { it.onStop(this) } }
    }

    protected fun notifyCancel() {
        listeners.forEach { dispatchOrNot(it) { it.onCancel(this) } }
    }

    protected fun notifyProgress(data: Any = Any()) {
        listeners.forEach { it.onProgress(this, data) }
    }

    protected fun notifyRepeat() {
        listeners.forEach { it.onRepeat(this) }
    }

    protected fun notifyFinalResult(data: Any = emptyResult) {
        listeners.forEach { dispatchOrNot(it) { it.onFinalResult(this, data) } }
    }

    protected fun notifyError(t: Throwable) {
        listeners.forEach { dispatchOrNot(it) { it.onError(this, t) } }
    }

    private fun dispatchOrNot(listener: TaskListener, block: () -> Unit) {
        block.takeIf { listener.dispatcher == null }?.invoke()
            ?: scope.launch(listener.dispatcher!!) { block() }
    }

    override fun cancel() {
        TaskLog.d(name, "[cancel]")
        notifyCancel()
        scope.cancel()
        _isCanceled.set(true)
    }

    override fun toString(): String {
        return "${name}_${hashCode()}"
    }

    companion object {
        val emptyResult = Any()
    }
}