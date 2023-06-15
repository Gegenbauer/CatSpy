package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.render.html.HtmlStringRender
import java.awt.Color

interface StringRender {

    val raw: String

    fun bold(start: Int, end: Int)

    fun italic(start: Int, end: Int)

    fun strikethrough(start: Int, end: Int)

    fun highlight(start: Int, end: Int, color: Color)

    fun foreground(start: Int, end: Int, color: Color)

    fun underline(start: Int, end: Int)

    fun render(): String

    fun clear(type: HtmlStringRender.SpanType)
}