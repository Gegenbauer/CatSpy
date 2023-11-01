package me.gegenbauer.catspy.log.ui.table

import kotlinx.coroutines.flow.Flow
import me.gegenbauer.catspy.log.model.LogcatItem
import me.gegenbauer.catspy.log.model.LogcatFilter
import me.gegenbauer.catspy.view.filter.FilterItem

interface ILogTableModel {
    var highlightFilterItem: FilterItem

    val boldTag: Boolean

    val boldPid: Boolean

    val boldTid: Boolean

    var selectedRows: List<Int>

    val logFlow: Flow<List<LogcatItem>>

    fun addLogTableModelListener(eventListener: LogTableModelListener)

    fun getItemInCurrentPage(row: Int): LogcatItem

    fun getLogFilter(): LogcatFilter

    fun getRowIndex(lineNumber: Int): Int

}