package me.gegenbauer.catspy.glog.jdk

import me.gegenbauer.catspy.glog.LogFormatter
import me.gegenbauer.catspy.glog.LogLevel
import java.util.logging.Formatter
import java.util.logging.LogRecord

open class JdkLogFormatter(private val logFormatter: LogFormatter): Formatter() {
    override fun format(record: LogRecord): String {
        return logFormatter.format(
            me.gegenbauer.catspy.glog.LogRecord(
            record.millis,
            fromJdkLevel(record.level),
            record.loggerName,
            record.message,
            record.thrown
        ))
    }

    private fun fromJdkLevel(level: java.util.logging.Level): LogLevel {
        return when (level) {
            java.util.logging.Level.SEVERE -> LogLevel.ERROR
            java.util.logging.Level.WARNING -> LogLevel.WARN
            java.util.logging.Level.INFO -> LogLevel.INFO
            java.util.logging.Level.CONFIG -> LogLevel.DEBUG
            java.util.logging.Level.FINE -> LogLevel.DEBUG
            java.util.logging.Level.FINER -> LogLevel.DEBUG
            java.util.logging.Level.FINEST -> LogLevel.DEBUG
            else -> LogLevel.VERBOSE
        }
    }
}