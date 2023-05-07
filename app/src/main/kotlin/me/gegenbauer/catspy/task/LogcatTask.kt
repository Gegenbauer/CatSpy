package me.gegenbauer.catspy.task

import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.log.appendPath
import me.gegenbauer.catspy.log.ensureDir
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.utils.LOG_DIR
import me.gegenbauer.catspy.utils.filesDir
import java.io.BufferedOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LogcatTask(private val device: String) : CommandTask(arrayOf("adb", "logcat")) {
    override val name: String = "LogcatTask"

    // TODO clean empty log file when process exit
    private val tempFile = getTempLogFile()
    private val tempFileStream = BufferedOutputStream(tempFile.outputStream())

    override suspend fun onReceiveOutput(line: String) {
        super.onReceiveOutput(line)
        GLog.d(name, "[onReceiveOutput] $line")
        writeToFile(line)
    }

    private fun writeToFile(line: String) {
        tempFileStream.write(line.toByteArray())
    }

    private fun getTempLogFile(): File {
        val dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HH.mm.ss")
        val filePath = filesDir
            .appendPath(LOG_DIR)
            .appendPath(device)
            .appendPath("${STRINGS.ui.app}_${device}_${dtf.format(LocalDateTime.now())}.txt")
        return File(filePath).apply {
            if (exists().not()) {
                parentFile.ensureDir()
                createNewFile()
            }
        }
    }
}