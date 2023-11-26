package me.gegenbauer.catspy.view.table

import me.gegenbauer.catspy.view.filter.parsePattern
import kotlin.test.Test
import kotlin.test.assertEquals

class LogcatFilterTest {

    @Test
    fun `should return correct negative pattern when parse string with negative pattern`() {
        val pattern = "-Activity|-Service"
        val expected = "Activity|Service"
        val actual = parsePattern(pattern).second
        assertEquals(expected, actual)
    }
}