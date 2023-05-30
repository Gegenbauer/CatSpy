import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.gegenbauer.catspy.ddmlib.device.DeviceManager
import me.gegenbauer.catspy.log.GLog

fun main() {

//Use coroutineScope
    runBlocking {
        val deviceManager = DeviceManager()
        deviceManager.startMonitor()
        deviceManager.registerDeviceListener { devices -> GLog.d("ASD", "onDeviceListUpdate: $devices") }
        delay(10000)
    }
}