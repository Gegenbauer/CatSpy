package me.gegenbauer.catspy.ddmlib.device

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.ddmlib.AdbStateChangeListener
import me.gegenbauer.catspy.log.GLog
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class DeviceManager: ContextService, AdbStateChangeListener {

    private val scope = ModelScope()
    private val adb = AndroidDebugBridgeClientFactory().build()
    private val deviceListLock = ReentrantReadWriteLock()
    private val deviceLock = ReentrantReadWriteLock()
    private val deviceListListeners = mutableListOf<DeviceListListener>()
    private val deviceListeners = mutableListOf<DeviceListener>()
    private val currentDevices = mutableListOf<Device>()

    fun startMonitor() {
        scope.launch {
            GLog.d(TAG, "[startMonitor]")
            val deviceEventsChannel: ReceiveChannel<List<Device>> = adb.execute(
                request = AsyncDeviceMonitorRequest(),
                scope = scope
            )

            deviceEventsChannel.consumeEach { dispatchDeviceListChange(it) }
            GLog.d(TAG, "[startMonitor] end, restart")

            delay(3000)
            startMonitor()
        }
    }

    fun registerDeviceListener(listener: DeviceListListener) {
        deviceListLock.write { deviceListListeners.add(listener) }
        diffDeviceListChange(currentDevices)
    }

    fun unregisterDeviceListener(listener: DeviceListListener) {
        deviceListLock.write { deviceListListeners.remove(listener) }
    }

    fun registerDeviceListener(listener: DeviceListener) {
        deviceLock.write { deviceListeners.add(listener) }
    }

    fun unregisterDeviceListener(listener: DeviceListener) {
        deviceLock.write { deviceListeners.remove(listener) }
    }

    private fun dispatchDeviceListChange(devices: List<Device>) {
        GLog.d(TAG, "[dispatchDeviceListChange] devices: $devices")
        diffDeviceListChange(devices)
        deviceListLock.read { deviceListListeners.forEach { it.onDeviceListUpdate(devices) } }
        currentDevices.clear()
        currentDevices.addAll(devices)
    }

    private fun diffDeviceListChange(devices: List<Device>) {
        val addedDevices = arrayListOf<Device>()
        val removedDevices = arrayListOf<Device>()
        val notChangedDevices = arrayListOf<Device>()

        devices.forEach {
            if (currentDevices.find { device -> device.serial == it.serial } != null) {
                notChangedDevices.add(it)
            } else {
                addedDevices.add(it)
            }
        }
        currentDevices.forEach {
            if (devices.find { device -> device.serial == it.serial } == null) {
                removedDevices.add(it)
            }
        }

        addedDevices.forEach { dispatchDeviceConnect(it) }
        removedDevices.forEach { dispatchDeviceDisconnect(it) }
        notChangedDevices.forEach { newDevice ->
            val oldDevice = currentDevices.first { it.serial == newDevice.serial }
            if (newDevice.state != oldDevice.state) {
                dispatchDeviceStateChange(oldDevice, newDevice)
            }
        }
    }

    private fun dispatchDeviceStateChange(deviceOld: Device, deviceNew: Device) {
        GLog.d(TAG, "[dispatchDeviceStateChange] $deviceOld -> $deviceNew")
        deviceLock.read { deviceListeners.forEach { it.onDeviceStateChange(deviceOld, deviceNew) } }
    }

    private fun dispatchDeviceConnect(device: Device) {
        GLog.d(TAG, "[dispatchDeviceConnect] $device")
        deviceLock.read { deviceListeners.forEach { it.onDeviceConnect(device) } }
    }

    private fun dispatchDeviceDisconnect(device: Device) {
        GLog.d(TAG, "[dispatchDeviceDisconnect] $device")
        deviceLock.read { deviceListeners.forEach { it.onDeviceDisconnect(device) } }
    }

    override fun onStateChange(connected: Boolean) {
        if (connected) {
            scope.launch { startMonitor() }.invokeOnCompletion {
                GLog.e(TAG, "[startMonitor] failed", it)
            }
        }
    }

    fun getDevices(): List<Device> {
        return currentDevices
    }

    companion object {
        private const val TAG = "DeviceManager"
    }
}

fun List<Device>.filterConnected(): List<Device> {
    return this.filter { it.state == DeviceState.DEVICE }
}