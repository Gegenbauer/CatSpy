package me.gegenbauer.logviewer.ui.combobox.highlight

interface Highlightable {
    fun setEnableHighlighter(enable: Boolean)

    fun updateHighlighter()
}