package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.utils.setBorderless
import javax.swing.JToggleButton


class ColorToggleButton(
    title: String,
    tooltip: String? = null
) : JToggleButton(title) {

    init {
        icon = GIcons.State.ToggleOff.get()
        selectedIcon = GIcons.State.ToggleOn.get()
        isContentAreaFilled = false
        toolTipText = tooltip
        isRolloverEnabled = true
        setBorderless()
    }
}

