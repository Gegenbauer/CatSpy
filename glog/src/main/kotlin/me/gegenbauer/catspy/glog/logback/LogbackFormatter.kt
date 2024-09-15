package me.gegenbauer.catspy.glog.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.LayoutBase
import me.gegenbauer.catspy.glog.LogFormatter
import me.gegenbauer.catspy.glog.LogLevel
import me.gegenbauer.catspy.glog.LogRecord

class LogbackFormatter(private val formatter: LogFormatter): LayoutBase<ILoggingEvent>() {
    override fun doLayout(event: ILoggingEvent): String {
        val logRecord = LogRecord(
            event.timeStamp,
            event.level.getLogLevel(),
            event.loggerName,
            event.formattedMessage,
            (event.throwableProxy as? ThrowableProxy)?.throwable
        )
        return formatter.format(logRecord)
    }

    private fun Level.getLogLevel(): LogLevel {
        return when (this.toInt()) {
            Level.TRACE_INT -> LogLevel.VERBOSE
            Level.DEBUG_INT -> LogLevel.DEBUG
            Level.INFO_INT -> LogLevel.INFO
            Level.WARN_INT -> LogLevel.WARN
            Level.ERROR_INT -> LogLevel.ERROR
            else -> LogLevel.VERBOSE
        }
    }
}