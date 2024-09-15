package me.gegenbauer.catspy.glog.jdk

import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.glog.ColoredLogFormatter
import me.gegenbauer.catspy.glog.LogConfiguration
import me.gegenbauer.catspy.glog.LogFilter
import me.gegenbauer.catspy.glog.LogLevel
import me.gegenbauer.catspy.glog.PlainLogFormatter
import java.util.logging.*

internal class JdkLogConfiguration(
    override val logPath: String,
    override val logName: String
) : LogConfiguration<JdkLogger> {
    private val logFilePath = logPath.appendPath(logName)
    private val consoleHandler: Handler = ConsoleHandler().apply {
        formatter = JdkLogFormatter(ColoredLogFormatter)
    }
    private val fileHandler = FileHandler(logFilePath, LOG_FILE_MAX_SIZE, LOG_FILE_MAX_COUNT, true).apply {
        formatter = JdkLogFormatter(PlainLogFormatter)
    }

    private val parentLogger = Logger.getLogger(TAG).apply {
        useParentHandlers = false
        addHandler(consoleHandler)
        addHandler(fileHandler)
    }

    override fun setLevel(level: LogLevel) {
        val jdkLogLevel = when (level) {
            LogLevel.ERROR, LogLevel.FATAL -> Level.SEVERE
            LogLevel.WARN -> Level.WARNING
            LogLevel.INFO -> Level.INFO
            LogLevel.DEBUG -> Level.CONFIG
            LogLevel.VERBOSE, LogLevel.NONE -> Level.ALL
        }

        parentLogger.level = jdkLogLevel
        consoleHandler.level = jdkLogLevel
        fileHandler.level = jdkLogLevel
    }

    override fun setFilter(filter: LogFilter) {
        // no-op
    }

    override fun configure(logger: JdkLogger) {
        logger.loggerImpl.apply {
            useParentHandlers = false
            parent = parentLogger
            addHandlerIfNotPresent(fileHandler)
            addHandlerIfNotPresent(consoleHandler)
        }
    }

    override fun flush() {
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

        internal val defaultConfig = JdkLogConfiguration(System.getProperty("user.dir"), "catspy.log")
    }
}