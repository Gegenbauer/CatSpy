package me.gegenbauer.catspy

class HtmlStringRender2(private val raw: String) {
    private val spans = mutableListOf<Span>()

    fun bold(start: Int, end: Int) {
        spans.add(Span(start, end, "<b>", "</b>"))
    }

    fun highlight(start: Int, end: Int) {
        spans.add(Span(start, end, "<mark>", "</mark>"))
    }

    fun italic(start: Int, end: Int) {
        spans.add(Span(start, end, "<i>", "</i>"))
    }

    fun render(): String {
        val result = StringBuilder()
        var lastIndex = 0
        val sortedSpans = spans.sortedBy { it.start }
        for (span in sortedSpans) {
            if (span.end > raw.length || span.start < lastIndex) {
                throw IndexOutOfBoundsException("The span is out of range")
            }
            result.append(raw.substring(lastIndex, span.start))
            result.append(span.startTag)
            result.append(raw.substring(span.start, span.end))
            result.append(span.endTag)
            lastIndex = span.end
        }
        result.append(raw.substring(lastIndex))
        return result.toString()
    }

    data class Span(val start: Int, val end: Int, val startTag: String, val endTag: String)
}