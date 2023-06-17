package me.gegenbauer.catspy.ui.button

import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.utils.loadIcon
import me.gegenbauer.catspy.utils.setHeight
import javax.swing.JToggleButton


class ColorToggleButton(
    title: String,
    tooltip: String? = null
) : JToggleButton(title) {

    init {
        icon = loadIcon("toggle_off.png")
        selectedIcon = loadIcon("toggle_on.png")
        componentName = title
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

