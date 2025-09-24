package me.gegenbauer.catspy.log.metadata

import me.gegenbauer.catspy.java.ext.EMPTY_STRING

object LogcatLogSupport {
    fun getLogcatCommand(adbPath: String, device: String): String {
        val quotedAdbPath = if (adbPath.contains(" ")) "\"$adbPath\"" else adbPath
        return "$quotedAdbPath ${"-s $device".takeIf { device.isNotBlank() } ?: EMPTY_STRING} logcat -D"
    }
}