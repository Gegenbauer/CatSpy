package me.gegenbauer.catspy.common.ui.button

import me.gegenbauer.catspy.common.support.setBorderless
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.iconset.GIcons
import javax.swing.JToggleButton


class ColorToggleButton(
    title: String,
    tooltip: String? = null
) : JToggleButton(title) {

    init {
        icon = GIcons.State.ToggleOff.get()
        selectedIcon = GIcons.State.ToggleOn.get()
        componentName = title
        isContentAreaFilled = false
        toolTipText = tooltip
        isRolloverEnabled = true
        setBorderless()
    }
}

