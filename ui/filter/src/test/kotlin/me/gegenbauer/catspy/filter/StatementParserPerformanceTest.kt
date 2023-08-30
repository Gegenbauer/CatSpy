package me.gegenbauer.catspy.filter

import me.gegenbauer.catspy.filter.parser.StatementParser

fun main() {
    // (tag:servicemanager | message:get) | (level:info & message~:"ser$")
    val expression = "((tag:servicemanager | message:get) | (level:info & message~:\"ser$\"))"
//    val expression = "(tag:servicemanager)"
    val parser = StatementParser()
    val costList = arrayListOf<Int>()
    for (i in 0..100000) {
        val startTime = System.currentTimeMillis()
        val filterExpression = parser.parse(expression)
        val endTime = System.currentTimeMillis()
        costList.add((endTime - startTime).toInt())
    }
    println("parse result: $expression, cost: ${costList.average()}ms")
}