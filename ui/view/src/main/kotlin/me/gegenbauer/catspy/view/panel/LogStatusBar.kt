package me.gegenbauer.catspy.view.panel

import com.formdev.flatlaf.FlatLaf
import me.gegenbauer.catspy.configuration.GThemeChangeListener
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.utils.ui.isLeftClick
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

class LogStatusBar : JPanel(), GThemeChangeListener {
    private val statusMethod = JLabel()
    private val logFilePath = JTextField(STRINGS.ui.none) applyTooltip STRINGS.toolTip.logFilePath
    private var logStatus: StatusBar.LogStatus = StatusBar.LogStatus.NONE
    private var logFile: File? = null

    init {
        border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        statusMethod.isOpaque = true
        logFilePath.isEditable = false
        logFilePath.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
        add(statusMethod)
        add(logFilePath)

        logFilePath.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.isLeftClick.not()) {
                    logFile?.takeIf { it.exists() }?.let { showOpenFilePopup(e, it.absolutePath) }
                }
            }
        })
    }

    private fun showOpenFilePopup(event: MouseEvent, path: String) {
        val popup = JPopupMenu()
        val openItem = JMenuItem(STRINGS.ui.showFileInFileManager)
        openItem.addActionListener {
            currentPlatform.showFileInExplorer(File(path))
        }
        popup.add(openItem)
        popup.show(event.component, event.x, event.y)
    }

    fun setLogStatus(status: StatusBar.LogStatus) {
        logStatus = status
        logFile = File(status.path)
        statusMethod.text = status.status
        statusMethod.foreground = status.foregroundColorProvider()
        statusMethod.background = status.backgroundColorProvider()
        logFilePath.text = status.path.takeIf { it.isNotEmpty() } ?: STRINGS.ui.none
    }

    override fun onThemeChange(theme: FlatLaf) {
        statusMethod.foreground = logStatus.foregroundColorProvider()
        statusMethod.background = logStatus.backgroundColorProvider()
    }
}