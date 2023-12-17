package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.iconset.GIcons
import javax.swing.Icon
import javax.swing.JToggleButton

class StatefulToggleButton(
    private val originalIcon: Icon? = null,
    private val originalSelectedIcon: Icon? = null,
    private val originalText: String? = null,
    private val overrideDefaultToggleSelectedIcon: Icon? = null,
    private val overrideDefaultToggleIcon: Icon? = null,
    tooltip: String? = null
) : JToggleButton(originalText, originalIcon), StatefulActionComponent {

    override var buttonDisplayMode: ButtonDisplayMode? =
        ButtonDisplayMode.ALL
        set(value) {
            field = value
            setDisplayMode(value)
        }

    init {
        componentName = originalText ?: ""
        toolTipText = tooltip
        isRolloverEnabled = true
    }

    override fun setDisplayMode(mode: ButtonDisplayMode?) {
        when (mode) {
            ButtonDisplayMode.TEXT -> {
                text = originalText
                icon = overrideDefaultToggleIcon ?: GIcons.State.ToggleOff.get()
                selectedIcon = overrideDefaultToggleSelectedIcon ?: GIcons.State.ToggleOn.get()
            }

            ButtonDisplayMode.ICON -> {
                text = null
                icon = originalIcon
                selectedIcon = originalSelectedIcon
            }

            else -> {
                text = originalText
                icon = originalIcon
                selectedIcon = originalSelectedIcon
            }
        }
    }
}