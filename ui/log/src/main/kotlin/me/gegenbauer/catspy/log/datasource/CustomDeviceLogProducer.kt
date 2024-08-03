package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.log.parse.LogParser
import java.io.File

class CustomDeviceLogProducer(
    logParser: LogParser,
    private val producer: () -> List<String>,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer(logParser) {

    override val tempFile: File = File("")

    override fun start(): Flow<Result<LogItem>> {
        return channelFlow {
            producer().forEachIndexed { index, line ->
                val parts = logParser.parse(line).toMutableList()
                parts.add(PART_INDEX_PACKAGE, FAKE_PROCESS_NAME)
                send(Result.success(LogItem(index, line, parts)))
            }
            invokeOnClose { moveToState(LogProducer.State.COMPLETE) }
        }
    }

    companion object {
        private const val PART_INDEX_PACKAGE = 2
        private const val FAKE_PROCESS_NAME = "fake_process"
    }
}
