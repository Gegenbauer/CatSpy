package me.gegenbauer.catspy

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.ui.button.DarkButtonUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.command.LogCmdManager
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.task.LogcatTask
import me.gegenbauer.catspy.task.TaskManager
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.utils.filesDir
import me.gegenbauer.catspy.viewmodel.GlobalViewModel
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.UIDefaults

fun main() {
    AppScope.launch(Dispatchers.UI) {
        withContext(Dispatchers.APP_LAUNCH) {
            GLog.init(filesDir, "glog.txt")
            GLog.debug = true
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
        val logcatTask = LogcatTask("53373619")
        TaskManager().exec(logcatTask)
        button1.addActionListener {
            logcatTask.pause()
        }
        button2.addActionListener {
            logcatTask.resume()
        }
        JOptionPane.showMessageDialog(frame, "e.message", "Error", JOptionPane.ERROR_MESSAGE)
        frame.isVisible = true
    }
}