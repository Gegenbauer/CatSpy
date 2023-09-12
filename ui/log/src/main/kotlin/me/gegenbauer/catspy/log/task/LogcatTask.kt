package me.gegenbauer.catspy.log.task

import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.file.ensureDir
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.LOG_DIR
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.strings.Configuration
import me.gegenbauer.catspy.task.CommandTask
import java.io.BufferedOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

class LogcatTask(private val device: String) : CommandTask(
    if (device.isEmpty()) arrayOf("adb", "logcat") else arrayOf("adb", "-s", device, "logcat"),
    name = "LogcatTask"
), LogTask {
    // test command
    // arrayOf("adb", "logcat")
    // arrayOf("cat", "/home/yingbin/.config/CatSpy/applog/53373619/CatSpy_53373619_20230513_11.39.26.txt")

    // TODO clean empty log file when process exit
    val tempFile = getTempLogFile()
    private val tempFileStream = BufferedOutputStream(tempFile.outputStream())
    private val lineCount = AtomicInteger(0)

    override suspend fun onReceiveOutput(line: String) {
        super.onReceiveOutput(line)
        lineCount.incrementAndGet()
        writeToFile(line)
    }

    private fun writeToFile(line: String) {
        tempFileStream.write(StringBuilder(line).appendLine().toString().toByteArray())
    }

    private fun getTempLogFile(): File {
        val dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HH.mm.ss")
        val filePath = filesDir
            .appendPath(LOG_DIR)
            .appendPath(device)
            .appendPath("${Configuration.APP_NAME}_${device}_${dtf.format(LocalDateTime.now())}.txt")
        return File(filePath).apply {
            if (exists().not()) {
                parentFile.ensureDir()
                createNewFile()
            }
        }
    }

    override fun onProcessEnd() {
        super.onProcessEnd()
        GLog.d(name, "[onProcessEnd] total line count = ${lineCount.get()}")
        runCatching {
            tempFileStream.use {
                it.flush()
            }
        }.onFailure {
            GLog.e(name, "[onProcessEnd]", it)
        }
    }
}