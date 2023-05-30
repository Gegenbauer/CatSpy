import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.gegenbauer.catspy.ddmlib.AndroidDebugBridgeManager
import me.gegenbauer.catspy.ddmlib.device.DeviceManager
import me.gegenbauer.catspy.ddmlib.logcat.LogcatTask
import me.gegenbauer.catspy.task.LoadProcessPackageTask
import me.gegenbauer.catspy.task.TaskManager

fun main() {
    runBlocking {
        AndroidDebugBridgeManager.init("adb")
        val taskManager = TaskManager()
        val loadProcessPackageTask = LoadProcessPackageTask()
        taskManager.exec(loadProcessPackageTask)
        val deviceManager = DeviceManager()
        deviceManager.startMonitor()
        delay(2000)
        taskManager.exec(LogcatTask(loadProcessPackageTask.getPidToNameMap()))
    }
}