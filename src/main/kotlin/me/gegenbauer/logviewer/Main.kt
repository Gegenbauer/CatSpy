package me.gegenbauer.logviewer

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.logviewer.ui.MainUI
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
    }
}

fun changeTheme(theme: Theme) {
    LafManager.install(theme)
}