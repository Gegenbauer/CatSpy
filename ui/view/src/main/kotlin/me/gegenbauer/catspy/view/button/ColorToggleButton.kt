package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.utils.ui.setBorderless
import javax.swing.Icon
import javax.swing.JToggleButton

class ColorToggleButton(
    title: String,
    unselectedIcon: Icon = GIcons.State.ToggleOff.get(),
    selectedIcon: Icon = GIcons.State.ToggleOn.get(),
    tooltip: String? = null
) : JToggleButton(title) {

    constructor(unselectedIcon: Icon, selectedIcon: Icon) : this(EMPTY_STRING, unselectedIcon, selectedIcon)

    init {
        icon = unselectedIcon
        setSelectedIcon(selectedIcon)
        isContentAreaFilled = false
        toolTipText = tooltip
        isRolloverEnabled = true
        setBorderless()
    }

}

