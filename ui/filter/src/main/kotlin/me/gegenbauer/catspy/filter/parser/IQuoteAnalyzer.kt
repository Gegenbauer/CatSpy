package me.gegenbauer.catspy.filter.parser

interface IQuoteAnalyzer {
    fun analyzeQuote(expression: FilterExpression)
    
    fun getInvalidQuotePairIndexes(): List<PositionPair> {
        return getQuotePairs().filter { it.startIndex == it.endIndex }
    }

    fun setQuotePairs(positionPairs: List<PositionPair>)

    fun getQuotePairs(): List<PositionPair>

    fun isQuotePairValid(): Boolean {
        return getInvalidQuotePairIndexes().isEmpty()
    }

    fun getInvalidQuoteIndex(): Int {
        return getInvalidQuotePairIndexes().firstOrNull()?.startIndex ?: -1
    }

    fun isInQuote(index: Int): Boolean {
        return getQuotePairs().any { it.startIndex < index && it.endIndex > index }
    }
}