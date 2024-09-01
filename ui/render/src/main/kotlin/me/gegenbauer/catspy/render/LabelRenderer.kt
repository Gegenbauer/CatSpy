package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.cache.CacheableObject
import me.gegenbauer.catspy.cache.ObjectPool
import me.gegenbauer.catspy.cache.use
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.utils.string.CacheableStringBuilder
import me.gegenbauer.catspy.utils.string.SimpleStringBuilder
import me.gegenbauer.catspy.utils.ui.toHtml
import java.awt.Color

/**
 * Renders a string with HTML tags.
 */
class LabelRenderer : StringRenderer {

    override var raw: String = EMPTY_STRING
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
        val span = Span.obtain()
        span.set(start, end, SpanType.BOLD)
        spans.add(span)
        return this
    }

    override fun bold(): StringRenderer {
        val span = Span.obtain()
        span.set(0, raw.length - 1, SpanType.BOLD)
        spans.add(span)
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun italic(start: Int, end: Int): StringRenderer {
        val span = Span.obtain()
        span.set(start, end, SpanType.ITALIC)
        spans.add(span)
        return this
    }

    override fun italic(): StringRenderer {
        val span = Span.obtain()
        span.set(0, raw.length - 1, SpanType.ITALIC)
        spans.add(span)
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun strikethrough(start: Int, end: Int): StringRenderer {
        val span = Span.obtain()
        span.set(start, end, SpanType.STRIKETHROUGH)
        spans.add(span)
        return this
    }

    override fun strikethrough(): StringRenderer {
        val span = Span.obtain()
        span.set(0, raw.length - 1, SpanType.STRIKETHROUGH)
        spans.add(span)
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun highlight(start: Int, end: Int, color: Color): StringRenderer {
        val span = Span.obtain()
        span.set(start, end, SpanType.HIGHLIGHT, color)
        spans.add(span)
        return this
    }

    override fun highlight(color: Color): StringRenderer {
        val span = Span.obtain()
        span.set(0, raw.length - 1, SpanType.HIGHLIGHT, color)
        spans.add(span)
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun foreground(start: Int, end: Int, color: Color): StringRenderer {
        val span = Span.obtain()
        span.set(start, end, SpanType.FOREGROUND, color)
        spans.add(span)
        return this
    }

    override fun foreground(color: Color): StringRenderer {
        val span = Span.obtain()
        span.set(0, raw.length - 1, SpanType.FOREGROUND, color)
        spans.add(span)
        return this
    }

    /**
     * @param start inclusive
     * @param end inclusive
     */
    override fun underline(start: Int, end: Int): StringRenderer {
        val span = Span.obtain()
        span.set(start, end, SpanType.UNDERLINE)
        spans.add(span)
        return this
    }

    override fun underline(): StringRenderer {
        val span = Span.obtain()
        span.set(0, raw.length - 1, SpanType.UNDERLINE)
        spans.add(span)
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
        // remove highlight if it covers the whole length, as it is handled by background color
        removeWholeLengthHighlight()

        result.rendered = HtmlStringBuilder.obtain().use {
            it.isHtmlTagInitialized = true
            it.append(renderWithoutTags())
            it.build()
        }
    }

    private fun removeWholeLengthHighlight() {
        val iterator = spans.iterator()
        while (iterator.hasNext()) {
            val span = iterator.next()
            if (span.type == SpanType.HIGHLIGHT && span.start == 0 && span.end >= raw.length - 1) {
                iterator.remove()
                span.recycle()
            }
        }
    }

    fun renderWithoutTags(): String {
        if (raw.isEmpty() || spans.isEmpty()) {
            return raw
        }

        val builder = HtmlStringBuilder.obtain()
        val helpSpan = Span.obtain()
        helpSpan.set(0, raw.length - 1, SpanType.NORMAL)
        spans.add(helpSpan)
        val splitIntervals = splitStringIntoIntervals(spans)
        val splitSpans = getSplitSpans(splitIntervals)

        val mergedSpans = mergeSpans(splitSpans)
        val spansGroupByStart = mergedSpans.groupBy { it.start }.map { it.value }
        spansGroupByStart.forEach { spanGroup ->
            val start = spanGroup.first().start
            val end = spanGroup.first().end
            val styledSpan = spanGroup.filter { span -> span.type != SpanType.NORMAL }
            if (styledSpan.isEmpty()) {
                builder.append(
                    CacheableStringBuilder.obtain().use {
                        it.set(raw).substring(start, end + 1).formatted.build()
                    }
                )
            } else {
                val css = styledSpan.joinToString(EMPTY_STRING) { span -> span.css() }
                builder.addTag(Tag.SPAN, "style=\"$css\"")
                builder.append(
                    CacheableStringBuilder.obtain().use {
                        it.set(raw).substring(start, end + 1).formatted.build()
                    }
                )
                builder.closeTag(Tag.SPAN)
            }
        }
        mergedSpans.forEach { it.recycle() }
        splitSpans.forEach { it.recycle() }
        return builder.use { it.build() }
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
                val span = Span.obtain()
                span.set(start, end, it.type, it.color)
                splitSpans.add(span)
            }
            if (coveringSpans.isEmpty()) {
                val span = Span.obtain()
                span.set(start, end, SpanType.NORMAL)
                splitSpans.add(span)
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
            val span = Span.obtain()
            span.set(it.first().start, it.last().end, it.first().type, color)
            mergedSpan.add(span)
        }
        return mergedSpan
    }

    override fun clear() {
        spans.forEach(Span::recycle)
        spans.clear()
    }

    private fun isComplexityLow(): Boolean {
        val isForegroundLowComplexity = spans.none { it.type == SpanType.FOREGROUND } ||
                spans.all { it.type != SpanType.FOREGROUND || (it.start == 0 && it.end >= raw.length - 1) }

        val isHighlightLowComplexity = spans.none { it.type == SpanType.HIGHLIGHT } ||
                spans.all { it.type != SpanType.HIGHLIGHT || (it.start == 0 && it.end >= raw.length - 1) }

        return isForegroundLowComplexity && isHighlightLowComplexity
    }

    private fun getForegroundColor(): Color {
        val foregroundSpan = spans.lastOrNull { it.type == SpanType.FOREGROUND }
        return foregroundSpan?.color ?: INVALID_COLOR
    }

    private fun getBackgroundColor(): Color {
        val highlightSpan =
            spans.lastOrNull { it.type == SpanType.HIGHLIGHT && it.start == 0 && it.end >= raw.length - 1 }
        return highlightSpan?.color ?: INVALID_COLOR
    }

    private inline val SimpleStringBuilder.formatted: SimpleStringBuilder
        get() {
            invalidHtmlChars.forEach { (regex, replacement) ->
                replace(regex, replacement)
            }
            return this
        }

    private class Span private constructor(
        var start: Int,
        var end: Int,
        var type: SpanType,
        var color: Color = Color.BLACK,
    ) : CacheableObject {
        fun css(): String = when (type) {
            SpanType.NORMAL -> EMPTY_STRING
            SpanType.BOLD -> "font-weight:bold;"
            SpanType.ITALIC -> "font-style:italic;"
            SpanType.UNDERLINE -> "text-decoration:underline;"
            SpanType.STRIKETHROUGH -> "text-decoration:line-through;"
            SpanType.HIGHLIGHT -> "background-color:${color.toHtml()};"
            SpanType.FOREGROUND -> "color:${color.toHtml()};"
        }

        fun set(start: Int, end: Int, type: SpanType, color: Color = Color.BLACK) {
            this.start = start
            this.end = end
            this.type = type
            this.color = color
        }

        override fun recycle() {
            pool.recycle(this)
        }

        companion object {
            private val pool = ObjectPool.createMemoryAwarePool(1000) {
                Span(0, 0, SpanType.NORMAL)
            }

            fun obtain(): Span {
                return pool.obtain()
            }
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
        private val invalidHtmlChars = mapOf(
            "<".toRegex() to "&lt;",
            ">".toRegex() to "&gt;",
            " ".toRegex() to "&nbsp;"
        )

        private val pool = ObjectPool.createMemoryAwarePool(1000) { LabelRenderer() }

        fun obtain(): LabelRenderer {
            return pool.obtain()
        }
    }
}