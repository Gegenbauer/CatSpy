package me.gegenbauer.catspy.glog.interceptor

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.glog.GLogger
import me.gegenbauer.catspy.glog.logback.LogbackLogger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.LegacyAbstractLogger

class GSlf4jLoggerAdapter(loggerName: String): LegacyAbstractLogger(), GLogger {
    val gLogger = LogbackLogger(loggerName)

    init {
        this.name = loggerName
    }

    override fun isTraceEnabled(): Boolean {
        return GLog.debug
    }

    override fun isDebugEnabled(): Boolean {
        return GLog.debug
    }

    override fun isInfoEnabled(): Boolean {
        return true
    }

    override fun isWarnEnabled(): Boolean {
        return true
    }

    override fun isErrorEnabled(): Boolean {
        return true
    }

    override fun getFullyQualifiedCallerName(): String {
        return name
    }

    override fun handleNormalizedLoggingCall(
        level: Level?,
        marker: Marker?,
        msg: String?,
        arguments: Array<out Any>?,
        throwable: Throwable?
    ) {
        val finalMessage = formatMessage(msg, arguments)
        when (level) {
            Level.TRACE -> v(name, finalMessage)
            Level.DEBUG -> d(name, finalMessage)
            Level.INFO -> i(name, finalMessage)
            Level.WARN -> w(name, finalMessage, throwable)
            Level.ERROR -> e(name, finalMessage, throwable)
            else -> i(name, finalMessage)
        }
    }

    private fun formatMessage(msg: String?, arguments: Array<out Any>?): String {
        return msg?.let { message ->
            arguments?.fold(message) { acc, argument ->
                acc.replaceFirst("{}", argument.toString())
            } ?: message
        } ?: ""
    }

    override fun v(tag: String, msg: String) {
        gLogger.v(tag, msg)
    }

    override fun d(tag: String, msg: String) {
        gLogger.d(tag, msg)
    }

    override fun i(tag: String, msg: String) {
        gLogger.i(tag, msg)
    }

    override fun w(tag: String, msg: String) {
        gLogger.w(tag, msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        gLogger.w(tag, msg, tr)
    }

    override fun e(tag: String, msg: String) {
        gLogger.e(tag, msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        gLogger.e(tag, msg, tr)
    }
}