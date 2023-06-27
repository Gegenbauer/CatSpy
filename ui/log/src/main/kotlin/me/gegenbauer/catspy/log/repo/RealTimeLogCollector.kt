package me.gegenbauer.catspy.log.repo

import me.gegenbauer.catspy.ddmlib.logcat.LogcatTask
import me.gegenbauer.catspy.task.Task
import me.gegenbauer.catspy.task.TaskManager

class RealTimeLogCollector(
    taskManager: TaskManager,
    device: String,
) : BaseLogcatLogCollector(taskManager) {
    override val collectorTask: Task = LogcatTask(device).apply {
        logTempFile = tempFile
    }
}