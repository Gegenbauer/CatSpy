package me.gegenbauer.catspy.view.filter

import me.gegenbauer.catspy.cache.PatternProvider
import me.gegenbauer.catspy.cache.PatternProvider.Companion.toPatternKey
import me.gegenbauer.catspy.context.ServiceManager
import java.util.*
import java.util.regex.Pattern

data class FilterItem(
    val positiveFilter: Pattern,
    val negativeFilter: Pattern,
    val errorMessage: String,
) {

    override fun hashCode(): Int {
        return Objects.hash(positiveFilter, negativeFilter)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FilterItem) {
            return false
        }
        return this.positiveFilter.pattern() == other.positiveFilter.pattern() &&
                this.negativeFilter.pattern() == other.negativeFilter.pattern() &&
                this.positiveFilter.flags() == other.positiveFilter.flags() &&
                this.negativeFilter.flags() == other.negativeFilter.flags()
    }

    override fun toString(): String {
        if (this.isEmpty()) {
            return STR_PATTERN_EMPTY
        }
        return "positive: ${positiveFilter.pattern()}, negative: ${negativeFilter.pattern()}"
    }

    companion object {
        val emptyItem = FilterItem(PatternProvider.EMPTY_PATTERN, PatternProvider.EMPTY_PATTERN, "")
        private const val STR_PATTERN_EMPTY = "Empty"

        fun FilterItem.isEmpty(): Boolean {
            return this == emptyItem
        }

        fun FilterItem.rebuild(matchCase: Boolean): FilterItem {
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
            return this != emptyItem
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
fun String.toFilterItem(matchCase: Boolean = false): FilterItem {
    if (this.isEmpty()) {
        return FilterItem.emptyItem
    }
    val patterns = parsePattern(this)
    val patternProvider = ServiceManager.getContextService(PatternProvider::class.java)
    var errorMessage = ""
    val positiveFilter = runCatching {
        patternProvider[patterns.first.toPatternKey(matchCase)] ?: PatternProvider.EMPTY_PATTERN
    }.onFailure {
        errorMessage = it.message ?: ""
    }.getOrDefault(PatternProvider.EMPTY_PATTERN)
    val negativeFilter = runCatching {
        patternProvider[patterns.second.toPatternKey(matchCase)] ?: PatternProvider.EMPTY_PATTERN
    }.onFailure {
        errorMessage += ", ${it.message ?: ""}"
    }.getOrDefault(PatternProvider.EMPTY_PATTERN)
    return FilterItem(positiveFilter, negativeFilter, errorMessage)
}

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