package me.gegenbauer.catspy.filter.parser

import me.gegenbauer.catspy.filter.parser.FilterExpression.Companion.toFilterExpression
import me.gegenbauer.catspy.java.ext.EMPTY_STRING

// TODO recognize the invalid expression
// TODO support value surrounded by double quotes, allow all character except double quotes; Recognize invalid double quotes;
// TODO add double quotes case for all character check
class StatementParser {
    // filter expression example: (tag:servicemanager | message:get) | (level:info & message~:"ser$")

    fun parse(expression: String): FilterExpression {
        val filterExpression = expression.toFilterExpression()
        if (filterExpression.isQuoteValid().not()) {
            return InvalidLiteralExpression(expression, filterExpression.getInvalidQuoteIndex())
        }
        val invalidIndex = findInvalidIndexForUnpairedParentheses(filterExpression)
        if (invalidIndex != -1) {
            return InvalidParenthesesExpression(expression, invalidIndex, invalidIndex)
        }
        return parseExpression(filterExpression)
    }

    /**
     * Pair Parentheses Expression
     */
    private fun parseExpression(expression: FilterExpression): FilterExpression {
        val trimmedExpression = expression.trim()
        if (isParenthesesExpression(trimmedExpression)) {
            return parseParenthesesExpression(trimmedExpression)
        }
        return parseLiteralExpression(trimmedExpression)
    }

    // (tag:servicemanager | message:get) | (level:info & message~:"ser$") is ok
    // (tag:servicemanager) is also ok
    // Exclude the first and last blank character interference
    // Record the start and end position of the expression
    private fun parseParenthesesExpression(expression: FilterExpression): ParenthesesExpression {
        val trimmedExpression = expression.trim()
        return ParenthesesExpression(
            parseExpression(
                trimmedExpression.crop(
                    trimmedExpression.start + 1,
                    trimmedExpression.end - 1
                )
            ), trimmedExpression
        )
    }

    /**
     * Pair Literal Expression
     * if the expression contains | or &, then it is a pair of literal expressions
     * Eg: tag:servicemanager | message:get, return FilterOrExpression(FilterLiteralExpression(tag:servicemanager), FilterLiteralExpression(message:get))
     * Eg: (tag:servicemanager | message:get) & (tag:service | message:post), return FilterAndExpression(FilterOrExpression(FilterLiteralExpression(tag:servicemanager), FilterLiteralExpression(message:get)), FilterOrExpression(FilterLiteralExpression(tag:service), FilterLiteralExpression(message:post)))
     */
    private fun parseLiteralExpression(expression: FilterExpression): FilterExpression {
        // | and & have the same priority, should not be contains in a pair of parentheses
        var parenthesesCount = 0
        val trimmedExpression = expression.trim()
        for (index in trimmedExpression.start..trimmedExpression.end) {
            if (expression.isInQuote(index)) {
                continue
            }
            val c = trimmedExpression.wholeExpression[index]
            if (TokenType.PARENTHESIS_LEFT.match(c)) {
                parenthesesCount++
            } else if (TokenType.PARENTHESIS_RIGHT.match(c)) {
                parenthesesCount--
            }
            if (parenthesesCount == 0 && c.isOperator()) {
                val left = parseExpression(expression.crop(trimmedExpression.start, index - 1))
                val right = parseExpression(expression.crop(index + 1, trimmedExpression.end))
                return if (TokenType.OPERATOR_OR.match(c)) {
                    OrExpression(left, right, trimmedExpression)
                } else {
                    AndExpression(left, right, trimmedExpression)
                }
            }
        }
        return parseSingleLiteralExpression(trimmedExpression)
    }

    private fun parseSingleLiteralExpression(expression: FilterExpression): FilterExpression {
        val trimmedExpression = expression.trim()
        val splitterIndex = findSplitterIndex(trimmedExpression)
        if (splitterIndex == -1) {
            return LiteralExpression(FilterKey.Message, NormalFilterValue(trimmedExpression.getContent()), trimmedExpression)
        }
        val leftPart = trimmedExpression.wholeExpression.substring(trimmedExpression.start, splitterIndex)
        if (splitterIndex + 1 > trimmedExpression.end) {
            return InvalidLiteralExpression(trimmedExpression)
        }
        val rightPart = trimmedExpression.wholeExpression.substring(splitterIndex + 1, trimmedExpression.end + 1)
        if (rightPart.isBlank()) { // only one token, eg: servicemanager
            return InvalidLiteralExpression(trimmedExpression)
        }
        val trimmedTokenLeftPart = leftPart.trim()
        val trimmedTokenRightPart = rightPart.trim()
        val isRegex = trimmedTokenLeftPart.endsWith(TokenType.REGEX_FLAG.value)
        val isExclude = trimmedTokenLeftPart.startsWith(TokenType.EXCLUDE_FLAG.value)
        val keyStart = if (isExclude) 1 else 0
        val keyEnd = if (isRegex) trimmedTokenLeftPart.length - 1 else trimmedTokenLeftPart.length
        val key = FilterKey.from(trimmedTokenLeftPart.substring(keyStart, keyEnd))
        if (key == FilterKey.UNKNOWN) {
            return LiteralExpression(
                FilterKey.Message,
                NormalFilterValue(trimmedExpression.getContent()),
                trimmedExpression
            )
        }
        return if (isRegex) {
            val regex = runCatching {
                Regex(
                    trimmedTokenRightPart
                        .removeSurrounding(TokenType.DOUBLE_QUOTE.value.toString())
                        .replace(TokenType.ESCAPE.value.toString(), EMPTY_STRING)
                )
            }.onFailure {
                return InvalidLiteralExpression(trimmedExpression, it.localizedMessage)
            }.getOrThrow()

            LiteralExpression(key, RegexFilterValue(regex), trimmedExpression, isExclude)
        } else {
            LiteralExpression(key, NormalFilterValue(trimmedTokenRightPart), trimmedExpression, isExclude)
        }
    }

    private fun findSplitterIndex(filterExpression: FilterExpression): Int {
        for (index in filterExpression.start..filterExpression.end) {
            if (filterExpression.isInQuote(index)) {
                continue
            }
            if (TokenType.SPLITTER.match(filterExpression.wholeExpression[index])) {
                return index
            }
        }
        return -1
    }
}