package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import me.gegenbauer.catspy.concurrency.CPU

class PeriodicTask(
    private val period: Long,
    private val task: Runnable,
    name: String = "PeriodicTask",
    dispatcher: CoroutineDispatcher = Dispatchers.CPU
) : PausableTask(dispatcher, name) {

    override suspend fun startInCoroutine() {
        super.startInCoroutine()
        repeat(Int.MAX_VALUE) {
            delay(period)
            addPausePoint()
            task.run()
        }
    }
}