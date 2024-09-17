package me.gegenbauer.catspy.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IgnoreFastCallbackScheduler(
    private val dispatcher: CoroutineDispatcher,
    private val delay: Long = 10
) : CallbackSchedule {
    private val scope = ModelScope()
    private var job: Job? = null

    override fun schedule(callback: Callback) {
        val lastJob = job
        job = scope.launch(dispatcher) {
            lastJob?.cancelAndJoin()
            delay(delay)
            callback.invoke()
        }
    }

    override fun cancel() {
        scope.cancel()
    }
}

interface CallbackSchedule {
    fun schedule(callback: Callback)

    fun cancel()
}

fun interface Callback {
    fun invoke()
}