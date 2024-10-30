package me.gegenbauer.catspy.log.datasource

import me.gegenbauer.catspy.context.Context

interface ILogViewModel : Context, LogFilterable {

    val fullLogObservables: LogProducerManager.LogObservables

    val filteredLogObservables: LogProducerManager.LogObservables
}