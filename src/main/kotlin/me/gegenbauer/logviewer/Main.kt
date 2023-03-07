package me.gegenbauer.logviewer

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.logviewer.ui.MainUI
import java.awt.Container
import javax.swing.SwingUtilities


const val VERSION: String = "0.3.0"
const val NAME: String = "LogViewer"

fun main(args: Array<String>) {
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")


    SwingUtilities.invokeLater {
        LafManager.install()

        val mainUI = MainUI(NAME)

        mainUI.isVisible = true
        mainUI.updateUIAfterVisible(args)
        addClickListenerForAllComponents(mainUI.components)
    }
}

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