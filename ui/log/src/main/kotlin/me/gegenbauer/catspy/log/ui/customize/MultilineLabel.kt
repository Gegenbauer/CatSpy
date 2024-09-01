package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import javax.swing.JLabel

open class MultilineLabel(text: String = EMPTY_STRING) : JLabel(text) {

    override fun setText(text: String?) {
        super.setText(processLineBreakText(text ?: EMPTY_STRING))
    }

    private fun processLineBreakText(text: String): String {
        return "<html>${text.replace("\n", "<br>")}</html>"
    }
}