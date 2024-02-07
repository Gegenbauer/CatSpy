package me.gegenbauer.catspy.ddmlib.device

interface AdbServerStatusMonitor {

    val adbServerRunning: Boolean

    fun registerListener(listener: AdbServerStatusListener)
    fun unregisterListener(listener: AdbServerStatusListener)
}