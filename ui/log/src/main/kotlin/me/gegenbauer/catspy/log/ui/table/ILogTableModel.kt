package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.log.model.LogcatLogItem
import me.gegenbauer.catspy.log.model.LogcatRealTimeFilter
import me.gegenbauer.catspy.view.filter.FilterItem

interface ILogTableModel {
    var highlightFilterItem: FilterItem

    val boldTag: Boolean

    val boldPid: Boolean

    val boldTid: Boolean

    fun addLogTableModelListener(eventListener: LogTableModelListener)

    fun getItemInCurrentPage(row: Int): LogcatLogItem

    fun getLogFilter(): LogcatRealTimeFilter

    fun getRowIndex(lineNumber: Int): Int

    fun isFullLogTable(): Boolean

}