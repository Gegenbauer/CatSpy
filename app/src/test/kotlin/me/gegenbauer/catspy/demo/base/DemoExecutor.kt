package me.gegenbauer.catspy.demo.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.strings.Configuration
import java.awt.Dimension
import javax.swing.JFrame

object DemoExecutor {

    fun show(demo: ComponentDemo) {
        AppScope.launch(Dispatchers.UI) {
            withContext(Dispatchers.APP_LAUNCH) {
                GLog.init(filesDir, Configuration.LOG_NAME)
                GLog.debug = true
                ThemeManager.init()
            }
            ThemeManager.installTheme()
            ThemeManager.applyTempTheme()

            val frame = JFrame()
            frame.size = Dimension(1000, 1000)
            frame.title = demo.demoName
            frame.add(demo.component)
            frame.isVisible = true
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        }
    }
}