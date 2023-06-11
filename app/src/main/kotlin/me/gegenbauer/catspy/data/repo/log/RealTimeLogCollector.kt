package me.gegenbauer.catspy.data.repo.log

import me.gegenbauer.catspy.task.LogcatTask
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