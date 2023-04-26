package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.databinding.bind.componentName
import me.gegenbauer.logviewer.utils.loadIcon
import java.awt.Insets
import javax.swing.JToggleButton


class ColorToggleButton(
    title: String,
    tooltip: String? = null
) : JToggleButton(title) {

    init {
        icon = loadIcon("toggle_off.png")
        selectedIcon = loadIcon("toggle_on.png")
        margin = Insets(0, 0, 0, 0)
        componentName = title
        toolTipText = tooltip
        isRolloverEnabled = true
    }
}
