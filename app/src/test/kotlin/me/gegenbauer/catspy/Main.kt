package me.gegenbauer.catspy

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.ui.button.DarkButtonBorder
import com.github.weisj.darklaf.ui.button.DarkButtonUI
import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.*
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.ui.combobox.filterComboBox
import me.gegenbauer.catspy.utils.filesDir
import me.gegenbauer.catspy.viewmodel.GlobalViewModel
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.plaf.UIResource

fun main() {
    AppScope.launch(Dispatchers.UI) {
        withContext(Dispatchers.APP_LAUNCH) {
            GLog.init(filesDir, "glog.txt")
            GLog.debug = UIConfManager.uiConf.debug
            ThemeManager.init()
            GlobalViewModel.init()
        }
        ThemeManager.installTheme()
        LafManager.registerInitTask { theme: Theme, defaults: UIDefaults ->
            defaults[DarkButtonUI.KEY_VARIANT] = DarkButtonUI.VARIANT_BORDERLESS
        }
        val frame = JFrame()
        frame.size = Dimension(500, 500)
        val panel = JPanel()
        val button1 = GButton("Pause")
        val button2 = GButton("Resume")
        panel.add(button1)
        panel.add(button2)
        frame.add(panel)
        val cancellablePause = CancellablePause()
        AppScope.async(Dispatchers.CPU) {
            repeat(Int.MAX_VALUE) {
                cancellablePause.addPausePoint(1000)
                GLog.d("CancellablePauseTest", "print")
            }
        }
        button1.addActionListener {
            cancellablePause.pause()
        }
        button2.addActionListener {
            cancellablePause.resume()
        }
        frame.isVisible = true
    }
}