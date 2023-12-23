package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.file.ensureDir
import me.gegenbauer.catspy.log.model.LogcatItem
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.platform.LOG_DIR
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.task.CommandExecutorImpl
import me.gegenbauer.catspy.task.CommandProcessBuilder
import me.gegenbauer.catspy.task.toCommandArray
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DeviceLogProducer(
    val device: String,
    private val processFetcher: AndroidProcessFetcher,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer() {
    override val tempFile: File = getTempLogFile()

    private val commandExecutor by lazy {
        val logcatCommand = "adb${" -s $device".takeIf { device.isNotBlank() } ?: ""} logcat"
        CommandExecutorImpl(CommandProcessBuilder(logcatCommand.toCommandArray()))
    }

    override fun start(): Flow<Result<LogcatItem>> {
        val logcatOutput = commandExecutor.execute()
        return logcatOutput.map { result ->
            result.map { line ->
                suspender.checkSuspend()
                LogcatItem.from(line, logNum.getAndIncrement(), processFetcher.getPidToPackageMap())
            }
        }.onCompletion {
            moveToState(LogProducer.State.COMPLETE)
        }
    }

    override fun cancel() {
        super.cancel()
        commandExecutor.cancel()
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
}
