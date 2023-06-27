package me.gegenbauer.catspy.common.ui.button

import com.github.weisj.darklaf.ui.button.DarkButtonUI
import java.awt.Insets
import javax.swing.Icon
import javax.swing.plaf.ButtonUI

/**
 * 尺寸紧凑，而且宽高相同
 */
class IconBarButton(icon: Icon) : GButton(icon) {

    init {
        margin = Insets(4, 4, 4, 4)
    }

    override fun setUI(ui: ButtonUI?) {
        super.setUI(ui)
        setCustomProperty()
    }

    private fun setCustomProperty() {
        putClientProperty(DarkButtonUI.KEY_VARIANT, DarkButtonUI.VARIANT_BORDERLESS)
    }
}