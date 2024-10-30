package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import me.gegenbauer.catspy.log.ui.LogConfiguration

class CustomDeviceLogProducer(
    logConfiguration: LogConfiguration,
) : BaseCustomLogProducer(logConfiguration) {

    override fun start(): Flow<Result<LogItem>> {
        return flow {
            generateLogItems().forEachIndexed { _, log ->
                emit(Result.success(log))
            }
        }.onCompletion {
            moveToState(LogProducer.State.Complete)
        }.onStart {
            moveToState(LogProducer.State.intermediateRunning())
        }
    }

    override fun getSampleLogItem(): LogItem {
        val sampleLogParts = logParser.parse(logConfiguration.logMetaData.sample).toMutableList()
        sampleLogParts.add(PART_INDEX_PACKAGE, FAKE_PROCESS_NAME)
        return LogItem(0, sampleLogParts)
    }

    companion object {
        private const val PART_INDEX_PACKAGE = 2
        private const val FAKE_PROCESS_NAME = "fake_process"
    }
}
