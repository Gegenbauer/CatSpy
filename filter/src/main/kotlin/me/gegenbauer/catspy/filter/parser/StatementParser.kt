package me.gegenbauer.catspy.filter.parser

import me.gegenbauer.catspy.filter.parser.FilterExpression.Companion.toFilterExpression

// TODO recognize the invalid expression
// TODO support value surrounded by double quotes, allow all character except double quotes; Recognize invalid double quotes;
// TODO add double quotes case for all character check
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
        if (!trimmedExpression.wholeExpression.contains(LiteralExpression.SPLITTER)) {
            return LiteralExpression(FilterKey.Message, NormalFilterValue(trimmedExpression.getContent()), trimmedExpression)
        }
        val tokens = trimmedExpression.getContent().split(LiteralExpression.SPLITTER)
        if ((tokens.size > 2) || (tokens.size == 2 && tokens[1].isBlank())) { // only one token, eg: servicemanager
            return InvalidLiteralExpression(trimmedExpression)
        }
        val trimmedTokenLeftPart = tokens[0].trim()
        val trimmedTokenRightPart = tokens[1].trim()
        val isRegex = trimmedTokenLeftPart.endsWith(LiteralExpression.REGEX_FLAG)
        val isExclude = trimmedTokenLeftPart.startsWith(LiteralExpression.EXCLUDE_FLAG)
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
            LiteralExpression(key, RegexFilterValue(trimmedTokenRightPart), trimmedExpression, isExclude)
        } else {
            LiteralExpression(key, NormalFilterValue(trimmedTokenRightPart), trimmedExpression, isExclude)
        }
    }
}