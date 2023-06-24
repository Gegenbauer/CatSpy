package me.gegenbauer.catspy.configuration

import com.github.weisj.darklaf.theme.event.ThemeChangeEvent
import com.github.weisj.darklaf.theme.event.ThemeChangeListener

fun interface GThemeChangeListener : ThemeChangeListener {
    override fun themeChanged(e: ThemeChangeEvent) {
        onThemeChange(e)
    }

    override fun themeInstalled(e: ThemeChangeEvent) {
        onThemeChange(e)
    }

    fun onThemeChange(e: ThemeChangeEvent)
}