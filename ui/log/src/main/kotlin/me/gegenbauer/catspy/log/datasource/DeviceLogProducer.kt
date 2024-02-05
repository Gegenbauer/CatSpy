package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.file.ensureDir
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.model.LogcatItem
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.platform.LOG_DIR
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.task.CommandExecutorImpl
import me.gegenbauer.catspy.task.CommandProcessBuilder
import me.gegenbauer.catspy.task.toCommandArray
import java.io.BufferedOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DeviceLogProducer(
    val device: String,
    private val processFetcher: AndroidProcessFetcher,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer() {
    override val tempFile: File = getTempLogFile()
    private val tempFileStream = BufferedOutputStream(tempFile.outputStream())

    private val commandExecutor by lazy {
        val logcatCommand = "${currentPlatform.adbCommand} ${"-s $device}".takeIf { device.isNotBlank() } ?: ""} logcat"
        CommandExecutorImpl(CommandProcessBuilder(logcatCommand.toCommandArray()))
    }

    override fun start(): Flow<Result<LogcatItem>> {
        val logcatOutput = commandExecutor.execute()
        return logcatOutput.map { result ->
            result.map { line ->
                suspender.checkSuspend()
                writeToFile(line)
                LogcatItem.from(line, logNum.getAndIncrement(), processFetcher.getPidToPackageMap())
            }
        }.onCompletion {
            moveToState(LogProducer.State.COMPLETE)
            flushTempFile()
        }
    }

    private fun writeToFile(line: String) {
        tempFileStream.write(StringBuilder(line).appendLine().toString().toByteArray())
    }

    override fun cancel() {
        super.cancel()
        commandExecutor.cancel()
        flushTempFile()
    }

    private fun flushTempFile() {
        kotlin.runCatching {
            AppScope.launch(NonCancellable) {
                tempFileStream.use { it.flush() }
            }
        }.onFailure {
            Log.e(TAG, "[flushTempFile] failed", it)
        }
    }

    override fun destroy() {
        super.destroy()
        commandExecutor.cancel()
    }

    private fun getTempLogFile(): File {
        val dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HH.mm.ss")
        val filePath = filesDir
            .appendPath(LOG_DIR)
            .appendPath(device)
            .appendPath("${GlobalProperties.APP_NAME}_${device}_${dtf.format(LocalDateTime.now())}.txt")
        return File(filePath).apply {
            if (exists().not()) {
                parentFile.ensureDir()
                createNewFile()
            }
        }
    }

    companion object {
        private const val TAG = "DeviceLogProducer"
    }
}
