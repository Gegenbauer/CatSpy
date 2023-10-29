package me.gegenbauer.catspy.task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import me.gegenbauer.catspy.concurrency.GIO
import java.io.BufferedInputStream
import java.io.File
import java.util.*

interface CommandExecutor {
    val processBuilder: CommandProcessBuilder

    val dispatcher: CoroutineDispatcher

    val name: String
        get() = processBuilder.name

    fun execute(): Flow<Result<String>>

    fun cancel()
}

class CommandExecutorImpl(
    override val processBuilder: CommandProcessBuilder,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO,
) : CommandExecutor {
    private var process: Process? = null

    private val processAlive: Boolean
        get() = process?.isAlive ?: false

    override fun execute(): Flow<Result<String>> {
        require(processAlive.not()) { "process is running" }
        return startProcess()
    }

    private fun startProcess(): Flow<Result<String>> {
        return channelFlow {
            runCatching {
                processBuilder.build().start().also {
                    readOutput(it)
                    readError(it)
                    process = it
                }
            }.onFailure {
                TaskLog.e(name, "[startProcess] $it")
                close(it)
            }
        }.flowOn(dispatcher)
    }

    private fun ProducerScope<Result<String>>.readOutput(process: Process) {
        async {
            Scanner(BufferedInputStream(process.inputStream)).use {
                while (it.hasNextLine()) {
                    val line = it.nextLine()
                    send(Result.success(line))
                }
            }
            TaskLog.d(name, "[readOutput] $process normally exit")
            close()
        }
    }

    private fun ProducerScope<Result<String>>.readError(process: Process) {
        async {
            val errorMessage = StringBuilder()
            process.errorStream.use { inputSteam ->
                inputSteam.readAllBytes().also { errorMessage.append(it.toString(Charsets.UTF_8)) }
            }
            if (errorMessage.isBlank()) return@async
            TaskLog.e(name, "[readError] $errorMessage")
            send(Result.failure(LogProduceProcessExecuteException(errorMessage.toString())))
            close()
        }
    }

    override fun cancel() {
        TaskLog.d(name, "[cancel]")
        process?.destroyForcibly()
    }
}

class LogProduceProcessExecuteException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    override fun toString(): String {
        return "LogProduceProcessExecuteException(message=$message)"
    }
}

class CommandProcessBuilder(
    private val commands: Array<String>,
    private val args: Array<String> = emptyArray(),
    private val envVars: Map<String, String> = emptyMap(),
    private val workingDir: File? = null,
    val name: String = "CommandProcess",
) {
    fun build(): ProcessBuilder {
        return ProcessBuilder(commands.toList() + args)
            .directory(workingDir)
            .also { it.environment().putAll(envVars) }
    }
}

fun String.toCommandArray(): Array<String> {
    return this.split(" ").toTypedArray()
}