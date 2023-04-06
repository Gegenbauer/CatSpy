package me.gegenbauer.logviewer.ui.button

import javax.swing.Icon
import javax.swing.JButton

class StatefulButton(
    private val originalIcon: Icon? = null,
    private val originalText: String? = null
) : JButton(originalText, originalIcon) {
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

}