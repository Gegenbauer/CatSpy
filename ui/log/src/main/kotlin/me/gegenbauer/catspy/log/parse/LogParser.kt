package me.gegenbauer.catspy.log.parse

fun interface LogParser {

    fun parse(line: String): List<String>

    companion object {
        val empty = LogParser {
            return@LogParser emptyList()
        }
    }
}