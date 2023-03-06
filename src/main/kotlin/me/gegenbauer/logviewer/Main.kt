package me.gegenbauer.logviewer

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.DarculaTheme
import me.gegenbauer.logviewer.ui.MainUI
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities


const val VERSION: String = "0.3.0"
const val NAME: String = "LogViewer"

fun main(args: Array<String>) {
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")


    SwingUtilities.invokeLater {
        LafManager.setTheme(DarculaTheme())
        LafManager.install()

        val mainUI = MainUI(NAME)

        mainUI.isVisible = true
        mainUI.updateUIAfterVisible(args)
    }
}