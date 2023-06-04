package me.gegenbauer.catspy.filter.parser

class ParenthesesExpression(
    val innerExpression: FilterExpression,
    wholeExpression: String,
    start: Int,
    end: Int,
) : FilterExpression(wholeExpression, start, end) {

    constructor(innerExpression: FilterExpression, expression: FilterExpression) : this(
        innerExpression,
        expression.wholeExpression,
        expression.start,
        expression.end
    )

    override fun toString(): String {
        return "expressions=$innerExpression"
    }
}