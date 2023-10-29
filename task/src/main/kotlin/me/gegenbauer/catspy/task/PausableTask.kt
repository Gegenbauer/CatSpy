package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import me.gegenbauer.catspy.concurrency.CoroutineSuspender
import me.gegenbauer.catspy.concurrency.GIO

/**
 * A task that can be paused and resumed.
 * @param dispatcher The dispatcher to run the task on. Note that can not pass in [Dispatchers.Main] because
 * it will cause the UI to freeze.
 */
abstract class PausableTask(dispatcher: CoroutineDispatcher = Dispatchers.GIO, name: String) :
    BaseObservableTask(dispatcher, name) {

    protected val coroutineSuspender = CoroutineSuspender(name)

    override fun pause() {
        super.pause()
        coroutineSuspender.enable()
    }

    override fun resume() {
        super.resume()
        coroutineSuspender.disable()
    }

    protected suspend fun addPausePoint() {
        coroutineSuspender.checkSuspend()
    }

    override fun cancel() {
        super.cancel()
        coroutineSuspender.disable()
    }

    override fun isPausing(): Boolean {
        return coroutineSuspender.isPausing()
    }
}