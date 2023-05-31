package me.gegenbauer.catspy.filter.parser

import kotlin.test.Test

class FilterStatementParserTest {
    private val parser = FilterStatementParser()

    @Test
    fun `should parse and expression`() {
        val expression = "  level:info   &   message~:\"ser$\"  "
        val filterExpression = parser.parse(expression)
        filterExpression as? FilterAndExpression ?: throw IllegalStateException("Filter expression is not an and expression")
        assert(filterExpression.left.getContent() == "level:info")
        assert(filterExpression.right.getContent() == "message~:\"ser$\"")
    }

    @Test
    fun `should parse or expression`() {
        val expression = "   tag:servicemanager   | message:get "
        val filterExpression = parser.parse(expression)
        filterExpression as? FilterOrExpression ?: throw IllegalStateException("Filter expression is not an or expression")
        assert(filterExpression.left.getContent() == "tag:servicemanager")
        assert(filterExpression.right.getContent() == "message:get")
    }

    @Test
    fun `should parse parentheses expression`() {
        val expression = "((tag:servicemanager | message:get) | (level:info & message~:\"ser$\"))"
        val filterExpression = parser.parse(expression)
        filterExpression as? FilterOrExpression ?: throw IllegalStateException("Filter expression is not an or expression")
        filterExpression.left as? FilterOrExpression ?: throw IllegalStateException("Filter expression is not an or expression")
        filterExpression.right as? FilterAndExpression ?: throw IllegalStateException("Filter expression is not an and expression")
        assert((filterExpression.left as FilterOrExpression).left.wholeExpression == "tag:servicemanager")
        assert((filterExpression.left as FilterOrExpression).right.wholeExpression == "message:get")
        assert((filterExpression.right as FilterAndExpression).left.wholeExpression == "level:info")
        assert((filterExpression.right as FilterAndExpression).right.wholeExpression == "message~:\"ser$\"")
    }
}