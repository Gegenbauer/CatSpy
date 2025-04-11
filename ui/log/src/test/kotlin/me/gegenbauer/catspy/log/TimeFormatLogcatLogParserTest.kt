package me.gegenbauer.catspy.log

import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.log.parse.TimeFormatLogcatLogParser
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeFormatLogcatLogParserTest {

    @Test
    fun `should return correct parts when parse with time format log`() {
        val logcatLog = "05-04 20:55:41.461 I/Machine model(    0): linux,ranchu"

        val parser = TimeFormatLogcatLogParser()
        val result = parser.parse(logcatLog, LogParser.ParseMetadata(0, 0, 0, ""))

        val expected = listOf(
            "05-04 20:55:41.461",
            "I",
            "Machine model",
            "0",
            "linux,ranchu"
        )
        assertEquals(expected, result)
    }

    @Test
    fun `should return correct parts when parse with time format log without tag`() {
        val logcatLog = "05-04 20:55:41.461 I/ (    0): linux,ranchu"

        val parser = TimeFormatLogcatLogParser()
        val result = parser.parse(logcatLog, LogParser.ParseMetadata(0, 0, 0, ""))

        val expected = listOf(
            "05-04 20:55:41.461",
            "I",
            "",
            "0",
            "linux,ranchu"
        )
        assertEquals(expected, result)
    }
}