package me.gegenbauer.catspy.view.panel

import java.awt.Color
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.UIManager

interface StatusBar : TaskMonitor {
    var logStatus: LogStatus

    var memoryMonitorBar: JComponent

    val statusIcons: StatusIconsBar

    val toolbar: BottomToolbar

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
        { UIManager.getColor("CatSpy.accent.green") },
        { UIManager.getColor("Button.default.foreground") },
        status,
        path
    )
}