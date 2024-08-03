package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.cache.ObjectPool
import me.gegenbauer.catspy.context.MemoryState
import java.awt.Color
import javax.swing.JLabel
import kotlin.math.roundToInt

/**
 * Renders a string with HTML tags.
 */
class LabelRenderer(private var label: JLabel? = null) : StringRenderer {

    override var raw: String = ""
        private set

    private val spans = mutableListOf<Span>()

    fun setLabel(label: JLabel): LabelRenderer {
        clear()
        this.label = label
        return this
    }

    private fun requireLabel(): JLabel {
        val target = label ?: throw IllegalStateException("Label not set")
        return target
    }

    override fun updateRaw(raw: String): StringRenderer {
        this.raw = raw
        return this
    }

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

    override fun render() {
        val label = requireLabel()
        if (raw.isEmpty() || spans.isEmpty()) {
            label.text = raw
            return
        }

        if (isComplexityLow()) {
            if (getForegroundColor() != INVALID_COLOR) {
                label.foreground = getForegroundColor()
            }
            if (getBackgroundColor() != INVALID_COLOR) {
                label.background = getBackgroundColor()
            }
            label.text = raw
            return
        }

        val builder = HtmlStringBuilder()
        builder.append(renderWithoutTags())
        val renderedText = builder.build()
        if (label.text != renderedText) {
            label.text = renderedText
        }
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
                averageColor(it.map { span -> span.color })
            } else {
                it.first().color
            }
            mergedSpan.add(Span(it.first().start, it.first().end, it.first().type, color))
        }
        return mergedSpan
    }

    override fun clear() {
        spans.clear()
        raw = ""
        label = null
    }

    private fun isComplexityLow(): Boolean {
        val foregroundSpans = spans.filter { it.type == SpanType.FOREGROUND }
        val highlightSpans = spans.filter { it.type == SpanType.HIGHLIGHT }

        val isForegroundLowComplexity =
            foregroundSpans.isEmpty() || (foregroundSpans.all { it.start == 0 && it.end == raw.length - 1 })
        val isHighlightLowComplexity =
            highlightSpans.isEmpty() || (highlightSpans.all { it.start == 0 && it.end == raw.length - 1 })

        return isForegroundLowComplexity && isHighlightLowComplexity
    }

    private fun getForegroundColor(): Color {
        val foregroundSpans = spans.filter { it.type == SpanType.FOREGROUND }
        return if (foregroundSpans.isEmpty()) {
            INVALID_COLOR
        } else {
            if (foregroundSpans.size == 1) {
                return foregroundSpans.first().color
            }
            averageColor(foregroundSpans.map { it.color })
        }
    }

    private fun getBackgroundColor(): Color {
        val highlightSpans = spans.filter { it.type == SpanType.HIGHLIGHT }
        return if (highlightSpans.isEmpty()) {
            INVALID_COLOR
        } else {
            if (highlightSpans.size == 1) {
                return highlightSpans.first().color
            }
            averageColor(highlightSpans.map { it.color })
        }
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

    override fun recycle() {
        clear()
        pool.recycle(this)
    }

    companion object {
        val INVALID_COLOR = Color(66, 66, 66, 66)

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