package me.gegenbauer.catspy.filter.parser

fun FilterExpression.getLiteralExpression(): List<LiteralExpression> {
    return when (this) {
        is ParenthesesExpression -> innerExpression.getLiteralExpression()
        is LiteralExpression -> listOf(this)
        is OrExpression -> left.getLiteralExpression() + right.getLiteralExpression()
        is AndExpression -> left.getLiteralExpression() + right.getLiteralExpression()
        is InvalidLiteralExpression -> emptyList()
        is InvalidParenthesesExpression -> emptyList()
        else -> throw IllegalStateException("Unknown expression type: $this")
    }
}

fun FilterExpression.getAllChildExpressions(): List<FilterExpression> {
    return when (this) {
        is ParenthesesExpression -> listOf(this) + innerExpression.getAllChildExpressions()
        is LiteralExpression -> listOf(this)
        is OrExpression -> listOf(this) + left.getAllChildExpressions() + right.getAllChildExpressions()
        is AndExpression -> listOf(this) + left.getAllChildExpressions() + right.getAllChildExpressions()
        is InvalidLiteralExpression -> listOf(this)
        is InvalidParenthesesExpression -> listOf(this)
        else -> throw IllegalStateException("Unknown expression type: $this")
    }
}