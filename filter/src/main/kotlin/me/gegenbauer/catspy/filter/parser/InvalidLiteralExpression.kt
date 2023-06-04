package me.gegenbauer.catspy.filter.parser

class InvalidLiteralExpression(
    wholeExpression: String,
    start: Int,
    end: Int,
    reason: String = ""
) : FilterExpression(wholeExpression, start, end) {

    constructor(
        expression: FilterExpression,
        reason: String = ""
    ) : this(expression.wholeExpression, expression.start, expression.end, reason)

    constructor(
        wholeExpression: String,
        start: Int,
        reason: String = ""
    ) : this(wholeExpression, start, start, reason)
}