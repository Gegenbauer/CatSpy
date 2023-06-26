package me.gegenbauer.catspy.log

import java.io.PrintWriter
import java.io.StringWriter
import java.net.UnknownHostException
import java.util.logging.*

class GLogger(tag: String) : ILogger {
    val logger: Logger = Logger.getLogger(tag)

    override fun v(tag: String, msg: String) {
        logger.fine(msg)
    }

    override fun d(tag: String, msg: String) {
        logger.config(msg)
    }

    override fun i(tag: String, msg: String) {
        logger.info(msg)
    }

    override fun w(tag: String, msg: String) {
        logger.warning(msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        logger.warning(msg + '\n' + tr.stackTraceString)
    }

    override fun e(tag: String, msg: String) {
        logger.severe(msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        logger.severe(msg + '\n' + tr.stackTraceString)
    }

    private inline val Throwable?.stackTraceString: String
        get() = run {
            if (this == null) {
                return ""
            }

            // This is to reduce the amount of log spew that apps do in the non-error
            // condition of the network being unavailable.
            var t = this
            while (t != null) {
                if (t is UnknownHostException) {
                    return ""
                }
                t = t.cause
            }
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            this.printStackTrace(pw)
            pw.flush()
            return sw.toString()
        }
}