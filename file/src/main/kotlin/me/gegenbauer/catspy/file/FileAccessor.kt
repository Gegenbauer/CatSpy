package me.gegenbauer.catspy.file

interface FileAccessor {
    val path: String

    val linesCount: Int

    fun parseFile(): List<Line>

    fun getLine(line: Int): String

    fun getLines(start: Int, count: Int): List<String> {
        val lines = mutableListOf<String>()
        for (i in start until start + count) {
            lines.add(getLine(i))
        }
        return lines
    }

    fun dispose()

    fun cancel()

    data class Line(
        val offset: Long,
        val length: Int,
    )
}