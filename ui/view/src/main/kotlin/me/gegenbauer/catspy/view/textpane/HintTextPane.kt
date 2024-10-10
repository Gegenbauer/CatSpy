package me.gegenbauer.catspy.view.textpane

import java.awt.Dimension
import javax.swing.JTextPane
import javax.swing.plaf.TextUI

class HintTextPane: JTextPane() {

    init {
        background = null
    }

    override fun setText(t: String?) {
        super.setText(t)
        if (t.isNullOrEmpty()) {
            preferredSize = Dimension(0, 0)
        } else {
            preferredSize = null
        }
    }

    override fun setUI(ui: TextUI?) {
        super.setUI(ui)
        background = null
    }
}