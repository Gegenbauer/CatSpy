package me.gegenbauer.catspy.task

import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ModelScope
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

abstract class BaseObservableTask(dispatcher: CoroutineDispatcher = Dispatchers.GIO, override val name: String) : Task {
    override val scope: CoroutineScope = object : ModelScope() {
        override val coroutineContext: CoroutineContext
            get() = dispatcher + Job()
    }

    override val isRunning: Boolean
        get() = _isRunning.get()

    override val isCanceled: Boolean
        get() = _isCanceled.get()

    private val listeners = Collections.synchronizedCollection(mutableSetOf<TaskListener>())
    private val _isRunning = AtomicBoolean(false)
    private val _isCanceled = AtomicBoolean(false)

    override fun start() {
        TaskLog.d(name, "[start]")
        _isCanceled.set(false)
        notifyStart()
        scope.launch { startInCoroutine() }
    }

    protected open suspend fun startInCoroutine() {
        // empty implementation
    }

    override fun pause() {
        TaskLog.d(name, "[pause]")
        notifyPause()
    }

    open fun isPausing(): Boolean {
        return false
    }

    protected fun setRunning(running: Boolean) {
        this._isRunning.set(running)
    }

    override fun resume() {
        TaskLog.d(name, "[resume]")
        notifyResume()
    }

    override fun addListener(taskListener: TaskListener) {
        listeners.add(taskListener)
    }

    override fun removeListener(taskListener: TaskListener) {
        listeners.remove(taskListener)
    }

    protected fun notifyStart() {
        listeners.toList().forEach { it.onStart(this) }
    }

    protected fun notifyPause() {
        listeners.toList().forEach { it.onPause(this) }
    }

    protected fun notifyResume() {
        listeners.toList().forEach { it.onResume(this) }
    }

    protected fun notifyStop() {
        listeners.toList().forEach { it.onStop(this) }
    }

    protected fun notifyCancel() {
        listeners.toList().forEach { it.onCancel(this) }
    }

    protected fun notifyProgress(data: Any = Any()) {
        listeners.toList().forEach { it.onProgress(this, data) }
    }

    protected fun notifyRepeat() {
        listeners.toList().forEach { it.onRepeat(this) }
    }

    protected fun notifyFinalResult(data: Any = emptyResult) {
        listeners.toList().forEach { it.onFinalResult(this, data) }
    }

    protected fun notifyError(t: Throwable) {
        listeners.toList().forEach { it.onError(this, t) }
    }

    override fun cancel() {
        TaskLog.d(name, "[cancel]")
        notifyCancel()
        scope.cancel()
        _isCanceled.set(true)
    }

    companion object {
        val emptyResult = Any()
    }
}