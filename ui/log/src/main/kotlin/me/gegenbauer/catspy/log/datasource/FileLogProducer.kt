package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.log.parse.LogParser
import java.io.File

class FileLogProducer(
    private val logPath: String,
    logParser: LogParser,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer(logParser) {

    override val tempFile: File = File(logPath)

    override fun start(): Flow<Result<LogItem>> {
        if (logPath.isBlank() || tempFile.exists().not()) {
            return emptyFlow()
        }
        return channelFlow {
            moveToState(LogProducer.State.RUNNING)
            tempFile.inputStream().bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    suspender.checkSuspend()
                    val num = logNum.getAndIncrement()
                    send(Result.success(LogItem(num, line, logParser.parse(line))))
                }
            }
            invokeOnClose { moveToState(LogProducer.State.COMPLETE) }
        }
    }
}