package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.cache.CacheableObject
import me.gegenbauer.catspy.cache.ObjectPool
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.awt.Color

interface StringRenderer : CacheableObject {

    val raw: String

    fun updateRaw(raw: String): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun bold(start: Int, end: Int): StringRenderer

    fun bold(): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun italic(start: Int, end: Int): StringRenderer

    fun italic(): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun strikethrough(start: Int, end: Int): StringRenderer

    fun strikethrough(): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun highlight(start: Int, end: Int, color: Color): StringRenderer

    fun highlight(color: Color): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun foreground(start: Int, end: Int, color: Color): StringRenderer

    fun foreground(color: Color): StringRenderer

    /**
     * @param start inclusive
     * @param end inclusive
     */
    fun underline(start: Int, end: Int): StringRenderer

    fun underline(): StringRenderer

    fun processRenderResult(result: RenderResult)

    fun clear()
}

val INVALID_COLOR = Color(0, 0, 0, 0)

fun Color.isValid(): Boolean {
    return this !== INVALID_COLOR
}

data class RenderResult(
    var raw: String,
    var rendered: String,
    var foreground: Color,
    var background: Color
) : CacheableObject {

    override fun recycle() {
        raw = EMPTY_STRING
        rendered = EMPTY_STRING
        foreground = INVALID_COLOR
        background = INVALID_COLOR

        pool.recycle(this)
    }

    companion object {
        private val pool = ObjectPool.createMemoryAwarePool(1000) {
            RenderResult(EMPTY_STRING, EMPTY_STRING, INVALID_COLOR, INVALID_COLOR)
        }

        fun obtain(): RenderResult {
            return pool.obtain()
        }
    }
}