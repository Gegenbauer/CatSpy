package me.gegenbauer.catspy.view.combobox.highlight

import javax.swing.text.Highlighter.HighlightPainter

interface Highlightable {
    val painterInclude: HighlightPainter

    val painterExclude: HighlightPainter

    val painterSeparator: HighlightPainter

    fun setEnableHighlighter(enable: Boolean)

    fun updateHighlighter()
}