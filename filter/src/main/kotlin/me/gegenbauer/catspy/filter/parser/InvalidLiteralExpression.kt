package me.gegenbauer.catspy.filter.parser

class InvalidLiteralExpression(
    wholeExpression: String,
    start: Int,
    end: Int,
) : FilterExpression(wholeExpression, start, end) {

    constructor(expression: FilterExpression) : this(expression.wholeExpression, expression.start, expression.end)
}