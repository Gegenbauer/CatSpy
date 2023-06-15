package me.gegenbauer.catspy

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.ui.button.DarkButtonUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.utils.filesDir
import me.gegenbauer.catspy.viewmodel.GlobalViewModel
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JLabel
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
        LafManager.registerInitTask { _: Theme, defaults: UIDefaults ->
            defaults[DarkButtonUI.KEY_VARIANT] = DarkButtonUI.VARIANT_BORDERLESS
        }
        val frame = JFrame()
        frame.size = Dimension(500, 500)
        val panel = JPanel()
        val label = JLabel("<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Sample text</title>\n" +
                "    <style>\n" +
                "        .red-text {\n" +
                "            color: red;\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "        span {\n" +
                "            background-color: white;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <span class=\"red-text\">123</span><span>456789</span>\n" +
                "</body>\n" +
                "</html>")
        panel.add(label)
        frame.add(panel)
        frame.isVisible = true
    }
}