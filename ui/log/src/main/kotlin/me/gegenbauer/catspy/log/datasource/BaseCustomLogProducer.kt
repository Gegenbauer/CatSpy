package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.Level
import me.gegenbauer.catspy.log.ui.LogConfiguration
import java.io.File

abstract class BaseCustomLogProducer(
    protected val logConfiguration: LogConfiguration,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer(logConfiguration.logMetaData.parser) {

    override val tempFile: File = File("")

    protected fun generateLogItems(): List<LogItem> {
        val sampleLog = getSampleLogItem()
        val allLevelLogs = getAllLevelLogs(sampleLog)
        return listOf(sampleLog) + allLevelLogs
    }

    protected open fun getSampleLogItem(): LogItem {
        return LogItem(0, logConfiguration.logMetaData.sample, logParser.parse(logConfiguration.logMetaData.sample))
    }

    private fun getAllLevelLogs(sampleLog: LogItem): List<LogItem> {
        val logPartCount = logConfiguration.logMetaData.columns.size
        val levelColumnIndex = logConfiguration.logMetaData.columns.indexOfFirst { it is Column.LevelColumn }
        if (levelColumnIndex == -1) {
            return emptyList()
        }
        val levels = logConfiguration.logMetaData.levels
        val logItems = mutableListOf<LogItem>()
        levels.forEachIndexed { index, level ->
            logItems.add(generateTargetLevelLogItem(sampleLog, level.level, logPartCount, levelColumnIndex, index + 1))
        }
        return logItems
    }

    private fun generateTargetLevelLogItem(sampleLog: LogItem, level: Level, logPartCount: Int, levelColumnIndex: Int, rowIndex: Int): LogItem {
        val logParts = (0 until logPartCount).map { sampleLog.parts[it] }.toMutableList()
        logParts[levelColumnIndex] = level.tag
        return LogItem(rowIndex, logParts.joinToString(" "), logParts)
    }
}