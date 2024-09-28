package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.utils.ui.EMPTY_INSETS
import me.gegenbauer.catspy.utils.ui.setBorderless
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JToggleButton

class IconBarToggleButton(
    icon: Icon,
    selectedIcon: Icon? = null,
    disabledIcon: Icon? = null,
    text: String? = null,
    selected: Boolean = false,
) : JToggleButton(text, icon, selected) {

    init {
        isRolloverEnabled = false
        isContentAreaFilled = false
        this.selectedIcon = selectedIcon
        this.disabledIcon = disabledIcon
        setBorderless()
        preferredSize = Dimension(30, 30)
        margin = EMPTY_INSETS
        border = null
    }

    override fun updateUI() {
        super.updateUI()
        border = null
    }
}