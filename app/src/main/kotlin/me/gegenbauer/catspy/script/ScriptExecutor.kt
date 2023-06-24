package me.gegenbauer.catspy.script

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.task.CommandTask
import me.gegenbauer.catspy.task.TaskManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class ScriptExecutor(private val taskManager: TaskManager, private val script: String) : CommandTask(script.toCommand()) {
    private val countDownLatch = CountDownLatch(1)
    private val cachedOutput = StringBuilder()
    private var output: AtomicReference<String> = AtomicReference(null)

    override suspend fun onReceiveOutput(line: String) {
        super.onReceiveOutput(line)
        cachedOutput.appendLine(line)
    }

    override fun onProcessEnd() {
        super.onProcessEnd()
        output.set(cachedOutput.toString())
        countDownLatch.countDown()
    }

    suspend fun executeAndGetResult() = withContext(Dispatchers.IO) {
        taskManager.exec(this@ScriptExecutor)
        if (output.get() == null) {
            countDownLatch.await()
        }
        output.get() ?: ""
    }

}

fun String.toCommand(): Array<String> {
    return this.split(" ").toTypedArray()
}