package me.gegenbauer.catspy.filter.parser

class AndExpression(
    val left: FilterExpression,
    val right: FilterExpression,
    wholeExpression: String,
    start: Int,
    end: Int,
) : FilterExpression(wholeExpression, start, end) {

    constructor(left: FilterExpression, right: FilterExpression, expression: FilterExpression) : this(
        left,
        right,
        expression.wholeExpression,
        expression.start,
        expression.end
    )

    override fun toString(): String {
        return "left=$left, right=$right"
    }
}