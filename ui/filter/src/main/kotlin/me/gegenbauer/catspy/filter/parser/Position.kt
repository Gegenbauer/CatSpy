package me.gegenbauer.catspy.filter.parser

data class QuoteIndexPair(override val startIndex: Int, override val endIndex: Int) : PositionPair

data class InvalidPosition(
    override val startIndex: Int,
    val invalidType: InvalidType,
    val invalidChar: TokenType,
    override val endIndex: Int = startIndex
) : PositionPair

interface Position {
    val startIndex: Int
        get() = -1
}

interface PositionPair : Position {
    val endIndex: Int
        get() = -1
}

enum class InvalidType {
    UNPAIRED_PARENTHESIS,
    UNPAIRED_DOUBLE_QUOTE,
    INVALID_CHAR,
}