package me.gegenbauer.catspy.task

open class OneTimeTask: BaseObservableTask(name = "OneTimeTask") {
    override suspend fun startInCoroutine() {
        // empty implementation
    }
}