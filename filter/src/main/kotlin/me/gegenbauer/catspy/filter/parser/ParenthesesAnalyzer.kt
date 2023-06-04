package me.gegenbauer.catspy.filter.parser

class ParenthesesAnalyzer: IParenthesesAnalyzer {
    private val parenthesesPair = mutableListOf<PositionPair>()

    override fun analyzeParentheses(expression: FilterExpression) {
        if (parenthesesPair.isNotEmpty()) return
        setParenthesesPairs(findParenthesesPairs(expression))
    }

    override fun setParenthesesPairs(positionPairs: List<PositionPair>) {
        parenthesesPair.clear()
        parenthesesPair.addAll(positionPairs)
    }

    override fun getParenthesesPairs(): List<PositionPair> {
        return parenthesesPair.toList()
    }

    private fun findParenthesesPairs(expression: FilterExpression): List<PositionPair> {
        val parenthesesPairIndexes = mutableListOf<PositionPair>()
        var parenthesesCount = 0
        var startIndex = -1
        for (index in expression.start..expression.end) {
            if (expression.isInQuote(index)) continue
            if (TokenType.PARENTHESIS_LEFT.match(expression.wholeExpression[index])) {
                parenthesesCount++
                if (parenthesesCount == 1) {
                    startIndex = index
                }
            } else if (TokenType.PARENTHESIS_RIGHT.match(expression.wholeExpression[index])) {
                parenthesesCount--
                if (parenthesesCount == 0) {
                    parenthesesPairIndexes.add(QuoteIndexPair(startIndex, index))
                }
                if (parenthesesCount < 0) {
                    parenthesesPairIndexes.add(InvalidPosition(index, InvalidType.UNPAIRED_PARENTHESIS, TokenType.PARENTHESIS_RIGHT))
                    return parenthesesPairIndexes
                }
            }
        }
        if (parenthesesCount == 1) {
            return listOf(InvalidPosition(startIndex, InvalidType.UNPAIRED_PARENTHESIS, TokenType.PARENTHESIS_LEFT))
        }
        return parenthesesPairIndexes
    }
}