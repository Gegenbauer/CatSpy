package me.gegenbauer.catspy.task

import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.ModelScope
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

abstract class BaseObservableTask(dispatcher: CoroutineDispatcher = Dispatchers.GIO, override val name: String) : Task {
    override val scope: CoroutineScope = object : ModelScope() {
        override val coroutineContext: CoroutineContext
            get() = dispatcher + Job()
    }

    private val listeners = mutableSetOf<TaskListener>()
    private val running = AtomicBoolean(false)

    override fun start() {
        TaskLog.d(name, "[start]")
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
        this.running.set(running)
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
        listeners.forEach { it.onStart(this) }
    }

    protected fun notifyPause() {
        listeners.forEach { it.onPause(this) }
    }

    protected fun notifyResume() {
        listeners.forEach { it.onResume(this) }
    }

    protected fun notifyStop() {
        listeners.forEach { it.onStop(this) }
    }

    protected fun notifyCancel() {
        listeners.forEach { it.onCancel(this) }
    }

    protected fun notifyProgress(data: Any = Any()) {
        listeners.forEach { it.onProgress(this, data) }
    }

    protected fun notifyRepeat() {
        listeners.forEach { it.onRepeat(this) }
    }

    protected fun notifyFinalResult(data: Any = emptyResult) {
        listeners.forEach { it.onFinalResult(this, data) }
    }

    protected fun notifyError(t: Throwable) {
        listeners.forEach { it.onError(this, t) }
    }

    override fun isRunning(): Boolean {
        return running.get()
    }

    override fun cancel() {
        TaskLog.d(name, "[cancel]")
        notifyCancel()
        scope.cancel()
    }

    companion object {
        val emptyResult = Any()
    }
}