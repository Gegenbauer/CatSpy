package me.gegenbauer.catspy.filter.parser

interface IParenthesesAnalyzer {
    fun analyzeParentheses(expression: FilterExpression)

    fun getInvalidParenthesesPairs(): List<PositionPair> {
        return getParenthesesPairs().filter { it.startIndex == it.endIndex }
    }

    fun setParenthesesPairs(positionPairs: List<PositionPair>)

    fun getParenthesesPairs(): List<PositionPair>

    fun getInvalidParenthesesIndex(): Int {
        return getInvalidParenthesesPairs().firstOrNull()?.startIndex ?: -1
    }

    fun isParenthesesValid(): Boolean {
        return getInvalidParenthesesPairs().isEmpty()
    }

    fun isInParentheses(index: Int): Boolean {
        return getParenthesesPairs().any { it.startIndex < index && it.endIndex > index }
    }

}