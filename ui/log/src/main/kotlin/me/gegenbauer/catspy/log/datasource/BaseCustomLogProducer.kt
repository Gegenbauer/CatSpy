package me.gegenbauer.catspy.log.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.Level
import me.gegenbauer.catspy.log.ui.LogConfiguration
import java.io.File

abstract class BaseCustomLogProducer(
    protected val logConfiguration: LogConfiguration,
    override val dispatcher: CoroutineDispatcher = Dispatchers.GIO
) : BaseLogProducer() {

    override val tempFile: File = File(EMPTY_STRING)

    protected val logParser = logConfiguration.logMetaData.parser

    protected fun generateLogItems(): List<LogItem> {
        val sampleLog = getSampleLogItem()
        val allLevelLogs = getAllLevelLogs(sampleLog)
        return allLevelLogs
    }

    protected open fun getSampleLogItem(): LogItem {
        return LogItem(0, logParser.parse(logConfiguration.logMetaData.sample))
    }

    private fun getAllLevelLogs(sampleLog: LogItem): List<LogItem> {
        val logPartCount = logConfiguration.logMetaData.columns.size
        val levelColumnIndex = logConfiguration.logMetaData.columns.indexOfFirst { it is Column.LevelColumn }
        if (levelColumnIndex == -1) {
            return listOf(sampleLog)
        }
        val levels = logConfiguration.logMetaData.levels
        val logItems = mutableListOf<LogItem>()
        levels.forEachIndexed { index, level ->
            logItems.add(generateTargetLevelLogItem(sampleLog, level.level, logPartCount, levelColumnIndex, index + 1))
        }
        return logItems
    }

    private fun generateTargetLevelLogItem(
        sampleLog: LogItem,
        level: Level,
        logPartCount: Int,
        levelColumnIndex: Int,
        rowIndex: Int
    ): LogItem {
        val logParts = (0 until logPartCount).map { sampleLog.getPart(it) }.toMutableList()
        logParts[levelColumnIndex] = level.keyword
        return LogItem(rowIndex, logParts)
    }
}