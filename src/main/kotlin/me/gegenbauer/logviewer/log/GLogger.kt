package me.gegenbauer.logviewer.log

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.UnknownHostException
import java.util.logging.*

class GLogger(private val level: Level, tag: String) : ILogger {
    private val logger = Logger.getLogger(tag).apply {
        useParentHandlers = true
        parent = parentLogger
        level = this@GLogger.level
        consoleHandler.level = this@GLogger.level
        fileHandler.level = this@GLogger.level
    }

    init {
        kotlin.runCatching {
            checkLogFile()
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun setLevel(level: Level) {
        logger.level = level
        consoleHandler.level = level
        fileHandler.level = level
    }

    private fun checkLogFile() {
        val file = File(logFilePath)
        println("[checkLogFile] logFile: $logFilePath")
        if (file.exists().not()) {
            file.createNewFile()
        }
    }

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

    override fun w(tag: String, msg: String, tr: Throwable) {
        logger.warning(msg + '\n' + tr.stackTraceString)
    }

    override fun e(tag: String, msg: String) {
        logger.severe(msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable) {
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
        private val logFilePath = System.getProperty("user.dir") + File.separator + "glog.txt"
        private val consoleHandler: Handler = ConsoleHandler().apply {
            formatter = GLogFormatter()
        }
        private val fileHandler = FileHandler(logFilePath, 100000, 1, true).apply {
            formatter = GLogFormatter()
        }

        private val parentLogger = Logger.getLogger(TAG).apply {
            useParentHandlers = false
            addHandler(consoleHandler)
            addHandler(fileHandler)
        }
    }
}