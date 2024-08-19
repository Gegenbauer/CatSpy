package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import me.gegenbauer.catspy.log.ui.LogConfiguration

class CustomDeviceLogProducer(
    logConfiguration: LogConfiguration,
) : BaseCustomLogProducer(logConfiguration) {

    override fun start(): Flow<Result<LogItem>> {
        return channelFlow {
            generateLogItems().forEachIndexed { _, log ->
                send(Result.success(log))
            }
            invokeOnClose { moveToState(LogProducer.State.COMPLETE) }
        }
    }

    override fun getSampleLogItem(): LogItem {
        val sampleLogParts = logParser.parse(logConfiguration.logMetaData.sample).toMutableList()
        sampleLogParts.add(PART_INDEX_PACKAGE, FAKE_PROCESS_NAME)
        return LogItem(0, logConfiguration.logMetaData.sample, sampleLogParts)
    }

    companion object {
        private const val PART_INDEX_PACKAGE = 2
        private const val FAKE_PROCESS_NAME = "fake_process"
    }
}
