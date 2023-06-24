package me.gegenbauer.catspy.ddmlib.device

import com.malinskiy.adam.request.device.Device

fun interface DeviceListListener {

    fun onDeviceListUpdate(devices: List<Device>)
}