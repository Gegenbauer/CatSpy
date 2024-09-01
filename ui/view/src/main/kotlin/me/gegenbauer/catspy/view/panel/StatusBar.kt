package me.gegenbauer.catspy.view.panel

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.awt.Color
import javax.swing.JComponent
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
                EMPTY_STRING,
                EMPTY_STRING
            )
        }
    }

    class LogStatusIdle(status: String, path: String = EMPTY_STRING) : LogStatus(
        { UIManager.getColor("Button.disabledText") },
        { UIManager.getColor("Button.disabledBackground") },
        status,
        path
    )

    class LogStatusRunning(status: String, path: String = EMPTY_STRING) : LogStatus(
        { UIManager.getColor("CatSpy.accent.green") },
        { UIManager.getColor("Button.default.foreground") },
        status,
        path
    )
}