package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.render.html.HtmlStringRender
import java.awt.Color

interface StringRender {

    val raw: String

    fun bold(start: Int, end: Int): StringRender

    fun italic(start: Int, end: Int): StringRender

    fun strikethrough(start: Int, end: Int): StringRender

    fun highlight(start: Int, end: Int, color: Color): StringRender

    fun foreground(start: Int, end: Int, color: Color): StringRender

    fun underline(start: Int, end: Int): StringRender

    fun render(): String

    fun clear(type: HtmlStringRender.SpanType)
}