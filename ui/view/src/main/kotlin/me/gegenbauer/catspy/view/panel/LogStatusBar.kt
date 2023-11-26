package me.gegenbauer.catspy.view.panel

import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.applyTooltip
import java.awt.event.MouseEvent
import javax.swing.*

class LogStatusBar : JPanel() {
    private val statusMethod = JLabel()
    private val logFilePath = JTextField(STRINGS.ui.none) applyTooltip STRINGS.toolTip.savedFileTf

    init {
        border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        statusMethod.isOpaque = true
        logFilePath.isEditable = false
        logFilePath.border = BorderFactory.createEmptyBorder()
        add(statusMethod)
        add(logFilePath)
    }

    fun setLogStatus(status: StatusBar.LogStatus) {
        statusMethod.text = status.status
        statusMethod.foreground = status.foregroundColor
        statusMethod.background = status.backgroundColor
        logFilePath.text = status.path
    }
}