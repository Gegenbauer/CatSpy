package me.gegenbauer.catspy.script.executor

import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.shell.v2.ShellCommandResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import me.gegenbauer.catspy.script.model.Script

interface ScriptExecutor {
    val script: Script

    val dispatcher: CoroutineDispatcher

    fun execute(device: Device): Flow<ShellCommandResult>

    fun cancel()
}