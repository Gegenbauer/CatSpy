package me.gegenbauer.catspy.ddmlib.device

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.ddmlib.adb.AdbConf
import me.gegenbauer.catspy.ddmlib.log.DdmLog
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

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is RequestRejectedException) {
            DdmLog.e(TAG, "[exceptionHandler] RequestRejectedException: ${throwable.message}")
        } else {
            DdmLog.d(TAG, "[exceptionHandler] ${throwable.message}")
        }
    }

    override fun configure(adbConf: AdbConf) {
        this.adbConf = adbConf
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

    @Synchronized
    private fun startMonitor() {
        if (isMonitoring) {
            return
        }
        monitorJob = scope.launch(exceptionHandler) {
            coroutineContext.job.invokeOnCompletion {
                isMonitoring = false
                it ?: return@invokeOnCompletion
                if (it !is CancellationException) {
                    handleMonitorException()
                }
            }

            DdmLog.i(TAG, "[startMonitor]")

            isMonitoring = true

            val deviceEventsChannel: ReceiveChannel<List<Device>> = adb.execute(
                request = AsyncDeviceMonitorRequest(),
                scope = this
            )

            receiveChannel = deviceEventsChannel

            deviceEventsChannel.consumeEach {
                _adbServerRunning.set(true)
                dispatchAdbServerStatusChange()
                dispatchDeviceListChange(it.filterConnected())
            }
        }
    }

    private fun handleMonitorException() {
        scope.launch {
            synchronized(this@AdamDeviceMonitor) {
                if (adbObserverCount == 0) {
                    return@launch
                }
            }

            dispatchDeviceListChange(emptyList())

            receiveChannel?.cancel()
            _adbServerRunning.set(false)
            dispatchAdbServerStatusChange()
            DdmLog.v(TAG, "[startMonitor] end, restart")

            delay(3000)
            startMonitor()
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

    override fun registerAdbServerStatusListener(listener: AdbServerStatusListener) {
        adbStatusLock.write { adbServerStatusListeners.add(listener) }
        dispatchAdbServerStatusChange()
    }

    override fun unregisterAdbServerStatusListener(listener: AdbServerStatusListener) {
        adbStatusLock.write { adbServerStatusListeners.remove(listener) }
    }

    private fun dispatchAdbServerStatusChange() {
        adbStatusLock.read { adbServerStatusListeners.forEach { it.onStateChange(_adbServerRunning.get()) } }
    }

    private class AdbConfNotConfiguredException : RuntimeException("AdbConf not configured")

    companion object {
        private const val TAG = "AdamDeviceMonitor"
    }
}

fun List<Device>.filterConnected(): List<Device> {
    return this.filter { it.state == DeviceState.DEVICE }
}