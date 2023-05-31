package me.gegenbauer.catspy.filter.parser

import kotlin.test.Test

class StatementParserTest {
    private val parser = StatementParser()

    @Test
    fun `should parse and expression`() {
        val expression = "  level:info   &   message~:\"ser$\"  "
        val filterExpression = parser.parse(expression)
        filterExpression as? AndExpression ?: throw IllegalStateException("Filter expression is not an and expression")
        assert(filterExpression.left.getContent() == "level:info")
        assert(filterExpression.right.getContent() == "message~:\"ser$\"")
    }

    @Test
    fun `should parse or expression`() {
        val expression = "   tag:servicemanager   | message:get "
        val filterExpression = parser.parse(expression)
        filterExpression as? OrExpression ?: throw IllegalStateException("Filter expression is not an or expression")
        assert(filterExpression.left.getContent() == "tag:servicemanager")
        assert(filterExpression.right.getContent() == "message:get")
    }

    @Test
    fun `should parse parentheses expression`() {
        val expression = "((tag:servicemanager | message:get) | (level:info & message~:\"ser$\"))"
        val filterExpression = parser.parse(expression)
        filterExpression as? OrExpression ?: throw IllegalStateException("Filter expression is not an or expression")
        filterExpression.left as? OrExpression ?: throw IllegalStateException("Filter expression is not an or expression")
        filterExpression.right as? AndExpression ?: throw IllegalStateException("Filter expression is not an and expression")
        assert((filterExpression.left as OrExpression).left.wholeExpression == "tag:servicemanager")
        assert((filterExpression.left as OrExpression).right.wholeExpression == "message:get")
        assert((filterExpression.right as AndExpression).left.wholeExpression == "level:info")
        assert((filterExpression.right as AndExpression).right.wholeExpression == "message~:\"ser$\"")
    }
}