package me.gegenbauer.catspy.filter

import me.gegenbauer.catspy.ui.filter.FilterExpression
import me.gegenbauer.catspy.ui.filter.FilterExpression.Companion.toFilterExpression
import kotlin.test.Test

class FilterExpressionTest {

    @Test
    fun `should return filter expression with correct start and end index for a string expression without blank character`() {
        val expression = "level:info".toFilterExpression()
        assert(expression.start == 0)
        assert(expression.end == 9)
    }

    @Test
    fun `should return filter expression with correct start and end index for a string expression with blank character`() {
        val expression = "  level:info  ".toFilterExpression()
        assert(expression.start == 2)
        assert(expression.end == 11)
    }

    @Test
    fun `should return int range with correct start and end index for a filter expression with blank character`() {
        val expression = FilterExpression("ab c  d ef", 2, 7)
        val trimmedExpression = expression.trim()
        assert(trimmedExpression.start == 3)
        assert(trimmedExpression.end == 6)
    }
}