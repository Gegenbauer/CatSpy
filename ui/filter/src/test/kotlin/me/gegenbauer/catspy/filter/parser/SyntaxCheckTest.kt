package me.gegenbauer.catspy.filter.parser

import me.gegenbauer.catspy.filter.parser.FilterExpression.Companion.toFilterExpression
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyntaxCheckTest {
    /**
     * [isParenthesesExpression] should return true for a valid parentheses' expression.
     */
    @Test
    fun `should return true for valid parentheses expression`() {
        val expression = "(tag:servicemanager)".toFilterExpression()
        val result = isParenthesesExpression(expression)
        assertTrue(result)
    }

    @Test
    fun `should return true for valid composed parentheses expression`() {
        val expression = "(tag:servicemanager | message:get)".toFilterExpression()
        val result = isParenthesesExpression(expression)
        assertTrue(result)
    }

    @Test
    fun `should return false for invalid parentheses expression`() {
        val expression = "(tag:servicemanager | message:get) | (level:info & message~:\"ser$\")".toFilterExpression()
        val result = isParenthesesExpression(expression)
        assertFalse(result)
    }

    /**
     * [findInvalidIndexForUnpairedParentheses] should return the index of the invalid parentheses.
     */
    @Test
    fun `should return the index of the valid parentheses`() {
        val expression = "(tag:servicemanager | message:get) | (level:info & message~:\"ser$\")".toFilterExpression()
        val result = findInvalidIndexForUnpairedParentheses(expression)
        assertEquals(result, -1)
    }

    @Test
    fun `should return the index of the invalid parentheses`() {
        val expression = "(message~: ".toFilterExpression()
        val result = findInvalidIndexForUnpairedParentheses(expression)
        assertEquals(result, 10)
    }

    @Test
    fun `should return the index of the invalid parentheses 2`() {
        val expression = "message~: )".toFilterExpression()
        val result = findInvalidIndexForUnpairedParentheses(expression)
        assertEquals(result, 10)
    }

    @Test
    fun `should return the index of the invalid parentheses 3`() {
        val expression = "(message~: )".toFilterExpression()
        val result = findInvalidIndexForUnpairedParentheses(expression)
        assertEquals(result, -1)
    }

    @Test
    fun `should return correct index pairs for double quotes`() {
        val expression = "(\"message~: \")".toFilterExpression()
        assertEquals(expression.getQuotePairs(), listOf(QuoteIndexPair(1, 12)))
    }

    @Test
    fun `should return correct index pairs for double quotes when nested parentheses`() {
        val expression = "(\"asd\"(\"bcs\"))".toFilterExpression()
        assertEquals(expression.getQuotePairs(), listOf(QuoteIndexPair(1, 5), QuoteIndexPair(7, 11)))
    }

    @Test
    fun `should return correct index pairs for double quotes when having escaped double quotes in it`() {
        val expression = "(\"asd\\\"\"(\"bcs\"))".toFilterExpression()
        assertEquals(expression.getQuotePairs(), listOf(QuoteIndexPair(1, 7), QuoteIndexPair(9, 13)))
    }

    @Test
    fun `should return invalid index for unpaired double quotes`() {
        val expression = "(\"asd\"\"(\"bcs\"))".toFilterExpression()
        assertEquals(expression.getQuotePairs(), listOf(InvalidPosition(12, InvalidType.UNPAIRED_DOUBLE_QUOTE, TokenType.DOUBLE_QUOTE)))
    }

    @Test
    fun `should return last index for unpaired parentheses with parentheses count more than 0`() {
        val expression = "(\"asd\"(\"bcs\")".toFilterExpression()
        assertEquals(expression.getInvalidParenthesesIndex(), 0)
    }

    @Test
    fun `should return -1 for paired parentheses`() {
        val expression = "(\"asd\"(\"bcs\"))".toFilterExpression()
        assertEquals(expression.getInvalidParenthesesIndex(), -1)
    }

    @Test
    fun `should return specific index for unpaired parentheses with parentheses count less than 0`() {
        val expression = "(\"asd\"\"bcs\"))".toFilterExpression()
        assertEquals(expression.getInvalidParenthesesIndex(), 12)
    }
}