package me.gegenbauer.catspy.view.panel

import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.applyTooltip
import me.gegenbauer.catspy.utils.isLeftClick
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

class LogStatusBar : JPanel() {
    private val statusMethod = JLabel()
    private val logFilePath = JTextField(STRINGS.ui.none) applyTooltip STRINGS.toolTip.savedFileTf
    private var logStatus: StatusBar.LogStatus = StatusBar.LogStatus.NONE

    init {
        border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        statusMethod.isOpaque = true
        logFilePath.isEditable = false
        logFilePath.border = BorderFactory.createEmptyBorder()
        add(statusMethod)
        add(logFilePath)

        logFilePath.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.isLeftClick.not()) {
                    logStatus.path.takeIf { it.isNotEmpty() }?.let { showOpenFilePopup(e, it) }
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
        statusMethod.text = status.status
        statusMethod.foreground = status.foregroundColor
        statusMethod.background = status.backgroundColor
        logFilePath.text = status.path.takeIf { it.isNotEmpty() } ?: STRINGS.ui.none
    }
}