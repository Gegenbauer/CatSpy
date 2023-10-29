package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.flow.Flow
import me.gegenbauer.catspy.log.model.LogcatItem
import me.gegenbauer.catspy.log.model.LogcatFilter

interface LogcatDatasource {
    val logcatProducer: LogProducer

    fun startCollectLog(): Flow<Result<List<LogcatItem>>>

    fun startFilterLog(filter: LogcatFilter): Flow<Result<List<LogcatItem>>>

    fun pause()

    fun resume()

    fun cancel()

    fun destroy()
}