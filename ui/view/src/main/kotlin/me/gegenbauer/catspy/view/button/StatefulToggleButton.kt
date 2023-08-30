package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.configuration.ToggleButtonTheme
import me.gegenbauer.catspy.databinding.bind.componentName
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

    override var buttonDisplayMode: me.gegenbauer.catspy.view.button.ButtonDisplayMode? =
        me.gegenbauer.catspy.view.button.ButtonDisplayMode.ALL
        set(value) {
            field = value
            setDisplayMode(value)
        }

    init {
        componentName = originalText ?: ""
        toolTipText = tooltip
        isRolloverEnabled = true
    }

    override fun setDisplayMode(mode: me.gegenbauer.catspy.view.button.ButtonDisplayMode?) {
        when (mode) {
            me.gegenbauer.catspy.view.button.ButtonDisplayMode.TEXT -> {
                text = originalText
                icon = overrideDefaultToggleIcon ?: ToggleButtonTheme.defaultIconUnselected
                selectedIcon = overrideDefaultToggleSelectedIcon ?: ToggleButtonTheme.defaultIconSelected
            }

            me.gegenbauer.catspy.view.button.ButtonDisplayMode.ICON -> {
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