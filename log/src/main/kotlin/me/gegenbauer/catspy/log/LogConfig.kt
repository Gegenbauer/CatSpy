package me.gegenbauer.catspy.log

import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger

internal class LogConfig(logPath: String, logName: String) {
    private val logFilePath = logPath.appendPath(logName)
    private val consoleHandler: Handler = ConsoleHandler().apply {
        formatter = GLogFormatter()
    }
    private val fileHandler = FileHandler(logFilePath, 100000, 1, true).apply {
        formatter = GLogFormatter()
    }

    private val parentLogger = Logger.getLogger(TAG).apply {
        useParentHandlers = false
        addHandler(consoleHandler)
        addHandler(fileHandler)
    }

    fun setLevel(level: Level) {
        parentLogger.level = level
        consoleHandler.level = level
        fileHandler.level = level
    }

    fun configure(gLogger: GLogger) {
        gLogger.logger.useParentHandlers = true
        gLogger.logger.parent = parentLogger
    }

    companion object {
        private const val TAG = "LogConfig"
    }
}