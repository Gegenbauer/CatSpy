package me.gegenbauer.catspy.filter.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatementParserTest {
    private val parser = StatementParser()

    @Test
    fun `should return AndExpression when parse and expression with extra space`() {
        val expression = "  level:info   &   message~:\"ser$\"  "
        val filterExpression = parser.parse(expression)
        filterExpression as? AndExpression ?: throw IllegalStateException("Filter expression is not an and expression")
        assertEquals(filterExpression.left.getContent(), "level:info")
        assertEquals(filterExpression.right.getContent(), "message~:\"ser$\"")
    }

    @Test
    fun `should return OrExpression when parse or expression with extra space`() {
        val expression = "   tag:servicemanager   | message:get "
        val filterExpression = parser.parse(expression)
        filterExpression as? OrExpression ?: throw IllegalStateException("Filter expression is not an or expression")
        assertEquals(filterExpression.left.getContent(), "tag:servicemanager")
        assertEquals(filterExpression.right.getContent(), "message:get")
    }

    @Test
    fun `should return ParenthesesExpression when parse parentheses expression`() {
        val expression = "((tag:servicemanager | message:get) | (level:info & message~:\"ser$\"))"
        val filterExpression = parser.parse(expression)
        assertTrue(filterExpression is ParenthesesExpression)
        assertTrue(filterExpression.innerExpression is OrExpression)
        assertTrue((filterExpression.innerExpression as OrExpression).left is ParenthesesExpression)
        assertTrue((filterExpression.innerExpression as OrExpression).right is ParenthesesExpression)
        assertTrue(((filterExpression.innerExpression as OrExpression).left as ParenthesesExpression).innerExpression is OrExpression)
        assertTrue(((filterExpression.innerExpression as OrExpression).right as ParenthesesExpression).innerExpression is AndExpression)
    }

    @Test
    fun `should return LiteralExpression with regex value when parse LiteralExpression with regex value`() {
        val expression = "message~:\"ser$\""
        val filterExpression = parser.parse(expression)
        assertTrue(filterExpression is LiteralExpression)
        assertEquals(filterExpression.getContent(), "message~:\"ser$\"")
        assertEquals(filterExpression.key, FilterKey.Message)
        assert(filterExpression.value is RegexFilterValue)
        assertEquals(filterExpression.value.value, "\"ser$\"")
    }

    @Test
    fun `should return LiteralExpression with exclude key when parse LiteralExpression with exclude key`() {
        val expression = "-message~:\"ser$\""
        val filterExpression = parser.parse(expression)
        assertTrue(filterExpression is LiteralExpression)
        assertEquals(filterExpression.getContent(), "-message~:\"ser$\"")
        assertEquals(filterExpression.key, FilterKey.Message)
        assertEquals(filterExpression.isExclude, true)
    }

    @Test
    fun `should return LiteralExpression with message key when parse LiteralExpression with unknown key`() {
        val expression = "-app~:\"ser$\""
        val filterExpression = parser.parse(expression)
        assertTrue(filterExpression is LiteralExpression)
        assertEquals(filterExpression.key, FilterKey.Message)
        assertEquals(filterExpression.value.value, "-app~:\"ser\$\"")
    }

    @Test
    fun `should return InvalidLiteralExpression when parse literal expression with empty value`() {
        val expression = "message~: "
        val filterExpression = parser.parse(expression)
        assertTrue(filterExpression is InvalidLiteralExpression)
        assertEquals(filterExpression.getContent(), "message~:")
    }

    @Test
    fun `should return InvalidLiteralExpression when parse literal expression with multi separator`() {
        val expression = "message~:tag:asd "
        val filterExpression = parser.parse(expression)
        assertTrue(filterExpression is InvalidLiteralExpression)
    }
}