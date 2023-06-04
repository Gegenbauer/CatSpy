package me.gegenbauer.catspy.ui.log

fun interface LogFilter<T: LogItem> {

    /**
     * @param item the item to be filtered
     * @return true if the item should be included in the list, false otherwise
     */
    fun filter(item: T): Boolean
}