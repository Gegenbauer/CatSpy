package me.gegenbauer.catspy.common.configuration

import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.theme.event.ThemeChangeEvent
import com.github.weisj.darklaf.theme.event.ThemeChangeListener

fun interface GThemeChangeListener : ThemeChangeListener {
    override fun themeChanged(e: ThemeChangeEvent) {
        onThemeChange(e.newTheme)
    }

    override fun themeInstalled(e: ThemeChangeEvent) {
        onThemeChange(e.newTheme)
    }

    fun onThemeChange(theme: Theme)
}