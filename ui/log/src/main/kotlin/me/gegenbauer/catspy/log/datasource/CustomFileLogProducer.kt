package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import me.gegenbauer.catspy.log.ui.LogConfiguration

class CustomFileLogProducer(
    logConfiguration: LogConfiguration,
) : BaseCustomLogProducer(logConfiguration) {

    override fun start(): Flow<Result<LogItem>> {
        return flow {
            generateLogItems().forEach { log ->
                emit(Result.success(log))
            }
        }.onCompletion {
            moveToState(LogProducer.State.Complete)
        }.onStart {
            moveToState(LogProducer.State.intermediateRunning())
        }
    }
}
