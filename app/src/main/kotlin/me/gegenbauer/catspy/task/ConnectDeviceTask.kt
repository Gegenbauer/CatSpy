package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.ModelScope

class ConnectDeviceTask() : BaseObservableTask(name = "ConnectDeviceTask") {
    override val scope: CoroutineScope = ModelScope()

    override fun start() {
        scope.launch {

        }
    }

    private suspend fun connectDevice() {

    }
}