package me.gegenbauer.catspy.log.model

fun interface LogFilter<T: LogItem> {

    /**
     * @param item the item to be filtered
     * @return true if the item should be included in the list, false otherwise
     */
    fun match(item: T): Boolean
}