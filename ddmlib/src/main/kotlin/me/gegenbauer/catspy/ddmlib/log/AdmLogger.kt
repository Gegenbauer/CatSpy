package me.gegenbauer.catspy.ddmlib.log

import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.LegacyAbstractLogger


class AdmLogger(loggerName: String) : LegacyAbstractLogger() {

    init {
        name = loggerName
    }

    override fun isTraceEnabled(): Boolean {
        return DdmLog.debug
    }

    override fun isDebugEnabled(): Boolean {
        return DdmLog.debug
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
            Level.TRACE -> DdmLog.v(name, finalMessage)
            Level.DEBUG -> DdmLog.d(name, finalMessage)
            Level.INFO -> DdmLog.i(name, finalMessage)
            Level.WARN -> DdmLog.w(name, finalMessage, throwable)
            Level.ERROR -> DdmLog.e(name, finalMessage, throwable)
            else -> DdmLog.i(name, finalMessage)
        }
    }

    private fun formatMessage(msg: String?, arguments: Array<out Any>?): String {
        return if (msg == null) {
            ""
        } else {
            arguments ?: return msg
            var finalMessage = msg!!
            for (argument in arguments) {
                val placeholder = "{}"
                val argumentString = argument.toString()
                finalMessage = finalMessage.replaceFirst(placeholder, argumentString)
            }
            finalMessage
        }
    }
}