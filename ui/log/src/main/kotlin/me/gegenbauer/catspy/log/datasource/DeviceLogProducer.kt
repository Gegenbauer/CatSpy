package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.file.appendExtension
import me.gegenbauer.catspy.file.appendName
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.file.getValidFileName
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.metadata.LogcatLogSupport
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.platform.LOG_DIR
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.task.CommandExecutorImpl
import me.gegenbauer.catspy.task.CommandProcessBuilder
import me.gegenbauer.catspy.task.toCommandArray
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DeviceLogProducer(
    val device: String,
    logConfiguration: LogConfiguration,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer(logConfiguration) {
    override val tempFile: File by lazy { getTempLogFile() }
    private val tempFileStream: OutputStream by lazy { BufferedOutputStream(tempFile.outputStream()) }
    private val processFetcher = AndroidProcessFetcher(device)

    private val commandExecutor by lazy {
        val logcatCommand = LogcatLogSupport.getLogcatCommand(SettingsManager.adbPath, device)
        val bufferCommand = logConfiguration.getLogBuffers().joinToString(" ") { "-b $it" }
        val logcatCommandWithBuffer = "$logcatCommand $bufferCommand"
        CommandExecutorImpl(CommandProcessBuilder(logcatCommandWithBuffer.toCommandArray()))
    }

    override fun start(): Flow<Result<LogItem>> {
        val logcatOutput = commandExecutor.execute()
        return logcatOutput.onStart {
            processFetcher.init()
        }.map { result ->
            result.map { line ->
                suspender.checkSuspend()
                writeToFile(line)
                val num = logNum.getAndIncrement()
                val parts = parseLog(line).toMutableList()
                val packageName = processFetcher.queryPackageName(parts[PART_INDEX_PID], parts[PART_INDEX_TIME])
                parts.add(PART_INDEX_PACKAGE, packageName)
                LogItem(num, parts)
            }
        }.onStart {
            moveToState(LogProducer.State.RUNNING)
        }.onCompletion {
            moveToState(LogProducer.State.COMPLETE)
            flushTempFile()
        }
    }

    override fun pause() {
        super.pause()
        processFetcher.invalidateTimeOnDevice()
    }

    private fun writeToFile(line: String) {
        tempFileStream.write(line.toByteArray())
        tempFileStream.write("\n".toByteArray())
    }

    override fun cancel() {
        super.cancel()
        commandExecutor.cancel()
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
        val dtf = DateTimeFormatter.ofPattern(LOG_FILE_NAME_TIME_FORMAT)
        val deviceName = getValidFileName(device)
        val filePath = filesDir
            .appendPath(LOG_DIR)
            .appendPath(deviceName)
            .appendPath(GlobalProperties.APP_NAME)
            .appendName(deviceName)
            .appendName(dtf.format(LocalDateTime.now()))
            .appendExtension(LOG_FILE_SUFFIX)
        return File(filePath).apply {
            if (exists().not()) {
                parentFile.mkdirs()
                createNewFile()
            }
        }
    }

    companion object {
        private const val TAG = "DeviceLogProducer"

        private const val PART_INDEX_TIME = 0
        private const val PART_INDEX_PID = 1
        private const val PART_INDEX_PACKAGE = 2
        private const val LOG_FILE_SUFFIX = "txt"
        private const val LOG_FILE_NAME_TIME_FORMAT = "yyyyMMdd_HH_mm_ss"
    }
}
