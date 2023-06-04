package me.gegenbauer.catspy.filter.parser

class LiteralExpression(
    val key: FilterKey,
    val value: FilterValue,
    wholeExpression: String,
    start: Int,
    end: Int,
    val isExclude: Boolean = false,
) : FilterExpression(wholeExpression, start, end) {

    constructor(key: FilterKey, value: FilterValue, expression: FilterExpression, isExclude: Boolean = false) : this(
        key,
        value,
        expression.wholeExpression,
        expression.start,
        expression.end,
        isExclude
    )
}