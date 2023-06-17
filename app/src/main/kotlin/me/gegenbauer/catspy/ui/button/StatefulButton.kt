package me.gegenbauer.catspy.ui.button

import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.utils.setHeight
import javax.swing.Icon

class StatefulButton(
    private val originalIcon: Icon? = null,
    private val originalText: String? = null,
    tooltip: String? = null
) : GButton(originalText, originalIcon), StatefulActionComponent {
    override var buttonDisplayMode: ButtonDisplayMode? = ButtonDisplayMode.ALL
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
    }

    init {
        componentName = originalText ?: ""
        toolTipText = tooltip
        isRolloverEnabled = true

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