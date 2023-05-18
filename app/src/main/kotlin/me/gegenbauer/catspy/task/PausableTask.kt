package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import me.gegenbauer.catspy.concurrency.CancellablePause

/**
 * A task that can be paused and resumed.
 * @param dispatcher The dispatcher to run the task on. Note that can not pass in [Dispatchers.Main] because
 * it will cause the UI to freeze.
 */
abstract class PausableTask(dispatcher: CoroutineDispatcher = Dispatchers.IO, name: String) :
    BaseObservableTask(dispatcher, name) {

    protected val cancellablePause = CancellablePause(name)

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

    override fun isPausing(): Boolean {
        return cancellablePause.isPausing()
    }
}