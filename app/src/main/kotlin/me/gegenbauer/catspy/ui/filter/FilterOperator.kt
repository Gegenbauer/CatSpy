package me.gegenbauer.catspy.ui.filter

sealed class FilterOperator(val operator: Char) {
    object And: FilterOperator('&')

    object Or: FilterOperator('|')

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