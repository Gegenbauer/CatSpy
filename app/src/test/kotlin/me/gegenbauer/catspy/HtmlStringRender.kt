package me.gegenbauer.catspy

import me.gegenbauer.catspy.utils.toHtml
import java.awt.Color
import kotlin.math.roundToInt

class HtmlStringRender {
    private val spans = mutableListOf<Span>()

    fun bold(start: Int, end: Int) {
        spans.add(Span(start, end, SpanType.BOLD))
    }

    fun italic(start: Int, end: Int) {
        spans.add(Span(start, end, SpanType.ITALIC))
    }

    fun strikethrough(start: Int, end: Int) {
        spans.add(Span(start, end, SpanType.STRIKETHROUGH))
    }

    fun highlight(start: Int, end: Int, color: Color) {
        spans.add(Span(start, end, SpanType.HIGHLIGHT, color))
    }

    fun foreground(start: Int, end: Int, color: Color) {
        spans.add(Span(start, end, SpanType.FOREGROUND, color))
    }

    fun underline(start: Int, end: Int) {
        spans.add(Span(start, end, SpanType.UNDERLINE))
    }

    //            render.bold(0, 5);
    //            render.highlight(0, 10, Color.BLUE);
    //            render.highlight(4, 12, Color.RED);
    //            render.italic(4, 12);

    // 更新 render 方法
// Update render method
// Update render method
    fun render(string: String): String {
        if (spans.isEmpty()) {
            return "<html>$string</html>"
        }

        val spanPoints = sortedSetOf<Int>()
        spans.forEach {
            spanPoints.add(it.start)
            spanPoints.add(it.end)
        }
        spanPoints.add(0)
        spanPoints.add(string.length - 1)

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
            val css = it.map { span -> span.css() }.joinToString("")
            result.append("<span style=\"$css\">${string.substring(start, end + 1)}</span>")
        }
        result.append("</html>")

        return result.toString()
    }

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