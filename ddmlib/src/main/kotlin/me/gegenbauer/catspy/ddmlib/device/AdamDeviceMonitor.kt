package me.gegenbauer.catspy.ddmlib.device

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.ddmlib.adb.AdbConf
import me.gegenbauer.catspy.ddmlib.adb.isServerRunning
import me.gegenbauer.catspy.ddmlib.adb.startServer
import me.gegenbauer.catspy.ddmlib.log.DdmLog
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class AdamDeviceMonitor : ContextService, DeviceMonitor, AdbServerStatusMonitor {

    override var isMonitoring = false

    private val scope = ModelScope()
    private val adb = AndroidDebugBridgeClientFactory().build()
    private val deviceListLock = ReentrantReadWriteLock()
    private val deviceLock = ReentrantReadWriteLock()
    private val deviceListObservers = mutableListOf<DeviceListObserver>()
    private val deviceObservers = mutableListOf<DeviceObserver>()
    private val currentDevices = mutableListOf<Device>()

    private val adbStatusLock = ReentrantReadWriteLock()
    private val adbServerStatusListeners = mutableListOf<AdbServerStatusListener>()

    private val _adbServerRunning = AtomicBoolean()
    override val adbServerRunning: Boolean
        get() = _adbServerRunning.get()

    private var adbObserverCount = 0

    private var receiveChannel: ReceiveChannel<*>? = null
    private var monitorJob: Job? = null
    private var adbConf: AdbConf? = null

    private val monitorExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is ConnectException) {
            scope.launch {

                dispatchDeviceListChange(emptyList())

                receiveChannel?.cancel()
                _adbServerRunning.set(false)
                DdmLog.v(TAG, "[startMonitor] end, restart")

                if (checkAndStartServer().not()) {
                    DdmLog.e(TAG, "[startMonitor] failed to start adb server")
                    return@launch
                }

                delay(3000)
                startMonitor()
            }
        } else if (throwable is CancellationException) {
            DdmLog.v(TAG, "[startMonitor] cancelled")
        }
    }

    private fun checkAndStartServer(): Boolean {
        val conf = adbConf ?: return false
        if (!isServerRunning(conf)) {
            val result = startServer(conf)
            DdmLog.i(TAG, "[configure] adb server started result: $result")
            return result
        }
        return true
    }

    override fun configure(adbConf: AdbConf) {
        this.adbConf = adbConf
        scope.launch {
            monitorJob?.cancelAndJoin()
            val result = startServer(adbConf)
            DdmLog.i(TAG, "[configure] adb server started result: $result")
        }
    }

    @Synchronized
    override fun tryStartMonitor() {
        checkAdbConf()
        DdmLog.i(TAG, "[tryStartMonitor] adbObserverCount: $adbObserverCount")
        if (adbObserverCount == 0) {
            startMonitor()
        }
        adbObserverCount++
    }

    @Synchronized
    override fun tryStopMonitor() {
        checkAdbConf()
        adbObserverCount--
        DdmLog.i(TAG, "[tryStopMonitor] adbObserverCount: $adbObserverCount")
        if (adbObserverCount == 0) {
            stopMonitor()
        }
    }

    private fun checkAdbConf() {
        adbConf ?: throw AdbConfNotConfiguredException()
    }

    private fun startMonitor() {
        monitorJob = scope.launch(monitorExceptionHandler) {
            DdmLog.i(TAG, "[startMonitor]")

            isMonitoring = true

            coroutineContext.job.invokeOnCompletion {
                isMonitoring = false
            }

            val deviceEventsChannel: ReceiveChannel<List<Device>> = adb.execute(
                request = AsyncDeviceMonitorRequest(),
                scope = scope
            )

            receiveChannel = deviceEventsChannel

            deviceEventsChannel.consumeEach {
                _adbServerRunning.set(true)
                dispatchDeviceListChange(it.filterConnected())
            }
        }
    }

    override fun onContextDestroyed(context: Context) {
        stopMonitor()
    }

    private fun stopMonitor() {
        DdmLog.i(TAG, "[stopMonitor]")
        monitorJob?.cancel()
    }

    override fun registerDevicesListener(listener: DeviceListObserver) {
        deviceListLock.write { deviceListObservers.add(listener) }
        dispatchDeviceListChange(currentDevices.toList())
    }

    override fun unregisterDevicesListener(listener: DeviceListObserver) {
        deviceListLock.write { deviceListObservers.remove(listener) }
    }

    override fun isDeviceConnected(serial: String): Boolean {
        return currentDevices.any { it.serial == serial }
    }

    override fun registerDeviceListener(listener: DeviceObserver) {
        deviceLock.write { deviceObservers.add(listener) }
    }

    override fun unregisterDeviceListener(listener: DeviceObserver) {
        deviceLock.write { deviceObservers.remove(listener) }
    }

    private fun dispatchDeviceListChange(devices: List<Device>) {
        DdmLog.d(TAG, "[dispatchDeviceListChange] devices: $devices")
        diffDeviceListChange(devices)
        deviceListLock.read { deviceListObservers.forEach { it.onDeviceListUpdate(devices) } }
        currentDevices.clear()
        currentDevices.addAll(devices)
    }

    private fun diffDeviceListChange(devices: List<Device>) {
        val addedDevices = arrayListOf<Device>()
        val removedDevices = arrayListOf<Device>()
        val notChangedDevices = arrayListOf<Device>()

        devices.forEach {
            if (currentDevices.any { device -> device.serial == it.serial }) {
                notChangedDevices.add(it)
            } else {
                addedDevices.add(it)
            }
        }
        currentDevices.forEach {
            if (devices.any { device -> device.serial == it.serial }) {
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
        deviceLock.read { deviceObservers.forEach { it.onDeviceStateChange(deviceOld, deviceNew) } }
    }

    private fun dispatchDeviceConnect(device: Device) {
        DdmLog.d(TAG, "[dispatchDeviceConnect] $device")
        deviceLock.read { deviceObservers.forEach { it.onDeviceConnect(device) } }
    }

    private fun dispatchDeviceDisconnect(device: Device) {
        DdmLog.d(TAG, "[dispatchDeviceDisconnect] $device")
        deviceLock.read { deviceObservers.forEach { it.onDeviceDisconnect(device) } }
    }

    override fun getDevices(): List<Device> {
        return currentDevices
    }

    override fun registerListener(listener: AdbServerStatusListener) {
        adbStatusLock.write { adbServerStatusListeners.add(listener) }
    }

    override fun unregisterListener(listener: AdbServerStatusListener) {
        adbStatusLock.write { adbServerStatusListeners.remove(listener) }
    }

    private fun dispatchAdbServerStatusChange() {
        adbStatusLock.read { adbServerStatusListeners.forEach { it.onStateChange(_adbServerRunning.get()) } }
    }

    private class AdbConfNotConfiguredException : RuntimeException("AdbConf not configured")

    companion object {
        private const val TAG = "DeviceManager"
    }
}

fun List<Device>.filterConnected(): List<Device> {
    return this.filter { it.state == DeviceState.DEVICE }
}