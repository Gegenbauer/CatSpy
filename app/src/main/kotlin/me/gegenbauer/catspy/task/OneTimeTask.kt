package me.gegenbauer.catspy.task

class OneTimeTask<T>: BaseObservableTask() {
    override val name: String = "OneTimeTask"

    override suspend fun startInCoroutine() {
        super.startInCoroutine()
    }
}