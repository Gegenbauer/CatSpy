package me.gegenbauer.catspy.filter.parser

/**
 * if the start and end of the expression is a pair of parentheses, return true
 * Eg: (tag:servicemanager | message:get) | (level:info & message~:"ser$") return false
 *    (tag:servicemanager) return true
 */
fun isParenthesesExpression(expression: FilterExpression): Boolean {
    if (expression.wholeExpression.isEmpty()) {
        return false
    }
    var parenthesesCount = 0
    for (i in expression.start..expression.end) {
        if (expression.isInQuote(i)) {
            continue
        }
        val ch = expression.wholeExpression[i]
        if (TokenType.PARENTHESIS_LEFT.match(ch)) {
            parenthesesCount++
        } else if (TokenType.PARENTHESIS_RIGHT.match(ch)) {
            parenthesesCount--
        }
        if ((parenthesesCount == 0) && (i != expression.end) && ch.toString().isNotBlank()) {
            return false
        }
    }
    return true
}

/**
 * Find the invalid index for unpaired parentheses
 * eg: "(message~: " return 10
 * eg: "message~: )" return 10
 * eg: "(message~: )" return -1
 */
fun findInvalidIndexForUnpairedParentheses(expression: FilterExpression): Int {
    var parenthesesCount = 0
    for (index in expression.start..expression.end) {
        if (expression.isInQuote(index)) {
            continue
        }
        val ch = expression.wholeExpression[index]
        if (TokenType.PARENTHESIS_LEFT.match(ch)) {
            parenthesesCount++
        } else if (TokenType.PARENTHESIS_RIGHT.match(ch)) {
            parenthesesCount--
        }
        if (parenthesesCount < 0) {
            return index
        }
    }
    return if (parenthesesCount == 0) {
        -1
    } else {
        expression.wholeExpression.length - 1
    }
}



