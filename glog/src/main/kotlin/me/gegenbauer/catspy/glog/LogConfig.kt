package me.gegenbauer.catspy.glog

import me.gegenbauer.catspy.file.appendPath
import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger

internal val defaultConfig = LogConfig(System.getProperty("user.dir"), "catspy.log")

internal class LogConfig(logPath: String, logName: String) {
    private val logFilePath = logPath.appendPath(logName)
    private val consoleHandler: Handler = ConsoleHandler().apply {
        formatter = GLogFormatter()
    }
    private val fileHandler = FileHandler(logFilePath, LOG_FILE_MAX_SIZE, LOG_FILE_MAX_COUNT, true).apply {
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
        gLogger.logger.useParentHandlers = false
        gLogger.logger.parent = parentLogger
        gLogger.logger.addHandlerIfNotPresent(fileHandler)
        gLogger.logger.addHandlerIfNotPresent(consoleHandler)
    }

    fun flush() {
        fileHandler.flush()
    }

    private fun Logger.addHandlerIfNotPresent(handler: Handler) {
        if (handlers.contains(handler)) {
            return
        }
        addHandler(handler)
    }

    companion object {
        private const val TAG = "LogConfig"
        private const val LOG_FILE_MAX_SIZE = 100 * 1024 * 1024L
        private const val LOG_FILE_MAX_COUNT = 3
    }
}