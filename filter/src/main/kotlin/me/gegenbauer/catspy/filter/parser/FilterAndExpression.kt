package me.gegenbauer.catspy.filter.parser

class FilterAndExpression(
    val left: FilterExpression,
    val right: FilterExpression,
    override val start: Int = 0,
    override val end: Int = 0,
): FilterExpression()