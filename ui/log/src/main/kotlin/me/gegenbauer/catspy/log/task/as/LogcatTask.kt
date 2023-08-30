package me.gegenbauer.catspy.log.task.`as`

import com.android.ddmlib.Log
import com.android.ddmlib.logcat.LogCatMessage
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.logcat.ChanneledLogcatRequest
import com.malinskiy.adam.request.logcat.LogcatReadMode
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.file.ensureDir
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.app
import me.gegenbauer.catspy.task.PausableTask
import me.gegenbauer.catspy.platform.LOG_DIR
import me.gegenbauer.catspy.platform.filesDir
import java.io.BufferedOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LogcatTask(private val device: String) : PausableTask(name = "LogcatTask") {
    val tempFile = getTempLogFile()

    private val adb = AndroidDebugBridgeClientFactory().build()

    // TODO clean empty log file when process exit
    private val tempFileStream = BufferedOutputStream(tempFile.outputStream())
    private val lineChars = StringBuilder()
    private lateinit var outputChannel: ReceiveChannel<String>

    override suspend fun startInCoroutine() {
        super.startInCoroutine()
        outputChannel = adb.execute(
            request = ChanneledLogcatRequest(modes = listOf(LogcatReadMode.threadtime)),
            scope = scope,
            serial = device
        )
        setRunning(true)
        outputChannel.consumeEach { logcatChunk ->

            addPausePoint()

            logcatChunk.forEach {
                if (it == '\n' && lineChars.isNotBlank()) {
                    processLogLine(lineChars.toString())
                    lineChars.clear()
                } else {
                    lineChars.append(it)
                }
            }
            tempFileStream.write(logcatChunk.toByteArray())
        }

        onStop()
        setRunning(false)
    }

    override fun cancel() {
        super.cancel()
        outputChannel.cancel()
    }

    private fun processLogLine(line: String) {
        notifyProgress(line)
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

    private fun onStop() {
        tempFileStream.flush()
    }

    companion object {

        // TODO use long buffer
        private val deviceDisconnectedMsg = LogCatMessage(Log.LogLevel.ERROR, "Device disconnected: 1")
        private val connectionTimeoutMsg = LogCatMessage(Log.LogLevel.ERROR, "LogCat Connection timed out")
        private val connectionErrorMsg = LogCatMessage(Log.LogLevel.ERROR, "LogCat Connection error")
    }
}