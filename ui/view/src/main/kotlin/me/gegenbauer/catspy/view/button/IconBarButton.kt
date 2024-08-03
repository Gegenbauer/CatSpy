package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.utils.ui.setBorderless
import java.awt.Dimension
import javax.swing.Icon

/**
 * Compact in size and equal in width and height
 */
class IconBarButton(
    icon: Icon? = null,
    tooltip: String? = null,
    disabledIcon: Icon? = null
) : GButton(icon, tooltip) {

    init {
        isRolloverEnabled = true
        this.disabledIcon = disabledIcon
        setBorderless()
        preferredSize = Dimension(24, 24)
    }
}