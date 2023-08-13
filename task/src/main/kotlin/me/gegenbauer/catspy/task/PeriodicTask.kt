package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import me.gegenbauer.catspy.concurrency.CPU

class PeriodicTask(
    var period: Long,
    name: String = "PeriodicTask",
    private val task: suspend () -> Unit = {},
    dispatcher: CoroutineDispatcher = Dispatchers.CPU
) : PausableTask(dispatcher, name) {

    override suspend fun startInCoroutine() {
        setRunning(true)
        super.startInCoroutine()
        repeat(Int.MAX_VALUE) {
            delay(period)
            addPausePoint()
            task()
            notifyRepeat()
            if (isCanceled) {
                setRunning(false)
                notifyStop()
                return
            }
        }
        setRunning(false)
    }

}