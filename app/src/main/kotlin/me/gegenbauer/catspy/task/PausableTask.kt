package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import me.gegenbauer.catspy.concurrency.CancellablePause

abstract class PausableTask(dispatcher: CoroutineDispatcher = Dispatchers.IO): BaseObservableTask(dispatcher) {
    private val cancellablePause = CancellablePause()

    override fun pause() {
        super.pause()
        cancellablePause.pause()
    }

    override fun resume() {
        super.resume()
        cancellablePause.resume()
    }

    protected suspend fun addPausePoint() {
        cancellablePause.addPausePoint()
    }

    override fun cancel() {
        super.cancel()
        cancellablePause.resume()
    }
}