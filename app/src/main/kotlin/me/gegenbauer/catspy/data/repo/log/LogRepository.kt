package me.gegenbauer.catspy.data.repo.log

import me.gegenbauer.catspy.data.model.log.LogcatLogItem
import me.gegenbauer.catspy.data.model.log.LogcatRealTimeFilter

interface LogRepository : LogObservable.Observer<LogcatLogItem> {

    var logFilter: LogcatRealTimeFilter

    var fullMode: Boolean

    var bookmarkMode: Boolean

    var selectedRow: Int

    fun onItemInsertFromInit(logItem: LogcatLogItem)

    fun onItemInsertFromFilterUpdate(logItem: LogcatLogItem)

    fun onFilterUpdate()

    fun cancelFilterUpdate()

    fun clear()

    fun addLogChangeListener(listener: LogChangeListener)

    fun removeLogChangeListener(listener: LogChangeListener)

    fun <R> accessCacheItems(visitor: (MutableList<LogcatLogItem>) -> R): R

    fun <R> accessLogItems(visitor: (MutableList<LogcatLogItem>) -> R): R

    fun getLogCount(): Int

    fun interface LogChangeListener {
        fun onLogChanged(event: LogChangeEvent)
    }

    data class LogChangeEvent(
        val type: Int,
        val startRow: Int,
        val endRow: Int,
    )
}