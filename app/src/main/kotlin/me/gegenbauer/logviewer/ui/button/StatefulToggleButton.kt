package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.databinding.bind.componentName
import me.gegenbauer.logviewer.ui.ToggleButton
import javax.swing.Icon
import javax.swing.JToggleButton

class StatefulToggleButton(
    private val originalIcon: Icon? = null,
    private val originalSelectedIcon: Icon? = null,
    private val originalText: String? = null,
    private val overrideDefaultToggleSelectedIcon: Icon? = null,
    private val overrideDefaultToggleIcon: Icon? = null,
    tooltip: String? = null
) : JToggleButton(originalText, originalIcon) {

    // TODO observe night mode change
    var buttonDisplayMode = ButtonDisplayMode.ALL
        set(value) {
            field = value
            when (value) {
                ButtonDisplayMode.TEXT -> {
                    text = originalText
                    icon = overrideDefaultToggleIcon ?: ToggleButton.defaultIconUnselected
                    selectedIcon = overrideDefaultToggleSelectedIcon ?: ToggleButton.defaultIconSelected
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
        toolTipText = tooltip
    }
}