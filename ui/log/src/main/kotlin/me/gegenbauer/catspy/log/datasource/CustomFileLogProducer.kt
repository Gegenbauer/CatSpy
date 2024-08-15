package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import me.gegenbauer.catspy.log.ui.LogConfiguration

class CustomFileLogProducer(
    logConfiguration: LogConfiguration,
) : BaseCustomLogProducer(logConfiguration) {

    override fun start(): Flow<Result<LogItem>> {
        return channelFlow {
            generateLogItems().forEach { log ->
                send(Result.success(log))
            }
            invokeOnClose { moveToState(LogProducer.State.COMPLETE) }
        }
    }
}
