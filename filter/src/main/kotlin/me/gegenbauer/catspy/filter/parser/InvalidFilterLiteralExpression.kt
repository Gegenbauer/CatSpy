package me.gegenbauer.catspy.filter.parser

class InvalidFilterLiteralExpression(
    wholeExpression: String,
    start: Int,
    end: Int,
) : FilterExpression(wholeExpression, start, end)