package me.gegenbauer.catspy.render

import java.awt.Color
import kotlin.math.roundToInt

/**
 * Renders a string with HTML tags.
 */
class HtmlStringRenderer(override val raw: String) : StringRenderer {
    private val spans = mutableListOf<Span>()

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun bold(start: Int, end: Int): StringRenderer {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.BOLD))
        }
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun italic(start: Int, end: Int): StringRenderer {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.ITALIC))
        }
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun strikethrough(start: Int, end: Int): StringRenderer {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.STRIKETHROUGH))
        }
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun highlight(start: Int, end: Int, color: Color): StringRenderer {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.HIGHLIGHT, color))
        }
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun foreground(start: Int, end: Int, color: Color): StringRenderer {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.FOREGROUND, color))
        }
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun underline(start: Int, end: Int): StringRenderer {
        if (checkIndex(start, end)) {
            spans.add(Span(start, end, SpanType.UNDERLINE))
        }
        return this
    }

    private fun checkIndex(start: Int, end: Int): Boolean {
        return start >= 0 && end < raw.length && start <= end
    }

    override fun render(): String {
        if (raw.isEmpty() || spans.isEmpty()) {
            return raw
        }

        val builder = TaggedStringBuilder()
        builder.addTag(Tag.HTML)
        builder.append(renderWithoutTags())
        builder.closeTag()
        return builder.build()
    }

    override fun renderWithoutTags(): String {
        if (raw.isEmpty() || spans.isEmpty()) {
            return raw
        }

        val builder = TaggedStringBuilder()
        spans.add(Span(0, raw.length - 1, SpanType.NORMAL))
        val splitIntervals = splitStringIntoIntervals(spans)
        val splitSpans = getSplitSpans(splitIntervals)

        val mergedSpans = mergeSpans(splitSpans)
        val spansGroupByStart = mergedSpans.groupBy { it.start }.map { it.value }
        spansGroupByStart.forEach {
            val start = it.first().start
            val end = it.first().end
            val styledSpan = it.filter { span -> span.type != SpanType.NORMAL }
            if (styledSpan.isEmpty()) {
                builder.append(raw.substring(start, end + 1).formatted)
            } else {
                val css = styledSpan.joinToString("") { span -> span.css() }
                builder.addTag(Tag.SPAN, "style=\"$css\"")
                builder.append(raw.substring(start, end + 1).formatted)
                builder.closeTag(Tag.SPAN)
            }
        }
        return builder.build()
    }

    /**
     * 将 span 按照 start 和 end 划分成不重叠的区间
     * 需要处理区间存在交集的情况，如果存在交集，则需要划分成多个区间
     * 例如：区间 [0, 10] 和 [5, 15]，则需要划分成 [0, 4], [5, 10], [11, 15]
     * 例如：区间 [0, 0] 和 [0, 3]，则需要划分成 [0, 0], [1, 3]
     */
    private fun splitStringIntoIntervals(spans: List<Span>): List<IntRange> {
        val points = mutableListOf<Pair<Int, Int>>()
        for (span in spans) {
            points.add(Pair(span.start, 1))
            points.add(Pair(span.end + 1, -1))
        }
        points.sortBy { it.first }

        var count = 0
        var start = 0
        val result = mutableListOf<IntRange>()
        for ((point, delta) in points) {
            if (count > 0 && point != start) {
                result.add(start until point)
            }
            start = point
            count += delta
        }

        return result
    }

    private fun getSplitSpans(splitIntervals: List<IntRange>): List<Span> {
        // 根据所有 start 和 end 重新划分 span
        val splitSpans = mutableListOf<Span>()
        for (i in splitIntervals.indices) {
            val start = splitIntervals[i].first
            val end = splitIntervals[i].last
            val coveringSpans = spans.filter { start >= it.start && end <= it.end }
            coveringSpans.forEach {
                splitSpans.add(Span(start, end, it.type, it.color))
            }
            if (coveringSpans.isEmpty()) {
                splitSpans.add(Span(start, end, SpanType.NORMAL))
            }
        }
        return splitSpans
    }

    private fun mergeSpans(spans: List<Span>): List<Span> {
        // 合并重叠 span
        val overlappedSameTypeSpans = spans.groupBy { it.start.hashCode() + it.type.hashCode() }.map { it.value }

        val mergedSpan = mutableListOf<Span>()

        overlappedSameTypeSpans.forEach {
            // color 不为空的，则计算 color 平均值
            // 否则只取一个
            val color = if (it.first().isColorType()) {
                averageColor(it.map { span -> span.color })
            } else {
                it.first().color
            }
            mergedSpan.add(Span(it.first().start, it.first().end, it.first().type, color))
        }
        return mergedSpan
    }

    override fun clear(type: SpanType) {
        spans.removeAll { it.type == type }
    }

    private val String.formatted: String
        get() = replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;")

    private fun averageColor(colors: List<Color>): Color {
        val alpha = colors.map { it.alpha }.average().roundToInt()
        val r = colors.map { it.red }.average().roundToInt()
        val g = colors.map { it.green }.average().roundToInt()
        val b = colors.map { it.blue }.average().roundToInt()
        return Color(r, g, b, alpha)
    }

    private data class Span(
        val start: Int,
        val end: Int,
        val type: SpanType,
        val color: Color = Color.BLACK
    ) {
        fun css(): String = when (type) {
            SpanType.NORMAL -> ""
            SpanType.BOLD -> "font-weight:bold;"
            SpanType.ITALIC -> "font-style:italic;"
            SpanType.UNDERLINE -> "text-decoration:underline;"
            SpanType.STRIKETHROUGH -> "text-decoration:line-through;"
            SpanType.HIGHLIGHT -> "background-color:${color.toHtml()};"
            SpanType.FOREGROUND -> "color:${color.toHtml()};"
        }
    }

    enum class SpanType {
        NORMAL, BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, HIGHLIGHT, FOREGROUND
    }

    private fun Span.isColorType(): Boolean {
        return this.type == SpanType.HIGHLIGHT || this.type == SpanType.FOREGROUND
    }
}