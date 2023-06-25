package me.gegenbauer.catspy.ddmlib

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.log.GLog

class AndroidDebugBridgeManager : AndroidDebugBridge.IDebugBridgeChangeListener {
    private val scope = ModelScope()

    fun init(adbPath: String) {
        AndroidDebugBridge.init(true, true, System.getenv())
        AndroidDebugBridge.addDebugBridgeChangeListener(this)
        AndroidDebugBridge.createBridge(adbPath, true)
    }

    fun connected(): Boolean {
        return AndroidDebugBridge.getBridge()?.isConnected ?: false
    }

    override fun bridgeChanged(bridge: AndroidDebugBridge?) {
        bridge ?: return
        scope.launch {
            if (bridge.isConnected.not()) {
                delay(5000)
                GLog.d(TAG, "[bridgeChanged] not connected to adb server, restart")
                bridge.restart()
            } else {
                GLog.d(TAG, "[bridgeChanged] connected to adb server")
            }
        }
    }

    fun getDevices(): List<IDevice> {
        return AndroidDebugBridge.getBridge()?.devices?.toList() ?: emptyList()
    }

    fun getDeviceByName(deviceName: String): IDevice? {
        return getDevices().firstOrNull { it.name == deviceName }
    }

    companion object {
        private const val TAG = "AndroidDebugBridgeManager"
    }
}