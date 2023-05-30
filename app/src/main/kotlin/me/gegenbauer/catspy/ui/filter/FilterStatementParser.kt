package me.gegenbauer.catspy.ui.filter

import me.gegenbauer.catspy.ui.filter.FilterExpression.Companion.toFilterExpression

class FilterStatementParser {
    // filter expression example: (tag:servicemanager | message:get) | (level:info & message~:"ser$")

    fun parse(expression: String): FilterExpression {
        return parseExpression(expression.toFilterExpression())
    }


    /**
     * Pair Parentheses Expression
     */
    private fun parseExpression(expression: FilterExpression): FilterExpression {
        val trimmedExpression = expression.trim()
        if (FilterParenthesesExpression.isParenthesesExpression(trimmedExpression)) {
            return parseParenthesesExpression(trimmedExpression)
        }
        return parseLiteralExpression(trimmedExpression)
    }

    // (tag:servicemanager | message:get) | (level:info & message~:"ser$") is ok
    // (tag:servicemanager) is also ok
    // Exclude the first and last blank character interference
    // Record the start and end position of the expression
    private fun parseParenthesesExpression(expression: FilterExpression): FilterParenthesesExpression {
        val expressions = mutableListOf<FilterExpression>()
        var parenthesesCount = 0
        var start = 0
        val trimmedExpression = expression.trim()

        for (index in trimmedExpression.start..trimmedExpression.end) {
            val c = trimmedExpression.wholeExpression[index]
            if (c == FilterParenthesesExpression.LEFT) {
                parenthesesCount++
            } else if (c == FilterParenthesesExpression.RIGHT) {
                parenthesesCount--
            }
            if ((parenthesesCount == 0) && FilterOperator.isOperator(c)) {
                expressions.add(parseExpression(expression.crop(trimmedExpression.start + 1, index - 1)))
                start = index + 1
            }
        }
        expressions.add(parseExpression(expression.crop(start + 1, trimmedExpression.end - 1)))
        return FilterParenthesesExpression(expressions)
    }

    /**
     * Pair Literal Expression
     * if the expression contains | or &, then it is a pair literal expression
     * Eg: tag:servicemanager | message:get, return FilterOrExpression(FilterLiteralExpression(tag:servicemanager), FilterLiteralExpression(message:get))
     * Eg: (tag:servicemanager | message:get) & (tag:service | message:post), return FilterAndExpression(FilterOrExpression(FilterLiteralExpression(tag:servicemanager), FilterLiteralExpression(message:get)), FilterOrExpression(FilterLiteralExpression(tag:service), FilterLiteralExpression(message:post)))
     */
    private fun parseLiteralExpression(expression: FilterExpression): FilterExpression {
        // | and & have the same priority, should not be contains in a pair of parentheses
        var parenthesesCount = 0
        val trimmedExpression = expression.trim()
        for (index in trimmedExpression.start..trimmedExpression.end) {
            val c = trimmedExpression.wholeExpression[index]
            if (c == FilterParenthesesExpression.LEFT) {
                parenthesesCount++
            } else if (c == FilterParenthesesExpression.RIGHT) {
                parenthesesCount--
            }
            if (parenthesesCount == 0 && FilterOperator.isOperator(c)) {
                val left = parseExpression(expression.crop(trimmedExpression.start, index - 1))
                val right = parseExpression(expression.crop(index + 1, trimmedExpression.end))
                return if (FilterOperator.isAndOperator(c)) {
                    FilterOrExpression(left, right)
                } else {
                    FilterAndExpression(left, right)
                }
            }
        }
        return parseSingleLiteralExpression(trimmedExpression)
    }

    private fun parseSingleLiteralExpression(expression: FilterExpression): FilterLiteralExpression {
        val tokens = expression.getContent().split(':')
        if (tokens.size > 2) {
            throw IllegalArgumentException("Invalid expression: $expression")
        } else if (tokens.size == 1) {
            return FilterLiteralExpression(FilterKey.Message, NormalFilterValue(tokens[0]))
        }
        return if (tokens[0].endsWith("~")) {
            val key = FilterKey.from(tokens[0].substring(0, tokens[0].length - 1))
            val value = RegexFilterValue(tokens[1])
            FilterLiteralExpression(key, value, )
        } else {
            val key = FilterKey.from(tokens[0])
            val value = NormalFilterValue(tokens[1])
            FilterLiteralExpression(key, value)
        }
    }
}