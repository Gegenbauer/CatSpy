package me.gegenbauer.catspy.log.datasource

import me.gegenbauer.catspy.log.filter.LogFilter

interface LogFilterable {
    val logFilter: LogFilter

    var fullMode: Boolean

    var bookmarkMode: Boolean

    var fullTableSelectedRows: List<Int>

    var filteredTableSelectedRows: List<Int>

    fun updateFilter(filter: LogFilter)

    fun updateFilter(filter: LogFilter, force: Boolean)
}