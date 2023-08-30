package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.utils.setBorderless
import java.awt.Dimension
import javax.swing.Icon

/**
 * 尺寸紧凑，而且宽高相同
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