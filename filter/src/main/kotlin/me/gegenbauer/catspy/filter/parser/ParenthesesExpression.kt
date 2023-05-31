package me.gegenbauer.catspy.filter.parser

class ParenthesesExpression(
    val innerExpression: FilterExpression,
    wholeExpression: String,
    start: Int,
    end: Int,
) : FilterExpression(wholeExpression, start, end) {

    constructor(innerExpression: FilterExpression, expression: FilterExpression) : this(
        expression,
        expression.wholeExpression,
        expression.start,
        expression.end
    )

    companion object {
        const val LEFT = '('
        const val RIGHT = ')'

        /**
         * if the start and end of the expression is a pair of parentheses, return true
         * Eg: (tag:servicemanager | message:get) | (level:info & message~:"ser$") return false
         *    (tag:servicemanager) return true
         */
        fun isParenthesesExpression(expression: FilterExpression): Boolean {
            if (expression.wholeExpression.isEmpty()) {
                return false
            }
            var parenthesesCount = 0
            for (i in expression.start..expression.end) {
                val ch = expression.wholeExpression[i]
                if (ch == LEFT) {
                    parenthesesCount++
                } else if (ch == RIGHT) {
                    parenthesesCount--
                }
                if ((parenthesesCount == 0) && (i != expression.end) && ch.toString().isNotBlank()) {
                    return false
                }
            }
            return true
        }

        /**
         * Find the invalid index for unpaired parentheses
         * eg: "(message~: " return 10
         * eg: "message~: )" return 10
         * eg: "(message~: )" return -1
         */
        fun findInvalidIndexForUnpairedParentheses(expression: FilterExpression): Int {
            var parenthesesCount = 0
            for (index in expression.start..expression.end) {
                if (expression.wholeExpression[index] == '(') {
                    parenthesesCount++
                } else if (expression.wholeExpression[index] == ')') {
                    parenthesesCount--
                }
                if (parenthesesCount < 0) {
                    return index
                }
            }
            return if (parenthesesCount == 0) {
                -1
            } else {
                expression.wholeExpression.length - 1
            }
        }
    }

    override fun toString(): String {
        return "expressions=$innerExpression"
    }
}