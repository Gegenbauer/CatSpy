package me.gegenbauer.catspy.common.ui.container

import me.gegenbauer.catspy.common.configuration.ThemeManager
import javax.swing.JPanel

class WrapablePanel : JPanel() {
    init {
        layout = WrapableLayout(HORIZONTAL_GAP, VERTICAL_GAP)
        ThemeManager.registerThemeUpdateListener { _ ->
            (layout as WrapableLayout).resizeComponent(this)
        }
    }


    companion object {
        private const val HORIZONTAL_GAP = 3
        private const val VERTICAL_GAP = 3
    }
}