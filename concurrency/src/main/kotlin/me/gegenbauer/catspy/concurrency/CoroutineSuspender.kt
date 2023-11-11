package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import me.gegenbauer.catspy.glog.GLog
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class CoroutineSuspender(private val name: String = "") {
    private val enabled = AtomicBoolean(false)
    private val suspended = AtomicBoolean(false)

    private var con: CancellableContinuation<Unit>? = null
    private var timer: Timer? = null

    suspend fun checkSuspend(timeout: Long = Int.MAX_VALUE.toLong()) {
        Unit.takeIf { enabled.get().not() } ?: suspendCancellableCoroutine { con ->
            GLog.d(TAG, "[$name] [checkSuspend] start suspend")
            this.con = con
            timer = Timer().also {
                it.schedule(object : TimerTask() {
                    override fun run() {
                        resumeSuspendPoint()
                        timer = null
                    }
                }, timeout)
            }
            suspended.set(true)
            enabled.set(true)
        }
    }

    fun enable() {
        enabled.set(true)
    }

    fun disable() {
        enabled.set(false)
        if (suspended.get().not()) {
            return
        }
        resumeSuspendPoint()
        timer?.cancel()
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