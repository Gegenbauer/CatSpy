package me.gegenbauer.catspy.common.ui.container

import me.gegenbauer.catspy.common.configuration.GThemeChangeListener
import me.gegenbauer.catspy.common.configuration.ThemeManager
import me.gegenbauer.catspy.context.Disposable
import javax.swing.JPanel

class WrapablePanel : JPanel(), Disposable {

    private val onThemeChangeListener = GThemeChangeListener {
        (layout as WrapableLayout).resizeComponent(this)
    }

    init {
        layout = WrapableLayout(HORIZONTAL_GAP, VERTICAL_GAP)
        ThemeManager.registerThemeUpdateListener(onThemeChangeListener)
    }

    override fun onDestroy() {
        ThemeManager.unregisterThemeUpdateListener(onThemeChangeListener)
    }


    companion object {
        private const val HORIZONTAL_GAP = 3
        private const val VERTICAL_GAP = 3
    }
}