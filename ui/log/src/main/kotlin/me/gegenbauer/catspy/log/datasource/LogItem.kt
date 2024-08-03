package me.gegenbauer.catspy.log.datasource

class LogItem(
    val num: Int,
    val logLine: String,
    private val parts: List<String>
) {

    fun getPart(index: Int): String {
        return if (index >= parts.size) "" else parts[index]
    }
}