package me.gegenbauer.catspy.data.repo.log

import me.gegenbauer.catspy.data.model.log.LogFilter
import me.gegenbauer.catspy.data.model.log.LogcatLogItem

interface LogRepository : LogObservable.Observer<LogcatLogItem> {

    var logFilter: LogFilter<LogcatLogItem>

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

    fun <R> accessCacheItems(write: Boolean, visitor: (MutableList<LogcatLogItem>) -> R): R

    fun <R> accessLogItems(write: Boolean, visitor: (MutableList<LogcatLogItem>) -> R): R

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