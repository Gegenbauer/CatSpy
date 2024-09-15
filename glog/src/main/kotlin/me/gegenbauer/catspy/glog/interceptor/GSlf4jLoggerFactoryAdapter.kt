package me.gegenbauer.catspy.glog.interceptor

import me.gegenbauer.catspy.glog.GLogger
import me.gegenbauer.catspy.glog.GLoggerFactory
import me.gegenbauer.catspy.glog.LogConfiguration
import me.gegenbauer.catspy.glog.LogFilter
import me.gegenbauer.catspy.glog.LogFilterable
import me.gegenbauer.catspy.glog.logback.LogbackConfiguration
import me.gegenbauer.catspy.glog.logback.LogbackLogger
import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap

object GSlf4jLoggerFactoryAdapter : ILoggerFactory, GLoggerFactory, LogFilterable {
    var logConfig: LogConfiguration<LogbackLogger> = LogbackConfiguration.defaultConfig

    private val loggers = ConcurrentHashMap<String, GLogger>()
    private var logFilter: LogFilter = LogFilter.DEFAULT

    override fun setFilter(filter: LogFilter) {
        logConfig.setFilter(filter)
        loggers.values.forEach { it.setFilter(filter) }
        logFilter = filter
    }

    /**
     * used to customize third-party libraries which use slf4j
     */
    override fun getLogger(tag: String): Logger {
        return if (loggers.containsKey(tag)) {
            loggers[tag] as Logger
        } else {
            val logger = GSlf4jLoggerAdapter(tag)
            logConfig.configure(logger.gLogger)
            loggers[tag] = logger
            logger
        }
    }

    /**
     * customize the logger for self-use
     */
    override fun getGLogger(tag: String): GLogger {
        return if (loggers.containsKey(tag)) {
            loggers[tag] as GLogger
        } else {
            val logger = GSlf4jLoggerAdapter(tag)
            logConfig.configure(logger.gLogger)
            logger.setFilter(logFilter)
            loggers[tag] = logger
            logger
        }
    }
}