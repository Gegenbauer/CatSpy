package me.gegenbauer.logviewer.log

import com.github.weisj.darklaf.util.log.LogFormatter
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.UnknownHostException
import java.util.logging.*

class GLogger(private val level: Level) : ILogger {
    private val logger = Logger.getLogger(TAG).apply { useParentHandlers = false }
    private val logFilePath = System.getProperty("user.dir") + File.separator + "glog.txt"
    private val consoleHandler: Handler = ConsoleHandler().apply {
        formatter = GLogFormatter {
            tag
        }
        this.level = this@GLogger.level
    }
    private val fileHandler = FileHandler(logFilePath, 100000, 1, true).apply {
        formatter = LogFormatter()
        this.level = this@GLogger.level
    }

    private var tag: String = ""

    init {
        kotlin.runCatching {
            logger.level = level
            checkLogFile()

            logger.addHandler(consoleHandler)
            logger.addHandler(fileHandler)
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun checkLogFile() {
        val file = File(logFilePath)
        println("[checkLogFile] logFile: $logFilePath")
        if (file.exists().not()) {
            file.createNewFile()
        }
    }

    override fun v(tag: String, msg: String) {
        this.tag = tag
        logger.fine(msg)
    }

    override fun v(tag: String, msg: String, tr: Throwable) {
        this.tag = tag
        logger.fine(msg + tr.stackTraceString)
    }

    override fun d(tag: String, msg: String) {
        this.tag = tag
        logger.config(msg)
    }

    override fun d(tag: String, msg: String, tr: Throwable) {
        this.tag = tag
        logger.config(msg + '\n' + tr.stackTraceString)
    }

    override fun i(tag: String, msg: String) {
        this.tag = tag
        logger.info(msg)
    }

    override fun i(tag: String, msg: String, tr: Throwable) {
        this.tag = tag
        logger.info(msg + '\n' + tr.stackTraceString)
    }

    override fun w(tag: String, msg: String) {
        this.tag = tag
        logger.warning(msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable) {
        this.tag = tag
        logger.warning(msg + '\n' + tr.stackTraceString)
    }

    override fun e(tag: String, msg: String) {
        this.tag = tag
        logger.severe(msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable) {
        this.tag = tag
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

    companion object {
        private const val TAG = "FormalLogger"
    }
}