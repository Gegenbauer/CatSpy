package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.*
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.util.concurrent.atomic.AtomicBoolean

class CoroutineSuspender(private val name: String = EMPTY_STRING) {
    private val enabled = AtomicBoolean(false)
    private val suspended = AtomicBoolean(false)

    private val scope = ModelScope()
    private var con: CancellableContinuation<Unit>? = null

    suspend fun checkSuspend(timeout: Long = Int.MAX_VALUE.toLong()) {
        if (enabled.get()) {
            suspendCancellableCoroutine { con ->
                GLog.d(TAG, "[$name] [checkSuspend] begin suspend")
                this.con = con
                scope.launch {
                    delay(timeout)
                    resumeSuspendPoint()
                }
                suspended.set(true)
                enabled.set(true)
            }
            GLog.d(TAG, "[$name] [checkSuspend] end suspend")
        }
    }

    @Synchronized
    fun enable() {
        enabled.set(true)
    }

    @Synchronized
    fun disable() {
        enabled.set(false)
        if (suspended.get().not()) {
            return
        }
        resumeSuspendPoint()
        scope.cancel()
        GLog.d(TAG, "[$name] [disable]")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun resumeSuspendPoint() {
        GLog.d(TAG, "[resumeSuspendPoint]")
        con?.resume(Unit, null)
        con = null
        suspended.set(false)
    }

    fun isPausing(): Boolean {
        return suspended.get()
    }

    companion object {
        private const val TAG = "CoroutineSuspender"
    }
}