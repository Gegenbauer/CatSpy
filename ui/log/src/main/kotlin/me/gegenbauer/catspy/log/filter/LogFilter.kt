package me.gegenbauer.catspy.log.filter

import me.gegenbauer.catspy.cache.isEmpty
import me.gegenbauer.catspy.log.filter.ColumnFilter.Companion.isEmpty
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.datasource.LogItem
import me.gegenbauer.catspy.view.filter.FilterItem
import java.util.regex.Pattern

class DefaultLogFilter(
    val filters: List<FilterInfo>,
    val columns: List<Column>,
) : LogFilter {

    override fun match(item: LogItem): Boolean {
        if (filters.isEmpty()) return true
        for (index in filters.indices) {
            val partIndex = filters[index].column.partIndex
            val filter = filters[index].filter
            if (filter.isEmpty) continue
            val matchResult = filters[index].filter.filter(item.getPart(partIndex))
            if (!matchResult) return false
        }
        return true
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