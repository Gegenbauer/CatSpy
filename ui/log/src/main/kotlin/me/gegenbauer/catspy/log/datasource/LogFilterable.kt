package me.gegenbauer.catspy.log.datasource

import me.gegenbauer.catspy.log.model.LogcatFilter

interface LogFilterable {
    val logcatFilter: LogcatFilter

    var fullMode: Boolean

    var bookmarkMode: Boolean

    var fullTableSelectedRows: List<Int>

    var filteredTableSelectedRows: List<Int>

    fun updateFilter(filter: LogcatFilter)
}