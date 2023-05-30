import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.gegenbauer.catspy.ddmlib.AndroidDebugBridgeManager
import me.gegenbauer.catspy.ddmlib.device.DeviceManager

fun main() {
    runBlocking {
        val deviceManager = DeviceManager()
        AndroidDebugBridgeManager.init("adb")
        deviceManager.startMonitor()
        delay(2000)
        val devices = AndroidDebugBridgeManager.getDevices()
        println(devices)
    }
}