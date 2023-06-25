import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.gegenbauer.catspy.ddmlib.logcat.LogcatTask
import me.gegenbauer.catspy.task.TaskManager

fun main() {
    runBlocking {
        val taskManager = TaskManager()
        //val loadProcessPackageTask = LoadProcessPackageTask()
        //taskManager.exec(loadProcessPackageTask)
        taskManager.exec(LogcatTask(mapOf()))

        delay(2000)
    }
}