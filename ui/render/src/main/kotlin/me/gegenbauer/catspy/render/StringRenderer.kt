package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.cache.CacheableObject
import java.awt.Color

interface StringRenderer: CacheableObject {

    val raw: String

    fun updateRaw(raw: String): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun bold(start: Int, end: Int): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun italic(start: Int, end: Int): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun strikethrough(start: Int, end: Int): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun highlight(start: Int, end: Int, color: Color): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun foreground(start: Int, end: Int, color: Color): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun underline(start: Int, end: Int): StringRenderer

    fun render()

    fun clear()
}