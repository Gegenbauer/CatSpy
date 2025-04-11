package me.gegenbauer.catspy.log.parse

import java.util.regex.Pattern

class TimeFormatLogcatLogParser : SequenceLogParser {
    private val pattern =
        Pattern.compile("(\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s([A-Z])/?(.*?)\\((\\s*\\w+)\\):\\s(.*)")
    /**
     * 05-04 20:55:41.461 I/Machine model(    0): linux,ranchu
     */
    override fun parse(lines: String, parseMetadata: LogParser.ParseMetadata): List<String> {
        val result = mutableListOf<String>()

        val matcher = pattern.matcher(lines)
        while (matcher.find()) {
            val timestamp = matcher.group(1)
            val level = matcher.group(2)
            val tag = matcher.group(3)?.trim()
            val pid = matcher.group(4).trim()
            val message = matcher.group(5).trim()
            result.add(timestamp)
            result.add(level)
            result.add(tag ?: "")
            result.add(pid)
            result.add(message)
        }

        return result
    }
}