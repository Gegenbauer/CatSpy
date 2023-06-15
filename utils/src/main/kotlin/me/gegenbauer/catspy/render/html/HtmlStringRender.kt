package me.gegenbauer.catspy.render.html

import me.gegenbauer.catspy.render.StringRender
import me.gegenbauer.catspy.utils.toHtml
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Renders a string with HTML tags.
 */
class HtmlStringRender(override val raw: String) : StringRender {
    private val spans = mutableListOf<Span>()

    override fun bold(start: Int, end: Int) {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.BOLD))
        }
    }

    override fun italic(start: Int, end: Int) {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.ITALIC))
        }
    }

    override fun strikethrough(start: Int, end: Int) {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.STRIKETHROUGH))
        }
    }

    override fun highlight(start: Int, end: Int, color: Color) {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.HIGHLIGHT, color))
        }
    }

    override fun foreground(start: Int, end: Int, color: Color) {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.FOREGROUND, color))
        }
    }

    override fun underline(start: Int, end: Int) {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.UNDERLINE))
        }
    }

    private fun checkIndex(start: Int, end: Int): Boolean {
        return start >= 0 && end < raw.length && start < end
    }

    override fun render(): String {
        if (spans.isEmpty()) {
            return raw
        }

        val spanPoints = sortedSetOf<Int>()
        spans.forEach {
            spanPoints.add(it.start)
            spanPoints.add(it.end)
        }
        spanPoints.add(0)
        spanPoints.add(raw.length - 1)

        // 根据所有 start 和 end 重新划分 span
        val subSpans = mutableListOf<Span>()
        for (i in 0 until spanPoints.size - 1) {
            val start = spanPoints.elementAt(i)
            val end = spanPoints.elementAt(i + 1) - 1

            val coveringSpans = spans.filter { start >= it.start && end <= it.end }
            coveringSpans.forEach {
                subSpans.add(Span(start, end, it.type, it.color))
            }
            if (coveringSpans.isEmpty()) {
                subSpans.add(Span(start, end, SpanType.NORMAL))
            }
        }

        // 合并重叠 span
        val overlappedSameTypeSpans = subSpans.groupBy { it.start.hashCode() + it.type.hashCode() }
            .map { it.value }

        val mergedSpan = mutableListOf<Span>()

        overlappedSameTypeSpans.forEach {
            // color 不为空的，则计算 color 平均值
            // 否则只取一个
            val color = if (it.first().color != null) {
                averageColor(it.map { span -> span.color!! })
            } else {
                it.first().color
            }
            mergedSpan.add(Span(it.first().start, it.first().end, it.first().type, color))
        }

        val spansGroupByStart = mergedSpan.groupBy { it.start }.map { it.value }

        val result = StringBuilder("<html>")
        spansGroupByStart.forEach {
            val start = it.first().start
            val end = it.first().end
            val css = it.joinToString("") { span -> span.css() }
            result.append("<span style=\"$css\">${raw.substring(start, end + 1).formatted}</span>")
        }
        result.append("</html>")

        return result.toString()
    }

    override fun clear(type: SpanType) {
        spans.removeAll { it.type == type }
    }

    private val String.formatted: String
        get() = replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;")

    private fun averageColor(colors: List<Color>): Color {
        val r = colors.map { it.red }.average().roundToInt()
        val g = colors.map { it.green }.average().roundToInt()
        val b = colors.map { it.blue }.average().roundToInt()
        return Color(r, g, b)
    }

    private data class Span(
        val start: Int,
        val end: Int,
        val type: SpanType,
        val color: Color? = null
    ) {
        fun css(): String = when (type) {
            SpanType.NORMAL -> ""
            SpanType.BOLD -> "font-weight:bold;"
            SpanType.ITALIC -> "font-style:italic;"
            SpanType.UNDERLINE -> "text-decoration:underline;"
            SpanType.STRIKETHROUGH -> "text-decoration:line-through;"
            SpanType.HIGHLIGHT -> "background-color:${color!!.toHtml()};"
            SpanType.FOREGROUND -> "color:${color!!.toHtml()};"
        }
    }

    enum class SpanType {
        NORMAL, BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, HIGHLIGHT, FOREGROUND
    }


}