package me.gegenbauer.logviewer.ui.combobox.highlight

import java.awt.Graphics
import java.awt.Insets
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JTextField

class HighlighterTextField : JTextField(), Highlightable<HighlighterTextField> {
    private lateinit var textComponentWrapper: BaseHighlighterEditor<HighlighterTextField>.HighlightTextComponentWrapper

    init {
        margin = Insets(0, 0, 0, 0)

        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
                // do nothing
            }

            override fun keyPressed(e: KeyEvent) {
                textComponentWrapper.setUpdateHighlighter(true)
            }

            override fun keyReleased(e: KeyEvent) {
                // do nothing
            }
        })
    }

    override fun setEnableHighlighter(enable: Boolean) {
        textComponentWrapper.setEnableHighlighter(enable)
    }

    override fun setUpdateHighlighter(updateHighlighter: Boolean) {
        textComponentWrapper.setUpdateHighlighter(updateHighlighter)
    }

    override fun paint(graphics: Graphics) {
        textComponentWrapper.paint()
        super.paint(graphics)
    }

    override fun setTextComponentWrapper(textComponentWrapper: BaseHighlighterEditor<HighlighterTextField>.HighlightTextComponentWrapper) {
        this.textComponentWrapper = textComponentWrapper
    }
}