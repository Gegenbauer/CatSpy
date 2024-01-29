package me.gegenbauer.catspy.ddmlib

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener
import com.android.ddmlib.IDevice
import com.android.ddmlib.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.ddmlib.log.DdmLog
import me.gegenbauer.catspy.ddmlib.log.DdmLogDelegate
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class AndroidDebugBridgeManager : AndroidDebugBridge.IDebugBridgeChangeListener, IDeviceChangeListener, ContextService {
    private val scope = ModelScope()
    private val listeners = Collections.synchronizedCollection(mutableListOf<AdbStateChangeListener>())
    private val currentConnectState = AtomicBoolean(false)

    fun init(adbPath: String) {
        Log.addLogger(DdmLogDelegate())
        runCatching {
            AndroidDebugBridge.init(true, true, System.getenv())
            AndroidDebugBridge.addDebugBridgeChangeListener(this)
            AndroidDebugBridge.addDeviceChangeListener(this)
            AndroidDebugBridge.createBridge(adbPath, true)
        }.onFailure {
            DdmLog.e(TAG, "[init] failed to init adb", it)
        }
    }

    fun connected(): Boolean {
        return AndroidDebugBridge.getBridge()?.isConnected ?: false
    }

    fun addListener(listener: AdbStateChangeListener) {
        listeners.add(listener)
        listener.onStateChange(connected())
    }

    fun removeListener(listener: AdbStateChangeListener) {
        listeners.remove(listener)
    }

    fun getDevices(): List<IDevice> {
        return AndroidDebugBridge.getBridge()?.devices?.toList() ?: emptyList()
    }

    fun getDeviceByName(deviceName: String): IDevice? {
        return getDevices().firstOrNull { it.name == deviceName }
    }

    private fun dispatchStateChange() {
        listeners.toList().forEach { it.onStateChange(connected()) }
    }

    override fun bridgeChanged(bridge: AndroidDebugBridge?) {
        bridge ?: return
        scope.launch {
            if (bridge.isConnected.not()) {
                delay(5000)
                DdmLog.e(TAG, "[bridgeChanged] not connected to adb server, restart")
                //bridge.restart()
            } else {
                DdmLog.d(TAG, "[bridgeChanged] connected to adb server")
            }
            dispatchStateChange()
        }
    }

    override fun deviceConnected(device: IDevice) {
        updateConnectState()
    }

    override fun deviceDisconnected(device: IDevice) {
        updateConnectState()
    }

    private fun updateConnectState() {
        DdmLog.d(TAG, "[updateConnectState] connected: ${connected()}")
        if (currentConnectState.get() != connected()) {
            dispatchStateChange()
            currentConnectState.set(connected())
            if (connected().not()) {
                scope.launch {
                    delay(2000)
                    DdmLog.e(TAG, "[updateConnectState] not connected to adb server, restart")
                    AndroidDebugBridge.getBridge()?.restart()
                }
            }
        }
    }

    override fun restartCompleted(isSuccessful: Boolean) {
        updateConnectState()
    }

    override fun deviceChanged(device: IDevice, changeMask: Int) {
        //
    }

    companion object {
        private const val TAG = "AndroidDebugBridgeManager"
    }
}

fun interface AdbStateChangeListener {
    fun onStateChange(connected: Boolean)
}