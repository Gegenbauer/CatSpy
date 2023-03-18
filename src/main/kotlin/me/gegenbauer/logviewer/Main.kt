package me.gegenbauer.logviewer

import com.formdev.flatlaf.FlatLightLaf
import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.theme.ThemeManager
import me.gegenbauer.logviewer.ui.MainUI
import java.awt.Container
import javax.swing.SwingUtilities


const val VERSION: String = "0.3.0"
const val NAME: String = "LogViewer"
private const val TAG = "Main"

fun main(args: Array<String>) {
    loadConfig()

    SwingUtilities.invokeLater {
        val mainUI = MainUI(NAME)
        mainUI.isVisible = true
        mainUI.updateUIAfterVisible(args)

        addClickListenerForAllComponents(mainUI.components)

        // need call after main ui created
        ThemeManager.init()
    }
}

private fun loadConfig() {
    ConfigManager.loadConfig()
    ColorManager.fullTableColor.getConfig()
    ColorManager.fullTableColor.applyColor()
    ColorManager.filterTableColor.getConfig()
    ColorManager.filterTableColor.applyColor()
    ColorManager.getConfigFilterStyle()
    ConfigManager.saveConfig()
}

// TODO removed
private fun addClickListenerForAllComponents(components: Array<java.awt.Component>) {
    components.forEach { component ->
        component.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                GLog.d(TAG, component.javaClass.name)
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