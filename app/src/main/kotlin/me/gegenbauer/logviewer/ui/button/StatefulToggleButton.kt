package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.databinding.componentName
import me.gegenbauer.logviewer.ui.defaultToggleIconSelected
import me.gegenbauer.logviewer.ui.defaultToggleIconUnselected
import javax.swing.Icon
import javax.swing.JToggleButton

class StatefulToggleButton(
    private val originalIcon: Icon? = null,
    private val originalSelectedIcon: Icon? = null,
    private val originalText: String? = null,
    private val overrideDefaultToggleSelectedIcon: Icon? = null,
    private val overrideDefaultToggleIcon: Icon? = null,
) : JToggleButton(originalText, originalIcon) {

    // TODO observe night mode change
    var buttonDisplayMode = ButtonDisplayMode.ALL
        set(value) {
            field = value
            when (value) {
                ButtonDisplayMode.TEXT -> {
                    text = originalText
                    icon = overrideDefaultToggleIcon ?: defaultToggleIconUnselected
                    selectedIcon = overrideDefaultToggleSelectedIcon ?: defaultToggleIconSelected
                }

                ButtonDisplayMode.ICON -> {
                    text = null
                    icon = originalIcon
                    selectedIcon = originalSelectedIcon
                }

                ButtonDisplayMode.ALL -> {
                    text = originalText
                    icon = originalIcon
                    selectedIcon = originalSelectedIcon
                }
            }
        }

    init {
        componentName = originalText ?: ""
    }
}