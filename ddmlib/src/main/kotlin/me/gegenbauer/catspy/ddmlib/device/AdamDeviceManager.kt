package me.gegenbauer.catspy.ddmlib.device

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.ddmlib.DdmLog
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class AdamDeviceManager : ContextService, DeviceManager {

    private val scope = ModelScope()
    private val adb = AndroidDebugBridgeClientFactory().build()
    private val deviceListLock = ReentrantReadWriteLock()
    private val deviceLock = ReentrantReadWriteLock()
    private val deviceListListeners = mutableListOf<DeviceListListener>()
    private val deviceListeners = mutableListOf<DeviceListener>()
    private val currentDevices = mutableListOf<Device>()
    private val adbServerRunning = AtomicBoolean()
    override val isAdbServerRunning: Boolean
        get() = adbServerRunning.get()

    private var receiveChannel: ReceiveChannel<*>? = null

    private val monitorExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is ConnectException) {
            scope.launch {
                receiveChannel?.cancel()
                adbServerRunning.set(false)
                DdmLog.v(TAG, "[startMonitor] end, restart")

                delay(3000)
                startMonitor()
            }
        }
    }

    override fun startMonitor() {
        scope.launch(monitorExceptionHandler) {
            DdmLog.d(TAG, "[startMonitor]")
            val deviceEventsChannel: ReceiveChannel<List<Device>> = adb.execute(
                request = AsyncDeviceMonitorRequest(),
                scope = scope
            )

            receiveChannel = deviceEventsChannel

            deviceEventsChannel.consumeEach {
                adbServerRunning.set(true)
                dispatchDeviceListChange(it)
            }
        }
    }

    override fun stopMonitor() {
        scope.cancel()
    }

    override fun registerDevicesListener(listener: DeviceListListener) {
        deviceListLock.write { deviceListListeners.add(listener) }
        dispatchDeviceListChange(currentDevices.toList())
    }

    override fun unregisterDevicesListener(listener: DeviceListListener) {
        deviceListLock.write { deviceListListeners.remove(listener) }
    }

    override fun isDeviceConnected(serial: String): Boolean {
        return currentDevices.find { it.serial == serial } != null
    }

    override fun registerDeviceListener(listener: DeviceListener) {
        deviceLock.write { deviceListeners.add(listener) }
    }

    override fun unregisterDeviceListener(listener: DeviceListener) {
        deviceLock.write { deviceListeners.remove(listener) }
    }

    private fun dispatchDeviceListChange(devices: List<Device>) {
        DdmLog.d(TAG, "[dispatchDeviceListChange] devices: $devices")
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
        DdmLog.d(TAG, "[dispatchDeviceStateChange] $deviceOld -> $deviceNew")
        deviceLock.read { deviceListeners.forEach { it.onDeviceStateChange(deviceOld, deviceNew) } }
    }

    private fun dispatchDeviceConnect(device: Device) {
        DdmLog.d(TAG, "[dispatchDeviceConnect] $device")
        deviceLock.read { deviceListeners.forEach { it.onDeviceConnect(device) } }
    }

    private fun dispatchDeviceDisconnect(device: Device) {
        DdmLog.d(TAG, "[dispatchDeviceDisconnect] $device")
        deviceLock.read { deviceListeners.forEach { it.onDeviceDisconnect(device) } }
    }

    override fun getDevices(): List<Device> {
        return currentDevices
    }

    companion object {
        private const val TAG = "DeviceManager"
    }
}

fun List<Device>.filterConnected(): List<Device> {
    return this.filter { it.state == DeviceState.DEVICE }
}