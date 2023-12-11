package me.gegenbauer.catspy.configuration

import com.formdev.flatlaf.FlatLaf
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.theme.event.ThemeChangeEvent
import com.github.weisj.darklaf.theme.event.ThemeChangeListener
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

fun interface GThemeChangeListener: PropertyChangeListener {

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == "lookAndFeel") {
            onThemeChange(evt.newValue as FlatLaf)
        }
    }

    fun onThemeChange(theme: FlatLaf)
}