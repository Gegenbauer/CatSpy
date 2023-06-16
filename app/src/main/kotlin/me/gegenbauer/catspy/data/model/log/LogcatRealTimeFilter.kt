package me.gegenbauer.catspy.data.model.log

import me.gegenbauer.catspy.cache.PatternProvider
import me.gegenbauer.catspy.cache.PatternProvider.Companion.toPatternKey
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.data.model.log.FilterItem.Companion.isEmpty
import java.lang.StringBuilder
import java.util.*
import java.util.regex.Pattern

class LogcatRealTimeFilter(
    val filterLog: FilterItem,
    val filterTag: FilterItem,
    val filterPid: FilterItem,
    val filterTid: FilterItem,
    val filterLevel: LogLevel,
    val matchCase: Boolean = false
) : LogFilter<LogcatLogItem> {

    constructor(
        filterLog: String,
        filterTag: String,
        filterPid: String,
        filterTid: String,
        filterLevel: LogLevel,
        matchCase: Boolean = false
    ) : this(
        filterLog.toFilterItem(matchCase),
        filterTag.toFilterItem(matchCase),
        filterPid.toFilterItem(matchCase),
        filterTid.toFilterItem(matchCase),
        filterLevel,
        matchCase
    )

    override fun filter(item: LogcatLogItem): Boolean {
        if (this == emptyRealTimeFilter) {
            return true
        }
        if (item.level < this.filterLevel) {
            return false
        }
        val logLine = item.logLine
        val tag = item.tag
        val pid = item.pid
        val tid = item.tid

        return (matchHidePattern(filterLog.negativeFilter, logLine) &&
                matchShowPattern(filterLog.positiveFilter, logLine) &&
                matchHidePattern(filterTag.negativeFilter, tag) &&
                matchShowPattern(filterTag.positiveFilter, tag) &&
                matchHidePattern(filterPid.negativeFilter, pid) &&
                matchShowPattern(filterPid.positiveFilter, pid) &&
                matchHidePattern(filterTid.negativeFilter, tid) &&
                matchShowPattern(filterTid.positiveFilter, tid))
    }

    private fun matchShowPattern(pattern: Pattern, text: String): Boolean {
        if (pattern == PatternProvider.emptyPattern) return true
        return pattern.matcher(text).find()
    }

    private fun matchHidePattern(pattern: Pattern, text: String): Boolean {
        if (pattern == PatternProvider.emptyPattern) return true
        return pattern.matcher(text).find().not()
    }

    override fun hashCode(): Int {
        return Objects.hash(filterLog, filterTag, filterPid, filterTid, filterLevel, matchCase)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LogcatRealTimeFilter) {
            return false
        }
        return filterLog == other.filterLog &&
                filterTag == other.filterTag &&
                filterPid == other.filterPid &&
                filterTid == other.filterTid &&
                filterLevel == other.filterLevel &&
                matchCase == other.matchCase
    }

    override fun toString(): String {
        return "LogcatRealTimeFilter(filterLog=$filterLog, filterTag=$filterTag, filterPid=$filterPid, " +
                "filterTid=$filterTid, filterLevel=$filterLevel, matchCase=$matchCase)"
    }

    companion object {
        val emptyRealTimeFilter = LogcatRealTimeFilter("", "", "", "", LogLevel.VERBOSE)
        private const val PATTERN_SPLITTER = "|"
        private const val NEGATIVE_PATTERN_PREFIX = '-'
        fun String.toFilterItem(matchCase: Boolean = false): FilterItem {
            if (this.isEmpty()) {
                return FilterItem.emptyItem
            }
            val patterns = parsePattern(this)
            val patternProvider = ServiceManager.getContextService(PatternProvider::class.java)
            var errorMessage = ""
            val positiveFilter = runCatching {
                patternProvider.get(patterns.first.toPatternKey(matchCase))
            }.onFailure {
                errorMessage = it.message ?: ""
            }.getOrDefault(PatternProvider.emptyPattern)
            val negativeFilter = runCatching {
                patternProvider.get(patterns.second.toPatternKey(matchCase))
            }.onFailure {
                errorMessage += ", ${it.message ?: ""}"
            }.getOrDefault(PatternProvider.emptyPattern)
            return FilterItem(positiveFilter, negativeFilter, errorMessage)
        }

        // TODO 无法正确解析只包含 negativePattern 的字符串，eg: "-Activity"
        fun parsePattern(pattern: String): Pair<String, String> {
            val positivePattern = StringBuilder()
            val negativePattern = StringBuilder()
            val splitStrings = pattern.split(PATTERN_SPLITTER)
            for (item in splitStrings) {
                val trimmedItem = item.trim()
                if (trimmedItem.isEmpty()) {
                    continue
                }
                if (trimmedItem[0] != NEGATIVE_PATTERN_PREFIX) {
                    positivePattern.appendPattern(trimmedItem)
                } else {
                    negativePattern.appendPattern(trimmedItem.substring(1))
                }
            }

            return Pair(positivePattern.toString(), negativePattern.toString())
        }

        private fun StringBuilder.appendPattern(pattern: String) {
            if (pattern.isNotEmpty()) {
                if (this.isNotEmpty()) {
                    this.append(PATTERN_SPLITTER)
                }
                this.append(pattern)
            }
        }
    }
}

data class FilterItem(
    val positiveFilter: Pattern,
    val negativeFilter: Pattern,
    val errorMessage: String,
) {

    override fun hashCode(): Int {
        return Objects.hash(positiveFilter.pattern(), negativeFilter.pattern())
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FilterItem) {
            return false
        }
        return this.positiveFilter.pattern() == other.positiveFilter.pattern() &&
                this.negativeFilter.pattern() == other.negativeFilter.pattern()
    }

    override fun toString(): String {
        if (this.isEmpty()) {
            return STR_PATTERN_EMPTY
        }
        return "positive: ${positiveFilter.pattern()}, negative: ${negativeFilter.pattern()}"
    }

    companion object {
        val emptyItem = FilterItem(PatternProvider.emptyPattern, PatternProvider.emptyPattern, "")
        private const val STR_PATTERN_EMPTY = "Empty"

        fun FilterItem.isEmpty(): Boolean {
            return this == emptyItem
        }

        fun Pattern.toString(): String {
            if (this == PatternProvider.emptyPattern) return STR_PATTERN_EMPTY
            return this.pattern()
        }

        fun FilterItem.isError(): Boolean {
            return this.errorMessage.isNotEmpty()
        }

        fun FilterItem.isNotEmpty(): Boolean {
            return this != emptyItem
        }

        fun FilterItem.getMatchedList(text: String): List<Pair<Int, Int>> {
            if (positiveFilter.pattern().isEmpty()) {
                return emptyList()
            }
            val positiveMatcher = this.positiveFilter.matcher(text)
            val matchedList = mutableListOf<Pair<Int, Int>>()
            while (positiveMatcher.find()) {
                matchedList.add(Pair(positiveMatcher.start(), positiveMatcher.end()))
            }
            return matchedList
        }
    }
}