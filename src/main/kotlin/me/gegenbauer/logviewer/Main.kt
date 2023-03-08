package me.gegenbauer.logviewer

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.settings.ThemeSettings
import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.logviewer.theme.ThemeManager
import me.gegenbauer.logviewer.theme.ThemeManager.loadThemeSettings
import me.gegenbauer.logviewer.ui.MainUI
import java.awt.Container
import java.util.*
import javax.swing.SwingUtilities


const val VERSION: String = "0.3.0"
const val NAME: String = "LogViewer"

fun main(args: Array<String>) {
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")


    SwingUtilities.invokeLater {
        val mainUI = MainUI(NAME)
        mainUI.isVisible = true
        mainUI.updateUIAfterVisible(args)

        addClickListenerForAllComponents(mainUI.components)

        LafManager.install()

        ThemeSettings.getInstance().setConfiguration(ThemeManager.settingsConfiguration)
        ThemeSettings.showSettingsDialog(mainUI)
        LafManager.registerDefaultsAdjustmentTask { t: Theme, _: Properties ->
            //ThemeManager.updateTheme(t)
        }
        mainUI.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                ThemeManager.saveThemeSettings()
            }
        })
    }
}

// TODO removed
private fun addClickListenerForAllComponents(components: Array<java.awt.Component>) {
    components.forEach { component ->
        component.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                println(component.javaClass.name)
            }
        })
        if (component is Container) {
            addClickListenerForAllComponents(component.components)
        }
    }
}

fun changeTheme(theme: Theme) {
    LafManager.install(theme)
}