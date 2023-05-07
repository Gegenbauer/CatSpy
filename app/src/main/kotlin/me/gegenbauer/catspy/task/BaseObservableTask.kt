package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.log.GLog
import kotlin.coroutines.CoroutineContext

abstract class BaseObservableTask : Task {
    override val scope: CoroutineScope = object : ModelScope() {
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.IO
    }

    private val listeners = mutableSetOf<TaskListener>()

    override fun start() {
        GLog.d(name, "[start]")
        notifyStart()
        scope.launch { startInCoroutine() }
    }

    protected open suspend fun startInCoroutine() {
        // empty implementation
    }

    override fun pause() {
        GLog.d(name, "[pause]")
        notifyPause()
    }

    override fun resume() {
        GLog.d(name, "[resume]")
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

    protected fun notifyProgress(data: Any) {
        listeners.forEach { it.onProgress(this, data) }
    }

    protected fun notifyFinalResult(data: Any) {
        listeners.forEach { it.onFinalResult(this, data) }
    }

    override fun stop() {
        GLog.d(name, "[resume]")
        scope.cancel()
    }
}