package me.gegenbauer.catspy.filter.parser

open class FilterExpression internal constructor(
    open val wholeExpression: String = "",
    open val start: Int = 0,
    open val end: Int = 0,
) : IQuoteAnalyzer by QuoteAnalyzer(), IParenthesesAnalyzer by ParenthesesAnalyzer() {

    internal constructor(expression: FilterExpression) : this(
        expression.wholeExpression,
        expression.start,
        expression.end
    )

    fun getContent(): String {
        return wholeExpression.substring(start, end + 1)
    }

    fun crop(start: Int, end: Int): FilterExpression {
        return FilterExpression(wholeExpression, start, end)
    }

    fun lastCharBefore(index: Int): Char {
        require(index > 0) { "Index must be greater than 0" }
        return wholeExpression[index - 1]
    }

    fun isQuoteValid(): Boolean {
        return getInvalidQuotePairIndexes().isEmpty()
    }

    override fun toString(): String {
        return getContent()
    }

    fun trim(): FilterExpression {
        val trimmedRange = trimmedRange()
        return FilterExpression(wholeExpression, trimmedRange.first, trimmedRange.last).apply {
            // must analyze parentheses after quote.
            setQuotePairs(getQuotePairs())
            setParenthesesPairs(getParenthesesPairs())
        }
    }

    companion object {
        fun String.toFilterExpression(): FilterExpression {
            return from(this)
        }

        private fun from(expression: String): FilterExpression {
            val trimmedRange = expression.trimmedRange()
            if (trimmedRange.first == -1 || trimmedRange.last == -1) {
                // expression is blank
                return FilterExpression(expression, 0, expression.length - 1).apply {
                    // must analyze parentheses after quote.
                    analyzeQuote(this)
                    analyzeParentheses(this)
                }
            }
            return FilterExpression(expression, trimmedRange.first, trimmedRange.last).apply {
                // must analyze parentheses after quote.
                analyzeQuote(this)
                analyzeParentheses(this)
            }
        }
    }
}

sealed class FilterKey(val value: String) {
    object Tag : FilterKey("tag")

    object Pid : FilterKey("pid")

    object Tid : FilterKey("tid")

    object Message : FilterKey("message")

    object Level : FilterKey("level")

    object Age : FilterKey("age")

    object UNKNOWN : FilterKey("unknown")

    companion object {
        // 使用反射来实现
        private val keyToObjectMap = FilterKey::class.nestedClasses.filter { clazz ->
            FilterKey::class.java.isAssignableFrom(clazz.java) && (clazz.java != FilterKey::class.java)
        }.map {
            it.objectInstance as FilterKey
        }.associateBy {
            it.value
        }

        fun from(key: String): FilterKey {
            return keyToObjectMap[key] ?: UNKNOWN
        }
    }
}

open class FilterValue(
    val value: String,
)

class NormalFilterValue(
    value: String,
) : FilterValue(value)

class RegexFilterValue(
    val regex: Regex
) : FilterValue(regex.pattern)


fun String.trimmedRange(): IntRange {
    val trimmed = this.trim()
    return if (trimmed.isEmpty()) {
        IntRange(-1, -1)
    } else {
        val start = indexOf(trimmed)
        IntRange(start, start + trimmed.length - 1)
    }
}

private fun FilterExpression.trimmedRange(): IntRange {
    val trimmed = wholeExpression.substring(start, end + 1).trim()
    return if (trimmed.isEmpty()) {
        IntRange(-1, -1)
    } else {
        val start = wholeExpression.indexOf(trimmed)
        IntRange(start, start + trimmed.length - 1)
    }
}