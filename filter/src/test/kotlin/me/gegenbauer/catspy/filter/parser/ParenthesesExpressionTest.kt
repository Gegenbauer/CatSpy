package me.gegenbauer.catspy.filter.parser

import me.gegenbauer.catspy.filter.parser.FilterExpression.Companion.toFilterExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class ParenthesesExpressionTest {

    /**
     * [ParenthesesExpression.isParenthesesExpression] should return true for a valid parentheses' expression.
     */
    @Test
    fun `should return true for valid parentheses expression`() {
        val expression = "(tag:servicemanager)".toFilterExpression()
        val result = ParenthesesExpression.isParenthesesExpression(expression)
        assertTrue(result)
    }

    @Test
    fun `should return true for valid composed parentheses expression`() {
        val expression = "(tag:servicemanager | message:get)".toFilterExpression()
        val result = ParenthesesExpression.isParenthesesExpression(expression)
        assertTrue(result)
    }

    @Test
    fun `should return false for invalid parentheses expression`() {
        val expression = "(tag:servicemanager | message:get) | (level:info & message~:\"ser$\")".toFilterExpression()
        val result = ParenthesesExpression.isParenthesesExpression(expression)
        assertFalse(result)
    }

    /**
     * [ParenthesesExpression.findInvalidIndexForUnpairedParentheses] should return the index of the invalid parentheses.
     */
    @Test
    fun `should return the index of the valid parentheses`() {
        val expression = "(tag:servicemanager | message:get) | (level:info & message~:\"ser$\")".toFilterExpression()
        val result = ParenthesesExpression.findInvalidIndexForUnpairedParentheses(expression)
        assertEquals(result, -1)
    }

    @Test
    fun `should return the index of the invalid parentheses`() {
        val expression = "(message~: ".toFilterExpression()
        val result = ParenthesesExpression.findInvalidIndexForUnpairedParentheses(expression)
        assertEquals(result, 10)
    }

    @Test
    fun `should return the index of the invalid parentheses 2`() {
        val expression = "message~: )".toFilterExpression()
        val result = ParenthesesExpression.findInvalidIndexForUnpairedParentheses(expression)
        assertEquals(result, 10)
    }

    @Test
    fun `should return the index of the invalid parentheses 3`() {
        val expression = "(message~: )".toFilterExpression()
        val result = ParenthesesExpression.findInvalidIndexForUnpairedParentheses(expression)
        assertEquals(result, -1)
    }
}