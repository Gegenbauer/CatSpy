package me.gegenbauer.catspy.filter.parser

import me.gegenbauer.catspy.java.ext.EMPTY_STRING

class InvalidLiteralExpression(
    wholeExpression: String,
    start: Int,
    end: Int,
    val reason: String = EMPTY_STRING
) : FilterExpression(wholeExpression, start, end) {

    constructor(
        expression: FilterExpression,
        reason: String = EMPTY_STRING
    ) : this(expression.wholeExpression, expression.start, expression.end, reason)

    constructor(
        wholeExpression: String,
        start: Int,
        reason: String = EMPTY_STRING
    ) : this(wholeExpression, start, start, reason)
}