package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.render.html.HtmlStringRenderer
import java.awt.Color

interface StringRenderer {

    val raw: String

    fun bold(start: Int, end: Int): StringRenderer

    fun italic(start: Int, end: Int): StringRenderer

    fun strikethrough(start: Int, end: Int): StringRenderer

    fun highlight(start: Int, end: Int, color: Color): StringRenderer

    fun foreground(start: Int, end: Int, color: Color): StringRenderer

    fun underline(start: Int, end: Int): StringRenderer

    fun render(): String

    fun clear(type: HtmlStringRenderer.SpanType)
}