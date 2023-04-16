package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.databinding.bind.componentName
import javax.swing.Icon
import javax.swing.JLabel

class StatefulLabel(
    private val originalIcon: Icon? = null,
    private val originalText: String? = null
): JLabel(originalText) {
    var buttonDisplayMode = ButtonDisplayMode.ALL
        set(value) {
            field = value
            when (value) {
                ButtonDisplayMode.TEXT -> {
                    text = originalText
                    icon = null
                }

                ButtonDisplayMode.ICON -> {
                    text = null
                    icon = originalIcon
                }

                ButtonDisplayMode.ALL -> {
                    text = originalText
                    icon = originalIcon
                }
            }
        }

    init {
        componentName = originalText ?: ""
    }
}