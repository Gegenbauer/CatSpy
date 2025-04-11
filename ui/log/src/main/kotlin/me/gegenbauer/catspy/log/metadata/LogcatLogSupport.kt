package me.gegenbauer.catspy.log.metadata

import me.gegenbauer.catspy.java.ext.EMPTY_STRING

object LogcatLogSupport {
    fun getLogcatCommand(adbPath: String, device: String): String {
        return "$adbPath ${"-s $device".takeIf { device.isNotBlank() } ?: EMPTY_STRING} logcat -D"
    }
}