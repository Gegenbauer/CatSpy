package me.gegenbauer.catspy.task

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import java.io.BufferedInputStream
import java.io.File
import java.util.*

// TODO 增加关于接受输出和处理输出的速度比较，避免出现数据积压然后遗漏的情况
open class CommandTask(
    protected val commands: Array<String>,
    private val args: Array<String> = arrayOf(),
    private val envVars: Map<String, String> = mapOf(),
    name: String = "CommandTask"
) : PausableTask(name = name) {

    protected var process: Process? = null
    protected var workingDirectory: File? = null

    override suspend fun startInCoroutine() {
        super.startInCoroutine()
        execute().collect {
            addPausePoint()
            onReceiveOutput(it)
        }
        onProcessEnd()
    }

    protected open suspend fun onReceiveOutput(line: String) {
        notifyProgress(line)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    protected open fun execute(): Flow<String> {
        if (process?.isAlive == true) {
            TaskLog.w(name, "[execute] , CommandExecutor is now executing!")
            return emptyFlow()
        }
        return runCatching {
            // flow buffer size is 20MB
            channelFlow {
                async {
                    val builder = ProcessBuilder(*commands)
                        .directory(workingDirectory)
                    builder.command().addAll(args)
                    builder.environment().putAll(envVars)
                    val process = builder.start().apply { this@CommandTask.process = this }
                    onProcessStart()
                    readOutput(process)
                    readError(process)
                }
            }.buffer(8 * 1024 * 1024 * 50, BufferOverflow.DROP_OLDEST)
        }.onFailure {
            TaskLog.e(name, "[execute]", it)
            notifyError(it)
        }.getOrElse { emptyFlow() }
    }

    protected open fun onProcessStart() {
        TaskLog.d(name, "[onProcessStart] $process")
        setRunning(true)
    }

    protected open fun onProcessEnd() {
        TaskLog.d(name, "[onProcessEnd] $process")
        setRunning(false)
        notifyStop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun ProducerScope<String>.readOutput(process: Process) {
        async {
            Scanner(BufferedInputStream(process.inputStream)).use {
                while (it.hasNextLine()) {
                    send(it.nextLine())
                }
            }
            TaskLog.d(name, "[readOutput] $process normally exit")
            close()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun ProducerScope<String>.readError(process: Process) {
        async {
            process.errorStream.readAllBytes().toString(Charsets.UTF_8).let {
                if (it.isNotEmpty()) {
                    TaskLog.e(name, "[readError] $process, $it")
                    notifyError(IllegalStateException(it))
                }
            }
        }
    }

    override fun cancel() {
        super.cancel()
        TaskLog.d(name, "[cancel] kill process $process")
        process?.destroyForcibly()
    }

}