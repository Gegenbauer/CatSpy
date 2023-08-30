package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.databinding.bind.componentName
import javax.swing.Icon
import javax.swing.JLabel

class StatefulLabel(
    private val originalIcon: Icon? = null,
    private val originalText: String? = null
) : JLabel(originalText), StatefulActionComponent {
    override var buttonDisplayMode: me.gegenbauer.catspy.view.button.ButtonDisplayMode? =
        me.gegenbauer.catspy.view.button.ButtonDisplayMode.ALL
        set(value) {
            field = value
            setDisplayMode(value)
        }

    override fun setDisplayMode(mode: me.gegenbauer.catspy.view.button.ButtonDisplayMode?) {
        when (mode) {
            me.gegenbauer.catspy.view.button.ButtonDisplayMode.TEXT -> {
                text = originalText
                icon = null
            }

            me.gegenbauer.catspy.view.button.ButtonDisplayMode.ICON -> {
                text = null
                icon = originalIcon
            }

            else -> {
                text = originalText
                icon = originalIcon
            }
        }
    }

    init {
        componentName = originalText ?: ""
    }
}