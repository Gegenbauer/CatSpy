package me.gegenbauer.catspy.ddmlib.adb

import java.net.Socket

data class AdbConf(
    val path: String,
    val port: Int = 5037,
)

fun AdbConf.socket(): Socket = Socket("localhost", port)