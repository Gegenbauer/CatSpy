package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IgnoreFastCallbackScheduler(private val dispatcher: CoroutineDispatcher, private val delay: Long = 10): CallbackSchedule {
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

fun interface CallbackSchedule {
    fun schedule(callback: Callback)
}

fun interface Callback {
    fun invoke()
}