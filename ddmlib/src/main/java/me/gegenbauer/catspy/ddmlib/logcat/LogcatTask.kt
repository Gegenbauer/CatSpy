package me.gegenbauer.catspy.ddmlib.logcat

import com.android.ddmlib.Log
import com.android.ddmlib.logcat.LogCatMessage
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.logcat.ChanneledLogcatRequest
import kotlinx.coroutines.channels.consumeEach
import me.gegenbauer.catspy.task.PausableTask

class LogcatTask(private val pidToNameMap: Map<String, String>) : PausableTask(name = "LogcatTask") {
    private val adb = AndroidDebugBridgeClientFactory().build()
    private val parser = LogCatMessageParser()

    override suspend fun startInCoroutine() {
        super.startInCoroutine()
        val channel = adb.execute(
            request = ChanneledLogcatRequest(),
            scope = scope,
            serial = "emulator-5554"
        )

        channel.consumeEach { logcatChunk ->
            //logcatChunk == "I/ActivityManager(  585): Starting activity: Intent { action=android.intent.action...}\nI/MyActivity( 1557): MyClass"
            //write to a file or append to a buffer

            //Dispose of channel to close the resources
            processLogLines(logcatChunk.split("\n").toTypedArray())
        }
    }

    private fun processLogLines(lines: Array<String>) {
        val newMessages: List<LogCatMessage> = parser.processLogLines(lines, pidToNameMap)
        if (newMessages.isNotEmpty()) {
            println(newMessages.map { it })
        }
    }

    companion object {
        private val deviceDisconnectedMsg = LogCatMessage(Log.LogLevel.ERROR, "Device disconnected: 1")
        private val connectionTimeoutMsg = LogCatMessage(Log.LogLevel.ERROR, "LogCat Connection timed out")
        private val connectionErrorMsg = LogCatMessage(Log.LogLevel.ERROR, "LogCat Connection error")
    }
}