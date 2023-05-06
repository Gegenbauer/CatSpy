package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import me.gegenbauer.catspy.log.GLog
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class CancellablePause {
    private val enablePause = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private var cancellableContinuation: CancellableContinuation<Unit>? = null
    private var timer: Timer? = null

    suspend fun addPausePoint(timeout: Long = Int.MAX_VALUE.toLong()) {
        if (enablePause.get().not()) {
            return
        }
        return suspendCancellableCoroutine {
            cancellableContinuation = it
            paused.set(true)
            GLog.d(TAG, "[pause] start cancellableContinuation=$cancellableContinuation")
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    GLog.d(TAG, "[pause] end pause due to timeout")
                    resumeCurrentPausePoint()
                    timer = null
                }
            }, timeout)
            enablePause.set(true)
        }
    }

    fun pause() {
        enablePause.set(true)
    }

    fun resume() {
        if (paused.get().not()) {
            return
        }
        resumeCurrentPausePoint()
        timer?.cancel()
        enablePause.set(false)
    }

    private fun resumeCurrentPausePoint() {
        cancellableContinuation?.resume(Unit, null)
        cancellableContinuation = null
        paused.set(false)
    }

    companion object {
        private const val TAG = "CancellablePause"
    }
}