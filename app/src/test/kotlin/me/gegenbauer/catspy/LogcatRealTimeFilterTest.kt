package me.gegenbauer.catspy

import me.gegenbauer.catspy.common.log.parsePattern
import kotlin.test.Test
import kotlin.test.assertEquals

class LogcatRealTimeFilterTest {

    @Test
    fun `should return correct negative pattern when parse string with negative pattern`() {
        val pattern = "-Activity|-Service"
        val expected = "Activity|Service"
        val actual = parsePattern(pattern).second
        assertEquals(expected, actual)
    }
}