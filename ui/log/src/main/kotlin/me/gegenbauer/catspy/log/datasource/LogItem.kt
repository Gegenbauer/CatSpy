package me.gegenbauer.catspy.log.datasource

import me.gegenbauer.catspy.java.ext.EMPTY_STRING

class LogItem(
    val num: Int,
    private val parts: List<String>
) {
    fun getPart(index: Int): String {
        return if (index >= parts.size) EMPTY_STRING else parts[index]
    }

    constructor(tag: String, content: String): this(0, listOf(tag, content))

    override fun toString(): String {
        return parts.joinToString(" ")
    }
}