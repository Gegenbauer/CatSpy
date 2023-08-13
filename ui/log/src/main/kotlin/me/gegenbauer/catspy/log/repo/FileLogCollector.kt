package me.gegenbauer.catspy.log.repo

import me.gegenbauer.catspy.log.task.LogTask
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
        LocalFileLogReadTask(file)
    }

    private fun checkFile(file: File) {
        require(file.exists()) { notifyError(IllegalArgumentException("File ${file.absolutePath} does not exist")) }
    }

    class LocalFileLogReadTask(file: File) : ReadFileTask(file), LogTask
}