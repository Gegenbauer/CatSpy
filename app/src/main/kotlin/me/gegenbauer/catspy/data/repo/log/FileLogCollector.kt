package me.gegenbauer.catspy.data.repo.log

import me.gegenbauer.catspy.task.ReadFileTask
import me.gegenbauer.catspy.task.Task
import me.gegenbauer.catspy.task.TaskManager
import java.io.File

class FileLogCollector(
    taskManager: TaskManager,
    fileName: String,
) : BaseLogcatLogCollector(taskManager) {

    override val collectorTask: Task = fileName.run {
        val file = File(this)
        logTempFile = file
        checkFile(file)
        ReadFileTask(file)
    }

    private fun checkFile(file: File) {
        require(file.exists()) { notifyError(IllegalArgumentException("File ${file.absolutePath} does not exist")) }
    }

}