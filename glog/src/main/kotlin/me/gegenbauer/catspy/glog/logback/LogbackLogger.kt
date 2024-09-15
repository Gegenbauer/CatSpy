package me.gegenbauer.catspy.glog.logback

import ch.qos.logback.classic.Logger
import me.gegenbauer.catspy.glog.GLogger
import me.gegenbauer.catspy.glog.LogFilter
import me.gegenbauer.catspy.glog.LogLevel

class LogbackLogger(tag: String): GLogger {
    private val loggerImpl = LogbackConfiguration.logContext.getLogger(tag) as Logger
    private var logFilter: LogFilter = LogFilter.DEFAULT

    override fun v(tag: String, msg: String) {
        if (logFilter.filter(tag, msg, LogLevel.VERBOSE)) {
            loggerImpl.trace(msg)
        }
    }

    override fun d(tag: String, msg: String) {
        if (logFilter.filter(tag, msg, LogLevel.DEBUG)) {
            loggerImpl.debug(msg)
        }
    }

    override fun i(tag: String, msg: String) {
        if (logFilter.filter(tag, msg, LogLevel.INFO)) {
            loggerImpl.info(msg)
        }
    }

    override fun w(tag: String, msg: String) {
        if (logFilter.filter(tag, msg, LogLevel.WARN)) {
            loggerImpl.warn(msg)
        }
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        if (logFilter.filter(tag, msg, LogLevel.WARN)) {
            loggerImpl.warn(msg, tr)
        }
    }

    override fun e(tag: String, msg: String) {
        if (logFilter.filter(tag, msg, LogLevel.ERROR)) {
            loggerImpl.error(msg)
        }
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        if (logFilter.filter(tag, msg, LogLevel.ERROR)) {
            loggerImpl.error(msg, tr)
        }
    }

    override fun setFilter(filter: LogFilter) {
        logFilter = filter
    }
}