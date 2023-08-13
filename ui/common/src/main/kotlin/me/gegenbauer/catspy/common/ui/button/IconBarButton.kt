package me.gegenbauer.catspy.common.ui.button

import me.gegenbauer.catspy.common.support.setBorderless
import java.awt.Dimension
import javax.swing.Icon

/**
 * 尺寸紧凑，而且宽高相同
 */
class IconBarButton(icon: Icon? = null, tooltip: String? = null) : GButton(icon, tooltip) {

    init {
        isRolloverEnabled = true
        setBorderless()
        preferredSize = Dimension(24, 24)
    }
}