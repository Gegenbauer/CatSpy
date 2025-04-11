package me.gegenbauer.catspy.log.datasource

import me.gegenbauer.catspy.log.filter.LogFilter

interface LogFilterable {
    val logFilter: LogFilter

    val fullTableSelectedRows: MutableSet<Int>

    val filteredTableSelectedRows: MutableSet<Int>

    fun updateFilter(filter: LogFilter)
}