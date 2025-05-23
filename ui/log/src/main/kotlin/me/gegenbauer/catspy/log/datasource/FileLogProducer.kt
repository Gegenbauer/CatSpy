package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.log.ui.LogConfiguration
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.openZip
import java.io.File

class FileLogProducer(
    private val logPath: String,
    logConfiguration: LogConfiguration,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer(logConfiguration) {

    override val tempFile: File = File(logPath)

    private var flowScope: ProducerScope<*>? = null

    override fun start(): Flow<Result<LogItem>> {
        if (logPath.isBlank() || tempFile.exists().not()) {
            return emptyFlow()
        }
        return channelFlow {
            flowScope = this

            moveToState(LogProducer.State.RUNNING)
            if (tempFile.extension.equals("zip", true)) {
                val fileSystem = FileSystem.SYSTEM
                val zipFileSystem = fileSystem.openZip(tempFile.toOkioPath())
                val files = zipFileSystem.listOrNull("/".toPath())
                if (files != null) {
                    zipFileSystem.read(files.first()) {
                        processLines(readUtf8().lineSequence())
                    }
                }
            } else {
                tempFile.inputStream().bufferedReader().use { reader ->
                    processLines(reader.lineSequence())
                }
            }
            invokeOnClose { moveToState(LogProducer.State.COMPLETE) }
        }.flowOn(dispatcher)
    }

    private suspend fun ProducerScope<Result<LogItem>>.processLines(lines: Sequence<String>) {
        lines.forEach { line ->
            suspender.checkSuspend()
            if (line.isBlank()) {
                return@forEach
            }
            val num = logNum.getAndIncrement()
            send(Result.success(LogItem(num, parseLog(line))))
        }
    }

    private val File.isZipFile: Boolean
        get() = extension.equals("zip", true)

    private val File.isGzipFile: Boolean
        get() = tempFile.extension.equals("gz", true)

    override fun cancel() {
        super.cancel()
        flowScope?.cancel()
    }
}