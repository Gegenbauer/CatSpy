package me.gegenbauer.catspy.filter.parser

sealed class Operator(val operator: Char) {
    object And: Operator('&')

    object Or: Operator('|')

    companion object {
        private val operators = listOf(And.operator, Or.operator)

        fun isOperator(char: Char): Boolean {
            return operators.contains(char)
        }

        fun isAndOperator(char: Char): Boolean {
            return char == And.operator
        }

        fun isOrOperator(char: Char): Boolean {
            return char == Or.operator
        }
    }
}