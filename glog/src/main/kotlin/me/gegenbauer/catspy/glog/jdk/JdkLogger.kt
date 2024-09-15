package me.gegenbauer.catspy.glog.jdk

import me.gegenbauer.catspy.glog.GLogger
import me.gegenbauer.catspy.glog.LogFilter
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.io.PrintWriter
import java.io.StringWriter
import java.net.UnknownHostException
import java.util.logging.Logger

class JdkLogger(tag: String): GLogger {
    val loggerImpl: Logger = Logger.getLogger(tag)

    override fun v(tag: String, msg: String) {
        loggerImpl.fine(msg)
    }

    override fun d(tag: String, msg: String) {
        loggerImpl.config(msg)
    }

    override fun i(tag: String, msg: String) {
        loggerImpl.info(msg)
    }

    override fun w(tag: String, msg: String) {
        loggerImpl.warning(msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        loggerImpl.warning(msg + '\n' + tr.stackTraceString)
    }

    override fun e(tag: String, msg: String) {
        loggerImpl.severe(msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        loggerImpl.severe(msg + '\n' + tr.stackTraceString)
    }

    override fun setFilter(filter: LogFilter) {
        // no-op
    }

    private inline val Throwable?.stackTraceString: String
        get() = run {
            if (this == null) {
                return EMPTY_STRING
            }

            // This is to reduce the amount of log spew that apps do in the non-error
            // condition of the network being unavailable.
            var t = this
            while (t != null) {
                if (t is UnknownHostException) {
                    return EMPTY_STRING
                }
                t = t.cause
            }
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            printStackTrace(pw)
            pw.flush()
            return sw.toString()
        }
}