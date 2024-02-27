package me.gegenbauer.catspy.ddmlib.adb

data class AdbConf(
    val path: String,
    val port: Int = 5037,
)