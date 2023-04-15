package me.gegenbauer.logviewer.ui.combobox.highlight

import java.awt.event.ActionListener
import javax.swing.JTextArea

class HighlighterTextArea : JTextArea(), Highlightable<HighlighterTextArea> {
    private val actionListeners = ArrayList<ActionListener>()
    private lateinit var textComponentWrapper: BaseHighlighterEditor<HighlighterTextArea>.HighlightTextComponentWrapper

    init {
        lineWrap = true
    }

   override fun setTextComponentWrapper(textComponentWrapper: BaseHighlighterEditor<HighlighterTextArea>.HighlightTextComponentWrapper) {
        this.textComponentWrapper = textComponentWrapper
    }

    override fun setEnableHighlighter(enable: Boolean) {
        textComponentWrapper.enableHighlighter(enable)
    }

    override fun setText(t: String?) {
        if (text == t) return
        super.setText(t)
    }

    fun addActionListener(l: ActionListener) {
        actionListeners.add(l)
    }

    fun removeActionListener(l: ActionListener) {
        actionListeners.remove(l)
    }
}