package me.gegenbauer.catspy.ui.script

import com.github.weisj.darklaf.ui.text.DarkTextBorder
import javax.swing.JTextArea
import javax.swing.plaf.TextUI

class BorderedTextArea: JTextArea() {
    private val customBorder = DarkTextBorder()

    init {
        lineWrap = true
    }

    override fun setUI(ui: TextUI?) {
        super.setUI(ui)
        border = customBorder
    }
}