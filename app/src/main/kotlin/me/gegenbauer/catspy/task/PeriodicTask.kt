package me.gegenbauer.catspy.task

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import me.gegenbauer.catspy.concurrency.CPU

class PeriodicTask(private val period: Long, private var task: Runnable? = null) : PausableTask(Dispatchers.CPU) {
    override val name: String = "PeriodicTask"

    override suspend fun startInCoroutine() {
        super.startInCoroutine()
        repeat(Int.MAX_VALUE) {
            delay(period)
            addPausePoint()
            task?.run()
        }
    }
}