package me.gegenbauer.catspy.filter.parser

import me.gegenbauer.catspy.filter.parser.FilterExpression.Companion.toFilterExpression

// TODO recognize the invalid expression
class StatementParser {
    // filter expression example: (tag:servicemanager | message:get) | (level:info & message~:"ser$")

    fun parse(expression: String): FilterExpression {
        return parseExpression(expression.toFilterExpression())
    }


    /**
     * Pair Parentheses Expression
     */
    private fun parseExpression(expression: FilterExpression): FilterExpression {
        val trimmedExpression = expression.trim()
        if (ParenthesesExpression.isParenthesesExpression(trimmedExpression)) {
            val start = System.currentTimeMillis()
            return parseParenthesesExpression(trimmedExpression).apply {
                println("parse $trimmedExpression cost: ${System.currentTimeMillis() - start}ms")
            }
        }
        val start = System.currentTimeMillis()
        return parseLiteralExpression(trimmedExpression).apply {
            println("parse $trimmedExpression cost: ${System.currentTimeMillis() - start}ms")
        }
    }

    // (tag:servicemanager | message:get) | (level:info & message~:"ser$") is ok
    // (tag:servicemanager) is also ok
    // Exclude the first and last blank character interference
    // Record the start and end position of the expression
    private fun parseParenthesesExpression(expression: FilterExpression): ParenthesesExpression {
        val expressions = mutableListOf<FilterExpression>()
        var parenthesesCount = 0
        val trimmedExpression = expression.trim()
        var start = trimmedExpression.start

        for (index in trimmedExpression.start..trimmedExpression.end) {
            val c = trimmedExpression.wholeExpression[index]
            if (c == ParenthesesExpression.LEFT) {
                parenthesesCount++
            } else if (c == ParenthesesExpression.RIGHT) {
                parenthesesCount--
            }
            if ((parenthesesCount == 0) && Operator.isOperator(c)) {
                expressions.add(parseExpression(expression.crop(trimmedExpression.start + 1, index - 1)))
                start = index + 1
            }
        }
        expressions.add(parseExpression(expression.crop(start + 1, trimmedExpression.end - 1)))
        return ParenthesesExpression(expressions, trimmedExpression.wholeExpression, trimmedExpression.start, trimmedExpression.end)
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
            if (c == ParenthesesExpression.LEFT) {
                parenthesesCount++
            } else if (c == ParenthesesExpression.RIGHT) {
                parenthesesCount--
            }
            if (parenthesesCount == 0 && Operator.isOperator(c)) {
                val left = parseExpression(expression.crop(trimmedExpression.start, index - 1))
                val right = parseExpression(expression.crop(index + 1, trimmedExpression.end))
                return if (Operator.isOrOperator(c)) {
                    OrExpression(left, right, trimmedExpression.wholeExpression, trimmedExpression.start, trimmedExpression.end)
                } else {
                    AndExpression(left, right, trimmedExpression.wholeExpression, trimmedExpression.start, trimmedExpression.end)
                }
            }
        }
        return parseSingleLiteralExpression(trimmedExpression)
    }

    private fun parseSingleLiteralExpression(expression: FilterExpression): FilterExpression {
        val trimmedExpression = expression.trim()
        val tokens = trimmedExpression.getContent().split(LiteralExpression.SPLITTER)
        if (tokens.size > 2) {
            return InvalidLiteralExpression(trimmedExpression.wholeExpression, trimmedExpression.start, trimmedExpression.end)
        } else if (tokens.size == 1) {
            return LiteralExpression(FilterKey.Message, NormalFilterValue(tokens[0]), trimmedExpression.wholeExpression, trimmedExpression.start, trimmedExpression.end)
        }
        val trimmedTokenLeftPart = tokens[0].trim()
        val trimmedTokenRightPart = tokens[1].trim()
        val isRegex = trimmedTokenLeftPart.endsWith(LiteralExpression.REGEX_FLAG)
        val isExclude = trimmedTokenLeftPart.startsWith(LiteralExpression.EXCLUDE_FLAG)
        val keyStart = if (isExclude) 1 else 0
        val keyEnd = if (isRegex) trimmedTokenLeftPart.length - 1 else trimmedTokenLeftPart.length
        return if (isRegex) {
            val key = FilterKey.from(trimmedTokenLeftPart.substring(keyStart, keyEnd))
            val value = RegexFilterValue(trimmedTokenRightPart)
            LiteralExpression(key, value, trimmedExpression.wholeExpression, trimmedExpression.start, trimmedExpression.end, isExclude)
        } else {
            val key = FilterKey.from(trimmedTokenLeftPart.substring(keyStart, keyEnd))
            val value = NormalFilterValue(trimmedTokenRightPart)
            LiteralExpression(key, value, trimmedExpression.wholeExpression, trimmedExpression.start, trimmedExpression.end, isExclude)
        }
    }
}