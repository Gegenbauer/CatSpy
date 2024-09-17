package me.gegenbauer.catspy.log.filter

import me.gegenbauer.catspy.cache.isEmpty
import me.gegenbauer.catspy.log.filter.ColumnFilter.Companion.isEmpty
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.datasource.LogItem
import me.gegenbauer.catspy.view.filter.FilterItem
import java.util.regex.Pattern

fun interface LogFilter {

    /**
     * @param item the item to be filtered
     * @return true if the item should be included in the list, false otherwise
     */
    fun match(item: LogItem): Boolean

    companion object {
        val default = LogFilter { true }
    }
}

class DefaultLogFilter(
    val filters: List<ColumnFilterInfo>,
    val columns: List<Column>,
) : LogFilter {

    override fun match(item: LogItem): Boolean {
        if (filters.isEmpty()) return true
        return filters.all { filterInfo ->
            val partIndex = filterInfo.column.partIndex
            val filter = filterInfo.filter
            filter.isEmpty || filter.filter(item.getPart(partIndex))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DefaultLogFilter) return false
        if (filters.size != other.filters.size) return false
        for (index in filters.indices) {
            if (filters[index].filterItem != other.filters[index].filterItem) return false
        }
        return true
    }

    override fun hashCode(): Int {
        return filters.sumOf { it.filterItem.hashCode() }
    }

    override fun toString(): String {
        return "[${filters.joinToString(", ") { it.filterItem.toString() }}]"
    }

    companion object {
        val default = DefaultLogFilter(emptyList(), emptyList())
    }
}

fun FilterItem.match(text: String): Boolean {
    return matchHidePattern(negativeFilter, text) && matchShowPattern(positiveFilter, text)
}

private fun matchShowPattern(pattern: Pattern, text: String): Boolean {
    if (pattern.isEmpty) return true
    return pattern.matcher(text).find()
}

private fun matchHidePattern(pattern: Pattern, text: String): Boolean {
    if (pattern.isEmpty) return true
    return pattern.matcher(text).find().not()
}