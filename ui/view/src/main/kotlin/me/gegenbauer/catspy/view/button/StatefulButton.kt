package me.gegenbauer.catspy.view.button

import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_BORDERLESS
import me.gegenbauer.catspy.utils.ui.BORDER_TYPE_NONE
import me.gegenbauer.catspy.utils.ui.setHeight
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
            FlatClientProperties.BUTTON_TYPE,
            if (mode == ButtonDisplayMode.ICON) BUTTON_TYPE_BORDERLESS else BORDER_TYPE_NONE
        )
    }

    init {
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