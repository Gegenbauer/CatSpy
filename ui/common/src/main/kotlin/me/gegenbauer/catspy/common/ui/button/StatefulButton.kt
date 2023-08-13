package me.gegenbauer.catspy.common.ui.button

import me.gegenbauer.catspy.common.support.BORDER_TYPE_BORDERLESS
import me.gegenbauer.catspy.common.support.BORDER_TYPE_NONE
import me.gegenbauer.catspy.common.support.PROPERTY_KEY_BUTTON_VARIANT
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.utils.setHeight
import javax.swing.Icon

class StatefulButton(
    private val originalIcon: Icon? = null,
    private val originalText: String? = null,
    tooltip: String? = null
) : GButton(originalText, originalIcon),
    StatefulActionComponent {
    override var buttonDisplayMode: ButtonDisplayMode? =
        ButtonDisplayMode.ALL
        set(value) {
            field = value
            setDisplayMode(value)
        }

    override fun setDisplayMode(mode: ButtonDisplayMode?) {
        when (mode) {
            ButtonDisplayMode.TEXT -> {
                text = originalText
                icon = null
            }

            ButtonDisplayMode.ICON -> {
                text = null
                icon = originalIcon
            }

            else -> {
                text = originalText
                icon = originalIcon
            }
        }

        putClientProperty(
            PROPERTY_KEY_BUTTON_VARIANT,
            if (mode == ButtonDisplayMode.ICON) BORDER_TYPE_BORDERLESS else BORDER_TYPE_NONE
        )
    }

    init {
        componentName = originalText ?: ""
        toolTipText = tooltip

        configureHeight()
    }

    override fun updateUI() {
        super.updateUI()
        configureHeight()
    }

    private fun configureHeight() {
        val fontMetrics = getFontMetrics(font)
        setHeight(fontMetrics.height + 10)
    }
}