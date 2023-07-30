package me.gegenbauer.catspy.log.repo

import me.gegenbauer.catspy.log.model.LogFilter
import me.gegenbauer.catspy.log.model.LogcatLogItem

interface LogRepository : LogObservable.Observer<LogcatLogItem> {

    var logFilter: LogFilter<LogcatLogItem>

    var fullMode: Boolean

    var bookmarkMode: Boolean

    var selectedRow: Int

    val isFiltering: Boolean

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