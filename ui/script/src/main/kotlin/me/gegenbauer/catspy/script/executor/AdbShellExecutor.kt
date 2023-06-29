package me.gegenbauer.catspy.script.executor

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.script.model.Script

class AdbShellExecutor(
    override val script: Script,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : ScriptExecutor {
    private val adbClient = AndroidDebugBridgeClientFactory().run {
        build()
    }

    override fun execute(device: Device): Flow<ShellCommandResult> = flow {
        val response = adbClient.execute(
            request = ShellCommandRequest(script.sourceCode),
            serial = device.serial
        )
        emit(response)
    }

    override fun cancel() {

    }

}