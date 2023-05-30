package me.gegenbauer.catspy.filter

import me.gegenbauer.catspy.ui.filter.FilterExpression.Companion.toFilterExpression
import me.gegenbauer.catspy.ui.filter.FilterParenthesesExpression
import kotlin.test.Test


class FilterParenthesesExpressionTest {

    /**
     * [FilterParenthesesExpression.isParenthesesExpression] should return true for a valid parentheses' expression.
     */
    @Test
    fun `should return true for valid parentheses expression`() {
        val expression = "(tag:servicemanager)".toFilterExpression()
        val result = FilterParenthesesExpression.isParenthesesExpression(expression)
        assert(result)
    }

    @Test
    fun `should return true for valid composed parentheses expression`() {
        val expression = "(tag:servicemanager | message:get)".toFilterExpression()
        val result = FilterParenthesesExpression.isParenthesesExpression(expression)
        assert(result)
    }

    @Test
    fun `should return false for invalid parentheses expression`() {
        val expression = "(tag:servicemanager | message:get) | (level:info & message~:\"ser$\")".toFilterExpression()
        val result = FilterParenthesesExpression.isParenthesesExpression(expression)
        assert(!result)
    }

    /**
     * [FilterParenthesesExpression.findInvalidIndexForUnpairedParentheses] should return the index of the invalid parentheses.
     */
    @Test
    fun `should return the index of the valid parentheses`() {
        val expression = "(tag:servicemanager | message:get) | (level:info & message~:\"ser$\")".toFilterExpression()
        val result = FilterParenthesesExpression.findInvalidIndexForUnpairedParentheses(expression)
        assert(result == -1)
    }

    @Test
    fun `should return the index of the invalid parentheses`() {
        val expression = "(message~: ".toFilterExpression()
        val result = FilterParenthesesExpression.findInvalidIndexForUnpairedParentheses(expression)
        assert(result == 10)
    }

    @Test
    fun `should return the index of the invalid parentheses 2`() {
        val expression = "message~: )".toFilterExpression()
        val result = FilterParenthesesExpression.findInvalidIndexForUnpairedParentheses(expression)
        assert(result == 10)
    }

    @Test
    fun `should return the index of the invalid parentheses 3`() {
        val expression = "(message~: )".toFilterExpression()
        val result = FilterParenthesesExpression.findInvalidIndexForUnpairedParentheses(expression)
        assert(result == -1)
    }
}