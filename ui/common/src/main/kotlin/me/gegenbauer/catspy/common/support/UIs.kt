package me.gegenbauer.catspy.common.support

import java.awt.BorderLayout
import javax.swing.*

const val PROPERTY_KEY_BUTTON_VARIANT = "JButton.variant"
const val BORDER_TYPE_BORDERLESS = "borderless"
const val BORDER_TYPE_NONE = "none"

fun addHSeparator(target: JPanel, title: String) {
    val titleHtml = title.replace(" ", "&nbsp;")
    val separator = JSeparator(SwingConstants.HORIZONTAL)
    val label = JLabel("<html><b>$titleHtml</b></html>")
    val panel = JPanel(BorderLayout())
    val separatePanel = JPanel(BorderLayout())
    separatePanel.add(Box.createVerticalStrut(label.font.size / 2), BorderLayout.NORTH)
    separatePanel.add(separator, BorderLayout.CENTER)
    panel.add(label, BorderLayout.WEST)
    panel.add(separatePanel, BorderLayout.CENTER)
    target.add(panel)
}

fun JComponent.setBorderless() {
    putClientProperty(PROPERTY_KEY_BUTTON_VARIANT, BORDER_TYPE_BORDERLESS)
}