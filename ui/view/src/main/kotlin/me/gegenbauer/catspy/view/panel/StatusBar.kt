package me.gegenbauer.catspy.view.panel

import java.awt.Color
import javax.swing.JComponent
import javax.swing.UIManager

interface StatusBar : TaskMonitor {
    var logStatus: LogStatus

    var memoryMonitorBar: JComponent

    var statusIcons: StatusIconsBar

    open class LogStatus(
        val backgroundColorProvider: () -> Color,
        val foregroundColorProvider: () -> Color,
        val status: String,
        val path: String
    ) {
        companion object {
            val NONE = LogStatus(
                { UIManager.getColor("Button.disabledSelectedBackground") },
                { UIManager.getColor("Button.background") },
                "",
                ""
            )
        }
    }

    class LogStatusIdle(status: String, path: String = "") : LogStatus(
        { UIManager.getColor("Button.disabledText") },
        { UIManager.getColor("Button.disabledBackground") },
        status,
        path
    )

    class LogStatusRunning(status: String, path: String = "") : LogStatus(
        { UIManager.getColor("CheckBox.background.selected") },
        { UIManager.getColor("CheckBox.foreground") },
        status,
        path
    )
}