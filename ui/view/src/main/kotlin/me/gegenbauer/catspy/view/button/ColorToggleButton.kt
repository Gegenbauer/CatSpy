package me.gegenbauer.catspy.view.button

import com.formdev.flatlaf.icons.FlatRadioButtonIcon
import me.gegenbauer.catspy.utils.ui.setBorderless
import javax.swing.Icon
import javax.swing.JToggleButton

class ColorToggleButton(
    title: String,
    unselectedIcon: Icon = FlatRadioButtonIcon(),
    selectedIcon: Icon = FlatRadioButtonIcon(),
    tooltip: String? = null
) : JToggleButton(title) {

    constructor(unselectedIcon: Icon, selectedIcon: Icon) : this("", unselectedIcon, selectedIcon)

    init {
        icon = unselectedIcon
        setSelectedIcon(selectedIcon)
        isContentAreaFilled = false
        toolTipText = tooltip
        isRolloverEnabled = true
        setBorderless()
    }

}

