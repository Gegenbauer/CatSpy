package me.gegenbauer.catspy.log.ui.customize

import javax.swing.JLabel

open class MultilineLabel(text: String = "") : JLabel(text) {

    override fun setText(text: String?) {
        super.setText(processLineBreakText(text ?: ""))
    }

    private fun processLineBreakText(text: String): String {
        return "<html>${text.replace("\n", "<br>")}</html>"
    }
}