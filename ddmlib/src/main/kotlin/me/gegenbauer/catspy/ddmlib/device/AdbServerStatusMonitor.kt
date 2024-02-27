package me.gegenbauer.catspy.ddmlib.device

interface AdbServerStatusMonitor {

    val adbServerRunning: Boolean

    fun registerAdbServerStatusListener(listener: AdbServerStatusListener)
    fun unregisterAdbServerStatusListener(listener: AdbServerStatusListener)
}