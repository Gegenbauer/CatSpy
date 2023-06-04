package me.gegenbauer.catspy.filter.parser

enum class TokenType(override val value: Char) : Token {
    OPERATOR_AND('&'),
    OPERATOR_OR('|'),
    PARENTHESIS_LEFT('('),
    PARENTHESIS_RIGHT(')'),
    DOUBLE_QUOTE('"'),
    EXCLUDE_FLAG('-'),
    REGEX_FLAG('~'),
    SPLITTER(':'),
    ESCAPE('\\'),
}

fun Char.isOperator(): Boolean {
    return this == TokenType.OPERATOR_AND.value || this == TokenType.OPERATOR_OR.value
}

interface Token {
    val value: Char
    fun match(char: Char): Boolean {
        return value == char
    }
}