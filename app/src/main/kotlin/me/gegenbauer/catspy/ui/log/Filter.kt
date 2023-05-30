package me.gegenbauer.catspy.ui.log

fun interface Filter<T: LogItem> {
    fun filter(item: T): Boolean
}