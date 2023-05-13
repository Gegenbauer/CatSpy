package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class IgnoreFastCallbackScheduler(private val dispatcher: CoroutineContext, private val delay: Long = 10): CallbackSchedule {
    private val scope = ModelScope()
    private var job: Job? = null

    override fun schedule(callback: Callback) {
        job?.cancel()
        job = scope.launch(dispatcher) {
            delay(delay)
            callback.invoke()
        }
    }
}

interface CallbackSchedule {
    fun schedule(callback: Callback)
}

fun interface Callback {
    fun invoke()
}