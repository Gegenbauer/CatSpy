package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.log.datasource.LogProducerManager
import me.gegenbauer.catspy.log.filter.LogFilter
import me.gegenbauer.catspy.log.datasource.LogItem

interface ILogTableModel {
    val selectedLogRows: MutableSet<Int>

    val logObservables: LogProducerManager.LogObservables

    fun addLogTableModelListener(eventListener: LogTableModelListener)

    fun getItemInCurrentPage(row: Int): LogItem

    fun getLogFilter(): LogFilter

    fun getRowIndexInAllPages(lineNumber: Int): Int

    fun getRowIndexInCurrentPage(lineNumber: Int): Int

    fun selectAllRowsInCurrentPage()
}