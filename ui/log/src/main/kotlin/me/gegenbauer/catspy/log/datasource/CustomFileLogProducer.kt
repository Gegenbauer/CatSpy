package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.log.parse.LogParser
import java.io.File

class CustomFileLogProducer(
    logParser: LogParser,
   private val producer: () -> List<String>,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer(logParser) {

    override val tempFile: File = File("")

    override fun start(): Flow<Result<LogItem>> {
        return channelFlow {
            producer().forEachIndexed { index, line ->
                send(Result.success(LogItem(index, line, logParser.parse(line))))
            }
            invokeOnClose { moveToState(LogProducer.State.COMPLETE) }
        }
    }
}
