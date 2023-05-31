package me.gegenbauer.catspy.filter.parser

class InvalidParenthesesExpression(
    wholeExpression: String,
    start: Int,
    end: Int,
) : FilterExpression(wholeExpression, start, end) {

    constructor(expression: FilterExpression) : this(expression.wholeExpression, expression.start, expression.end)
}