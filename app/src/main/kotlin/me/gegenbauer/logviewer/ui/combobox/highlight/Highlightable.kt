package me.gegenbauer.logviewer.ui.combobox.highlight

import javax.swing.text.JTextComponent

interface Highlightable<T: JTextComponent> {
    fun setTextComponentWrapper(textComponentWrapper: BaseHighlighterEditor<T>.HighlightTextComponentWrapper)

    fun setEnableHighlighter(enable: Boolean)

    fun setUpdateHighlighter(updateHighlighter: Boolean)
}