package me.gegenbauer.catspy.task

class OneTimeTask<T>: BaseObservableTask(name = "OneTimeTask") {

    override suspend fun startInCoroutine() {
        super.startInCoroutine()
    }
}