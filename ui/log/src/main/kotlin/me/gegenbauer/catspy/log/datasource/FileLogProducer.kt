package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.log.model.LogcatItem
import java.io.File

class FileLogProducer(
    private val logPath: String,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer() {

    override val tempFile: File = File(logPath)

    override fun start(): Flow<Result<LogcatItem>> {
        if (logPath.isBlank()) {
            return emptyFlow()
        }
        if (tempFile.exists().not()) {
            return emptyFlow()
        }
        return channelFlow {
            moveToState(LogProducer.State.RUNNING)
            tempFile.inputStream().bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    suspender.checkSuspend()
                    send(Result.success(LogcatItem.from(line, logNum.getAndIncrement())))
                }
            }
            invokeOnClose { moveToState(LogProducer.State.COMPLETE) }
        }
    }
}