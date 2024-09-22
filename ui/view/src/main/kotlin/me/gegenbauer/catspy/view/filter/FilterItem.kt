package me.gegenbauer.catspy.view.filter

import me.gegenbauer.catspy.cache.PatternProvider
import me.gegenbauer.catspy.cache.PatternProvider.Companion.toPatternKey
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.isEmpty
import java.util.*
import java.util.regex.Pattern

data class FilterItem(
    val positiveFilter: Pattern,
    val negativeFilter: Pattern,
    val errorMessage: String,
) {

    override fun hashCode(): Int {
        return Objects.hash(positiveFilter, negativeFilter, errorMessage)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FilterItem) {
            return false
        }
        return this.positiveFilter.pattern() == other.positiveFilter.pattern() &&
                this.negativeFilter.pattern() == other.negativeFilter.pattern() &&
                this.positiveFilter.flags() == other.positiveFilter.flags() &&
                this.negativeFilter.flags() == other.negativeFilter.flags() &&
                this.errorMessage == other.errorMessage
    }

    override fun toString(): String {
        if (this.isEmpty()) {
            return STR_PATTERN_EMPTY
        }
        return "positive: ${positiveFilter.pattern()}, negative: ${negativeFilter.pattern()}, error: $errorMessage"
    }

    companion object {
        val EMPTY_ITEM = FilterItem(PatternProvider.EMPTY_PATTERN, PatternProvider.EMPTY_PATTERN, EMPTY_STRING)
        private const val STR_PATTERN_EMPTY = "Empty"

        fun FilterItem.isEmpty(): Boolean {
            return this == EMPTY_ITEM
        }

        fun FilterItem.rebuild(matchCase: Boolean): FilterItem {
            if (EMPTY_ITEM == this) return this
            return FilterItem(
                positiveFilter = ServiceManager.getContextService(PatternProvider::class.java)
                    [this.positiveFilter.pattern().toPatternKey(matchCase)] ?: PatternProvider.EMPTY_PATTERN,
                negativeFilter = ServiceManager.getContextService(PatternProvider::class.java)
                    [this.negativeFilter.pattern().toPatternKey(matchCase)] ?: PatternProvider.EMPTY_PATTERN,
                errorMessage = this.errorMessage
            )
        }

        fun Pattern.str(): String {
            if (this == PatternProvider.EMPTY_PATTERN) return STR_PATTERN_EMPTY
            return this.pattern()
        }

        fun FilterItem.isError(): Boolean {
            return this.errorMessage.isNotEmpty()
        }

        fun FilterItem.isNotEmpty(): Boolean {
            return this != EMPTY_ITEM
        }

        fun FilterItem.getMatchedList(text: String): List<Pair<Int, Int>> {
            if (positiveFilter.pattern().isEmpty()) {
                return emptyList()
            }
            val positiveMatcher = this.positiveFilter.matcher(text)
            val matchedList = mutableListOf<Pair<Int, Int>>()
            while (positiveMatcher.find()) {
                matchedList.add(Pair(positiveMatcher.start(), positiveMatcher.end() - 1))
            }
            return matchedList
        }
    }
}

private const val PATTERN_SPLITTER = "|"
private const val NEGATIVE_PATTERN_PREFIX = '-'

fun String.getOrCreateFilterItem(matchCase: Boolean = false): FilterItem {
    if (this.isEmpty()) {
        return FilterItem.EMPTY_ITEM
    }
    val filterCache = ServiceManager.getContextService(FilterCache::class.java)
    return filterCache[toFilterKey(matchCase)] ?: FilterItem.EMPTY_ITEM
}

internal fun String.toFilterItem(matchCase: Boolean = false): FilterItem {
    if (this.isEmpty()) {
        return FilterItem.EMPTY_ITEM
    }
    val patterns = parsePattern(this)
    val patternProvider = ServiceManager.getContextService(PatternProvider::class.java)
    var errorMessage = EMPTY_STRING
    val positiveFilter = runCatching {
        patternProvider[patterns.first.toPatternKey(matchCase)] ?: PatternProvider.EMPTY_PATTERN
    }.onFailure {
        errorMessage = it.message ?: EMPTY_STRING
    }.getOrDefault(PatternProvider.EMPTY_PATTERN)
    val negativeFilter = runCatching {
        patternProvider[patterns.second.toPatternKey(matchCase)] ?: PatternProvider.EMPTY_PATTERN
    }.onFailure {
        errorMessage += ", ${it.message ?: EMPTY_STRING}"
    }.getOrDefault(PatternProvider.EMPTY_PATTERN)
    return FilterItem(positiveFilter, negativeFilter, errorMessage)
}

internal fun parsePattern(pattern: String): Pair<String, String> {
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

fun FilterItem.matches(text: String): Boolean {
    val positiveMatcher = this.positiveFilter.matcher(text)
    val negativeMatcher = this.negativeFilter.matcher(text)
    return positiveMatcher.find() && !negativeMatcher.find()
}