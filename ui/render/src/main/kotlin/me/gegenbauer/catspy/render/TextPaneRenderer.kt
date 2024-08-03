package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.cache.CacheableObject
import me.gegenbauer.catspy.cache.ObjectPool
import me.gegenbauer.catspy.context.MemoryState
import java.awt.Color
import javax.swing.JTextPane
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class TextPaneRenderer(
    private var textPane: JTextPane? = null,
) : StringRenderer, CacheableObject {

    override var raw: String = ""
        private set

    private val spans = mutableListOf<Span>()

    fun setTextPane(textPane: JTextPane): TextPaneRenderer {
        clear()
        this.textPane = textPane
        return this
    }

    override fun updateRaw(raw: String): TextPaneRenderer {
        this.raw = raw
        return this
    }

    private fun requireTextPane(): JTextPane {
        val target = textPane ?: throw IllegalStateException("TextPane not set")
        return target
    }

    override fun bold(start: Int, end: Int): StringRenderer {
        spans.add(Span(start, end, SpanType.BOLD))
        return this
    }

    override fun italic(start: Int, end: Int): StringRenderer {
        spans.add(Span(start, end, SpanType.ITALIC))
        return this
    }

    override fun strikethrough(start: Int, end: Int): StringRenderer {
        spans.add(Span(start, end, SpanType.STRIKETHROUGH))
        return this
    }

    override fun highlight(start: Int, end: Int, color: Color): StringRenderer {
        spans.add(Span(start, end, SpanType.HIGHLIGHT, color))
        return this
    }

    override fun foreground(start: Int, end: Int, color: Color): StringRenderer {
        spans.add(Span(start, end, SpanType.FOREGROUND, color))
        return this
    }

    override fun underline(start: Int, end: Int): StringRenderer {
        spans.add(Span(start, end, SpanType.UNDERLINE))
        return this
    }

    override fun render() {
        val textPane = requireTextPane()
        textPane.text = raw
        if (raw.isEmpty() || spans.isEmpty()) {
            return
        }
        applyStyles(textPane, spans)
    }

    private data class Span(
        val start: Int,
        val end: Int,
        val type: SpanType,
        val color: Color = Color.BLACK
    )

    private fun applyStyles(textPane: JTextPane, spans: List<Span>) {
        spans.forEachIndexed { index, span ->
            val style = SimpleAttributeSet()
            when (span.type) {
                SpanType.BOLD -> StyleConstants.setBold(style, true)
                SpanType.ITALIC -> StyleConstants.setItalic(style, true)
                SpanType.UNDERLINE -> StyleConstants.setUnderline(style, true)
                SpanType.STRIKETHROUGH -> StyleConstants.setStrikeThrough(style, true)
                SpanType.HIGHLIGHT -> StyleConstants.setBackground(style, span.color)
                SpanType.FOREGROUND -> StyleConstants.setForeground(style, span.color)
            }
            textPane.styledDocument.setCharacterAttributes(span.start, span.end - span.start + 1, style, index == 0)
        }
    }

    private enum class SpanType {
        BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, HIGHLIGHT, FOREGROUND
    }

    override fun clear() {
        spans.clear()
        textPane = null
    }

    override fun recycle() {
        clear()
        pool.recycle(this)
    }

    companion object {
        private val pool = object : ObjectPool<TextPaneRenderer>(1000) {

            init {
                MemoryState.register(this)
            }

            override fun create(): TextPaneRenderer {
                return TextPaneRenderer()
            }
        }

        fun obtain(): TextPaneRenderer {
            return pool.obtain()
        }
    }
}