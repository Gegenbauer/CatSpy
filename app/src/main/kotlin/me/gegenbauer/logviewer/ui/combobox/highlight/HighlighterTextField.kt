package me.gegenbauer.logviewer.ui.combobox.highlight

import javax.swing.JTextField

class HighlighterTextField : JTextField(), Highlightable<HighlighterTextField> {
    private lateinit var textComponentWrapper: BaseHighlighterEditor<HighlighterTextField>.HighlightTextComponentWrapper

    override fun setEnableHighlighter(enable: Boolean) {
        textComponentWrapper.enableHighlighter(enable)
    }

    override fun setTextComponentWrapper(textComponentWrapper: BaseHighlighterEditor<HighlighterTextField>.HighlightTextComponentWrapper) {
        this.textComponentWrapper = textComponentWrapper
    }

    override fun setText(t: String?) {
        if (text == t) return
        super.setText(t)
    }
}