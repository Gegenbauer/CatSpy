package me.gegenbauer.catspy.ddmlib.device

import com.malinskiy.adam.request.device.Device
import me.gegenbauer.catspy.ddmlib.adb.AdbConf

interface DeviceMonitor {

    var isMonitoring: Boolean

    fun configure(adbConf: AdbConf)

    fun tryStartMonitor()

    fun tryStopMonitor()

    fun registerDeviceListener(listener: DeviceObserver)

    fun unregisterDeviceListener(listener: DeviceObserver)

    fun isDeviceConnected(serial: String): Boolean

    fun registerDevicesListener(listener: DeviceListObserver)

    fun unregisterDevicesListener(listener: DeviceListObserver)

    fun getDevices(): List<Device>
}