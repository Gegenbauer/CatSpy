package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class RateLimiterCallbackSchedule(private val dispatcher: CoroutineContext, private val delay: Long = 10): CallbackSchedule {
    private val scope = ModelScope()
    private var job: Job? = null

    override fun schedule(callback: () -> Unit) {
        job?.cancel()
        job = scope.launch(dispatcher) {
            delay(delay)
            callback()
        }
    }
}

interface CallbackSchedule {
    fun schedule(callback: () -> Unit)
}