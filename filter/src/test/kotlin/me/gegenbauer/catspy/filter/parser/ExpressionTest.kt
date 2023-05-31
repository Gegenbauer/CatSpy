package me.gegenbauer.catspy.filter.parser

import me.gegenbauer.catspy.filter.parser.FilterExpression.Companion.toFilterExpression
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpressionTest {

    @Test
    fun `should return filter expression with correct start and end index for a string expression without blank character`() {
        val expression = "level:info".toFilterExpression()
        assertEquals(expression.start, 0)
        assertEquals(expression.end, 9)
    }

    @Test
    fun `should return filter expression with correct start and end index for a string expression with blank character`() {
        val expression = "  level:info  ".toFilterExpression()
        assertEquals(expression.start, 2)
        assertEquals(expression.end, 11)
    }

    @Test
    fun `should return int range with correct start and end index for a filter expression with blank character`() {
        val expression = FilterExpression("ab c  d ef", 2, 7)
        val trimmedExpression = expression.trim()
        assertEquals(trimmedExpression.start, 3)
        assertEquals(trimmedExpression.end, 6)
    }
}