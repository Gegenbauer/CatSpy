package me.gegenbauer.catspy.log

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
        gLogger.logger.useParentHandlers = false
        gLogger.logger.parent = parentLogger
        gLogger.logger.addHandlerIfNotPresent(fileHandler)
        gLogger.logger.addHandlerIfNotPresent(consoleHandler)
    }

    private fun Logger.addHandlerIfNotPresent(handler: Handler) {
        if (handlers.contains(handler)) {
            return
        }
        addHandler(handler)
    }

    companion object {
        private const val TAG = "LogConfig"
    }
}