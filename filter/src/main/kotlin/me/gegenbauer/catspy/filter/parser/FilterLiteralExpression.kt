package me.gegenbauer.catspy.filter.parser

class FilterLiteralExpression(
    val key: FilterKey,
    val value: FilterValue,
    val isExclude: Boolean = false,
    override val start: Int = 0,
    override val end: Int = 0,
): FilterExpression()