package me.gegenbauer.catspy.ddmlib.device

import com.malinskiy.adam.request.device.Device

interface DeviceObserver {
    fun onDeviceStateChange(deviceOld: Device, deviceNew: Device) {
        //
    }

    fun onDeviceDisconnect(device: Device) {
        //
    }

    fun onDeviceConnect(device: Device) {
        //
    }
}