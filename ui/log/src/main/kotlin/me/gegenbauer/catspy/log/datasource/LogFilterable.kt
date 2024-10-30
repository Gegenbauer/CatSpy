package me.gegenbauer.catspy.log.datasource

import me.gegenbauer.catspy.log.filter.LogFilter

interface LogFilterable {
    val logFilter: LogFilter

    fun updateFilter(filter: LogFilter)
}