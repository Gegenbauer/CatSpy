package me.gegenbauer.catspy.filter

import me.gegenbauer.catspy.ui.filter.FilterStatementParser

fun main() {
    // (tag:servicemanager | message:get) | (level:info & message~:"ser$")
    val expression = "(tag:servicemanager | message:get) | (level:info & message~:\"ser$\")"
    val parser = FilterStatementParser()
    val filterExpression = parser.parse(expression)
    println(filterExpression)
}