package me.gegenbauer.catspy.filter.parser

class QuoteAnalyzer: IQuoteAnalyzer {
    private val positionPairs = mutableListOf<PositionPair>()

    override fun analyzeQuote(expression: FilterExpression) {
        if (positionPairs.isNotEmpty()) return
        setQuotePairs(findDoubleQuotePairs(expression))
    }
    
    override fun setQuotePairs(positionPairs: List<PositionPair>) {
        this.positionPairs.clear()
        this.positionPairs.addAll(positionPairs)
    }

    override fun getQuotePairs(): List<PositionPair> {
        return positionPairs.toList()
    }

    private fun findDoubleQuotePairs(expression: FilterExpression): List<PositionPair> {
        val doubleQuotePairIndexes = mutableListOf<PositionPair>()
        var doubleQuotesCount = 0
        var startIndex = -1
        for (index in expression.start..expression.end) {
            if(TokenType.DOUBLE_QUOTE.match(expression.wholeExpression[index]) &&
                (index == 0 || !TokenType.ESCAPE.match(expression.lastCharBefore(index)))) {
                doubleQuotesCount++
                if (doubleQuotesCount == 1) {
                    startIndex = index
                }
            }
            if (doubleQuotesCount == 2) {
                doubleQuotePairIndexes.add(QuoteIndexPair(startIndex, index))
                doubleQuotesCount = 0
            }
        }
        if (doubleQuotesCount == 1) {
            return listOf(InvalidPosition(startIndex, InvalidType.UNPAIRED_DOUBLE_QUOTE, TokenType.DOUBLE_QUOTE))
        }
        return doubleQuotePairIndexes
    }
}