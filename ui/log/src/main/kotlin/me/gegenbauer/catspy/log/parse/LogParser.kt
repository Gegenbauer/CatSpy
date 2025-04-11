package me.gegenbauer.catspy.log.parse

fun interface LogParser {

    fun parse(
        line: String,
        parseMetadata: ParseMetadata
    ): List<String>

    data class ParseMetadata(
        val columnCount: Int,
        val messageColumn: Int,
        val levelColumn: Int,
        val defaultLevelTag: String
    )

    companion object {
        val empty = LogParser { _, _ ->
            return@LogParser emptyList()
        }

        val defaultParseMetadata = ParseMetadata(0, 0, 0, "")
    }
}

interface SequenceLogParser {
    fun parse(
        lines: String,
        parseMetadata: LogParser.ParseMetadata
    ): List<String>
}