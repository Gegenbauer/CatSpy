package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.cache.ObjectPool
import me.gegenbauer.catspy.context.MemoryState
import java.awt.Color

/**
 * Renders a string with HTML tags.
 */
class LabelRenderer : StringRenderer {

    override var raw: String = ""
        private set

    private val spans = mutableListOf<Span>()

    override fun updateRaw(raw: String): StringRenderer {
        this.raw = raw
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun bold(start: Int, end: Int): StringRenderer {
        spans.add(Span(start, end, SpanType.BOLD))
        return this
    }

    override fun bold(): StringRenderer {
        spans.add(Span(0, raw.length - 1, SpanType.BOLD))
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun italic(start: Int, end: Int): StringRenderer {
        spans.add(Span(start, end, SpanType.ITALIC))
        return this
    }

    override fun italic(): StringRenderer {
        spans.add(Span(0, raw.length - 1, SpanType.ITALIC))
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun strikethrough(start: Int, end: Int): StringRenderer {
        spans.add(Span(start, end, SpanType.STRIKETHROUGH))
        return this
    }

    override fun strikethrough(): StringRenderer {
        spans.add(Span(0, raw.length - 1, SpanType.STRIKETHROUGH))
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun highlight(start: Int, end: Int, color: Color): StringRenderer {
        spans.add(Span(start, end, SpanType.HIGHLIGHT, color))
        return this
    }

    override fun highlight(color: Color): StringRenderer {
        spans.add(Span(0, raw.length - 1, SpanType.HIGHLIGHT, color))
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun foreground(start: Int, end: Int, color: Color): StringRenderer {
        spans.add(Span(start, end, SpanType.FOREGROUND, color))
        return this
    }

    override fun foreground(color: Color): StringRenderer {
        spans.add(Span(0, raw.length - 1, SpanType.FOREGROUND, color))
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun underline(start: Int, end: Int): StringRenderer {
        spans.add(Span(start, end, SpanType.UNDERLINE))
        return this
    }

    override fun underline(): StringRenderer {
        spans.add(Span(0, raw.length - 1, SpanType.UNDERLINE))
        return this
    }

    override fun processRenderResult(result: RenderResult) {
        val backgroundColor = getBackgroundColor()
        if (backgroundColor.isValid()) {
            result.background = backgroundColor
        }
        if (isComplexityLow()) {
            val foreground = getForegroundColor()
            if (foreground.isValid()) {
                result.foreground = foreground
            }
            result.rendered = raw
            return
        }

        val builder = HtmlStringBuilder()
        builder.append(renderWithoutTags())
        result.rendered = builder.build()
    }

    fun renderWithoutTags(): String {
        if (raw.isEmpty() || spans.isEmpty()) {
            return raw
        }

        val builder = HtmlStringBuilder(false)
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
     * Divide the spans into non-overlapping intervals according to their start and end.
     * Handle cases where intervals overlap; if they do, divide them into multiple intervals.
     * For example: intervals [0, 10] and [5, 15] should be divided into [0, 4], [5, 10], [11, 15]
     * For example: intervals [0, 0] and [0, 3] should be divided into [0, 0], [1, 3]
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
                it.last().color
            } else {
                it.first().color
            }
            mergedSpan.add(Span(it.first().start, it.first().end, it.first().type, color))
        }
        return mergedSpan
    }

    override fun clear() {
        spans.clear()
    }

    private fun isComplexityLow(): Boolean {
        val foregroundSpans = spans.filter { it.type == SpanType.FOREGROUND }
        val highlightSpans = spans.filter { it.type == SpanType.HIGHLIGHT }

        val isForegroundLowComplexity =
            foregroundSpans.isEmpty() || (foregroundSpans.all { it.start == 0 && it.end >= raw.length - 1 })
        val isHighlightLowComplexity =
            highlightSpans.isEmpty() || (highlightSpans.all { it.start == 0 && it.end >= raw.length - 1 })

        return isForegroundLowComplexity && isHighlightLowComplexity
    }

    private fun getForegroundColor(): Color {
        val foregroundSpans = spans.filter { it.type == SpanType.FOREGROUND }
        return if (foregroundSpans.isEmpty()) {
            INVALID_COLOR
        } else {
            foregroundSpans.last().color
        }
    }

    private fun getBackgroundColor(): Color {
        val highlightSpans = spans.filter { it.type == SpanType.HIGHLIGHT && it.start == 0 && it.end >= raw.length - 1 }
        return if (highlightSpans.isEmpty()) {
            INVALID_COLOR
        } else {
            highlightSpans.last().color
        }
    }

    private val String.formatted: String
        get() = replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;")

    private data class Span(
        val start: Int,
        val end: Int,
        val type: SpanType,
        val color: Color = Color.BLACK,
        val replace: Boolean = false
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

    override fun recycle() {
        clear()
        pool.recycle(this)
    }

    companion object {
        private val pool = object : ObjectPool<LabelRenderer>(1000) {

            init {
                MemoryState.register(this)
            }

            override fun create(): LabelRenderer {
                return LabelRenderer()
            }
        }

        fun obtain(): LabelRenderer {
            return pool.obtain()
        }
    }
}