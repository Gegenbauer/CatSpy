package me.gegenbauer.catspy.ddmlib.device

import com.malinskiy.adam.request.device.Device

interface DeviceManager {

    var isMonitoring: Boolean

    val isAdbServerRunning: Boolean

    fun startMonitor()

    fun tryStartMonitor()

    fun stopMonitor()

    fun registerDeviceListener(listener: DeviceListener)

    fun unregisterDeviceListener(listener: DeviceListener)

    fun isDeviceConnected(serial: String): Boolean

    fun registerDevicesListener(listener: DeviceListListener)

    fun unregisterDevicesListener(listener: DeviceListListener)

    fun getDevices(): List<Device>
}