package me.gegenbauer.catspy.glog.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.util.FileSize
import me.gegenbauer.catspy.file.appendExtension
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.glog.LogConfiguration
import me.gegenbauer.catspy.glog.LogLevel
import org.slf4j.LoggerFactory

class LogbackConfiguration(
    override val logPath: String,
    override val logName: String
) : LogConfiguration<LogbackLogger> {
    private var logLevel: LogLevel = LogLevel.VERBOSE

    private val encoder = LayoutWrappingEncoder<ILoggingEvent>().apply {
        context = logContext
        layout = LogbackFormatter()
        start()
    }
    private val consoleAppender = ConsoleAppender<ILoggingEvent>().apply {
        context = logContext
        name = CONSOLE_APPENDER_NAME
        encoder = this@LogbackConfiguration.encoder
        start()
    }
    private val fileAppender = RollingFileAppender<ILoggingEvent>().apply appender@{
        context = logContext
        name = FILE_APPENDER_NAME
        encoder = this@LogbackConfiguration.encoder
        file = logPath.appendPath(logName).appendExtension(LOG_FILE_EXTENSION)

        val triggeringPolicy = SizeBasedTriggeringPolicy<ILoggingEvent>().apply {
            context = logContext
            maxFileSize = FileSize(LOG_FILE_MAX_SIZE) // Set the maximum file size before rolling
            start()
        }

        val rollingPolicy = FixedWindowRollingPolicy().apply {
            context = logContext
            setParent(this@appender)
            fileNamePattern = logPath.appendPath(logName).appendExtension("%i").appendExtension(LOG_FILE_EXTENSION)
            minIndex = 1
            maxIndex = LOG_FILE_MAX_COUNT
            start()
        }

        this.triggeringPolicy = triggeringPolicy
        this.rollingPolicy = rollingPolicy

        start()
    }

    private val rootLogger: Logger
        get() = logContext.getLogger(Logger.ROOT_LOGGER_NAME)

    init {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).detachAndStopAllAppenders()
    }

    override fun configure(logger: LogbackLogger) {
        rootLogger.addAppender(consoleAppender)
        rootLogger.addAppender(fileAppender)

        rootLogger.level = logLevel.toLogbackLevel()
    }

    override fun setLevel(level: LogLevel) {
        logLevel = level

        val logbackLevel = level.toLogbackLevel()
        logContext.loggerList.forEach { logger ->
            logger.level = logbackLevel
        }
    }

    private fun LogLevel.toLogbackLevel(): Level = when (this) {
        LogLevel.ERROR, LogLevel.FATAL -> Level.ERROR
        LogLevel.WARN -> Level.WARN
        LogLevel.INFO -> Level.INFO
        LogLevel.DEBUG -> Level.DEBUG
        LogLevel.VERBOSE, LogLevel.NONE -> Level.TRACE
    }

    override fun flush() {
        // no-op
    }

    companion object {
        val logContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val defaultConfig = LogbackConfiguration(System.getProperty("user.dir"), "catspy")

        private const val LOG_FILE_MAX_SIZE = 100 * 1024 * 1024L
        private const val LOG_FILE_MAX_COUNT = 3

        private const val CONSOLE_APPENDER_NAME = "CONSOLE"
        private const val FILE_APPENDER_NAME = "ROLLING_FILE"
        private const val LOG_FILE_EXTENSION = "log"
    }
}