package me.gegenbauer.catspy.ddmlib.device

fun interface AdbServerStatusListener {
    fun onStateChange(connected: Boolean)
}