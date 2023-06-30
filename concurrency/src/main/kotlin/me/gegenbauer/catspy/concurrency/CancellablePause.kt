package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import me.gegenbauer.catspy.log.GLog
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class CancellablePause(private val name: String = "") {
    private val enablePause = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private var cancellableContinuation: CancellableContinuation<Unit>? = null
    private var timer: Timer? = null

    suspend fun addPausePoint(timeout: Long = Int.MAX_VALUE.toLong()) {
        if (enablePause.get().not()) {
            return
        }
        return suspendCancellableCoroutine {
            GLog.d(TAG, "[$name] [pause]")
            cancellableContinuation = it
            paused.set(true)
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
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
        enablePause.set(false)
        if (paused.get().not()) {
            return
        }
        resumeCurrentPausePoint()
        timer?.cancel()
        GLog.d("CancellablePause", "[$name] [resume]")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun resumeCurrentPausePoint() {
        cancellableContinuation?.resume(Unit, null)
        cancellableContinuation = null
        paused.set(false)
    }

    fun isPausing(): Boolean {
        return paused.get()
    }

    companion object {
        private const val TAG = "CancellablePause"
    }
}