package me.gegenbauer.catspy.script.executor

import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.shell.v2.ShellCommandResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import me.gegenbauer.catspy.script.model.Script
import me.gegenbauer.catspy.task.CommandTask
import me.gegenbauer.catspy.task.TaskManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class CommandExecutor(
    private val taskManager: TaskManager,
    override val script: Script,
    override val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CommandTask(script.getCommand().toCommand()), ScriptExecutor {
    private val countDownLatch = CountDownLatch(1)
    private val cachedOutput = StringBuilder()
    private val output: AtomicReference<String> = AtomicReference(null)

    override suspend fun onReceiveOutput(line: String) {
        super.onReceiveOutput(line)
        cachedOutput.appendLine(line)
    }

    override fun onProcessEnd() {
        super.onProcessEnd()
        output.set(cachedOutput.toString())
        countDownLatch.countDown()
    }

    override fun execute(device: Device): Flow<ShellCommandResult> = flow {
        cachedOutput.clear()
        taskManager.exec(this@CommandExecutor)
        if (output.get() == null) {
            countDownLatch.await()
        }
        emit(ShellCommandResult((output.get() ?: "").toByteArray(), byteArrayOf(0), 0))
    }.flowOn(dispatcher)

}

fun String.toCommand(): Array<String> {
    return this.split(" ").toTypedArray()
}