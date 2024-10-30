package me.gegenbauer.catspy.log.datasource

import FileLogSourceFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import me.gegenbauer.catspy.concurrency.GIO
import java.io.File

class FileGroupLogProducer(
    file: File,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer() {

    override val tempFile: File = file

    override fun start(): Flow<Result<LogItem>> {
        return flow {
            emitAll(FileLogSourceFactory().createSource(tempFile).read().map { Result.success(it) })
        }.onCompletion {
            moveToState(LogProducer.State.Complete)
        }.onStart {
            moveToState(LogProducer.State.intermediateRunning())
        }
    }

}
