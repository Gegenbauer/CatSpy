package me.gegenbauer.catspy.log.model

import me.gegenbauer.catspy.cache.isEmpty
import me.gegenbauer.catspy.log.LogLevel
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.isEmpty
import me.gegenbauer.catspy.view.filter.getOrCreateFilterItem
import java.util.regex.Pattern

data class LogcatFilter(
    val filterLog: FilterItem,
    val filterTag: FilterItem,
    val filterPid: FilterItem,
    val filterPackage: FilterItem,
    val filterTid: FilterItem,
    val filterLevel: LogLevel,
    val matchCase: Boolean = false
) : LogFilter<LogcatItem> {

    constructor(
        filterLog: String,
        filterTag: String,
        filterPid: String,
        filterPackage: String,
        filterTid: String,
        filterLevel: LogLevel,
        matchCase: Boolean = false
    ) : this(
        filterLog.getOrCreateFilterItem(matchCase),
        filterTag.getOrCreateFilterItem(matchCase),
        filterPid.getOrCreateFilterItem(matchCase),
        filterPackage.getOrCreateFilterItem(matchCase),
        filterTid.getOrCreateFilterItem(matchCase),
        filterLevel,
        matchCase
    )

    override fun match(item: LogcatItem): Boolean {
        if (this == EMPTY_FILTER) {
            return true
        }
        if (item.level < this.filterLevel) {
            return false
        }
        val message = item.message
        val tag = item.tag
        val pid = item.pid
        val tid = item.tid
        val packageName = item.packageName

        val logLineMatches = matchHidePattern(filterLog.negativeFilter, message) &&
                matchShowPattern(filterLog.positiveFilter, message)

        val tagMatches = matchHidePattern(filterTag.negativeFilter, tag) &&
                matchShowPattern(filterTag.positiveFilter, tag)

        val pidMatches = matchHidePattern(filterPid.negativeFilter, pid) &&
                matchShowPattern(filterPid.positiveFilter, pid)

        val packageMatches = matchHidePattern(filterPackage.negativeFilter, packageName) &&
                matchShowPattern(filterPackage.positiveFilter, packageName)

        val tidMatches = matchHidePattern(filterTid.negativeFilter, tid) &&
                matchShowPattern(filterTid.positiveFilter, tid)

        return logLineMatches && tagMatches && pidMatches && packageMatches && tidMatches
    }

    private fun matchShowPattern(pattern: Pattern, text: String): Boolean {
        if (pattern.isEmpty) return true
        return pattern.matcher(text).find()
    }

    private fun matchHidePattern(pattern: Pattern, text: String): Boolean {
        if (pattern.isEmpty) return true
        return pattern.matcher(text).find().not()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LogcatFilter) {
            return false
        }
        if (filterLog.isEmpty() && other.filterLog.isEmpty() &&
            filterTag.isEmpty() && other.filterTag.isEmpty() &&
            filterPid.isEmpty() && other.filterPid.isEmpty() &&
            filterPackage.isEmpty() && other.filterPackage.isEmpty() &&
            filterTid.isEmpty() && other.filterTid.isEmpty() &&
            filterLevel == other.filterLevel
        ) {
            return true
        }
        return this.filterLog == other.filterLog &&
                this.filterTag == other.filterTag &&
                this.filterPid == other.filterPid &&
                this.filterPackage == other.filterPackage &&
                this.filterTid == other.filterTid &&
                this.filterLevel == other.filterLevel &&
                this.matchCase == other.matchCase
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    companion object {
        val EMPTY_FILTER = LogcatFilter("", "", "", "", "", LogLevel.NONE)
    }
}