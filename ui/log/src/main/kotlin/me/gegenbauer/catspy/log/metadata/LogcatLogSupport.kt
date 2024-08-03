package me.gegenbauer.catspy.log.metadata

import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import java.awt.Color

object LogcatLog {
    private val levels = listOf(
        Level(0, "Verbose", "V"),
        Level(1, "Debug", "D"),
        Level(2, "Info", "I"),
        Level(3, "Warning", "W"),
        Level(4, "Error", "E"),
        Level(5, "Fatal", "F")
    )

    private val displayedLevelColors = listOf(
        DarkThemeAwareColor(Color(0x000000), Color(0xF0F0F0)),
        DarkThemeAwareColor(Color(0x209000), Color(0x6C9876)),
        DarkThemeAwareColor(Color(0x0080DF), Color(0x5084C4)),
        DarkThemeAwareColor(Color(0xF07000), Color(0xCB8742)),
        DarkThemeAwareColor(Color(0xD00000), Color(0xCD6C79)),
        DarkThemeAwareColor(Color(0x700000), Color(0xED3030)),
    )

    val displayedLevels = levels.mapIndexed { index, level -> level.toDisplayedLevel(displayedLevelColors[index]) }

    fun getLogcatCommand(adbPath: String, device: String): String {
        return "$adbPath ${"-s $device".takeIf { device.isNotBlank() } ?: ""} logcat"
    }
}