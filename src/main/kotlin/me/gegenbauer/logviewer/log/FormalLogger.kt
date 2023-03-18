/**
 * Copyright (C), 2020-2023, FlyingLight
 * FileName: FormalLogger
 * Author: Gegenbauer
 * Date: 2023/1/1 19:50
 * Description:
 * History:
 * <author>    <time>            <version> <desc>
 * FlyingLight 2023/1/1 19:50   0.0.1     *
 */
package me.gegenbauer.logviewer.log

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.UnknownHostException
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

object FormalLogger : ILogger {
    private const val TAG = "FormalLogger"
    private val logger = Logger.getLogger(TAG)
    private val LOG_FILE_PATH = System.getProperty("user.dir") + File.separator + "glog.txt"

    init {
        kotlin.runCatching {
            logger.level = Level.INFO
            checkLogFile()
            val fh = FileHandler(LOG_FILE_PATH, 100000, 1, true)
            fh.formatter = SimpleFormatter()
            logger.addHandler(fh)
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun checkLogFile() {
        val file = File(LOG_FILE_PATH)
        println("[checkLogFile] logFile: $LOG_FILE_PATH")
        if (file.exists().not()) {
            file.createNewFile()
        }
    }
    
    override fun v(tag: String, msg: String) {
        logger.fine("$tag: $msg")
    }

    override fun v(tag: String, msg: String, tr: Throwable) {
        logger.fine("$tag: ${msg + tr.stackTraceString}")
    }

    override fun d(tag: String, msg: String) {
        logger.config(tag + msg)
    }

    override fun d(tag: String, msg: String, tr: Throwable) {
        logger.config(tag + msg + '\n' + tr.stackTraceString)
    }

    override fun i(tag: String, msg: String) {
        logger.info(tag + msg)
    }

    override fun i(tag: String, msg: String, tr: Throwable) {
        logger.info(tag + msg + '\n' + tr.stackTraceString)
    }

    override fun w(tag: String, msg: String) {
        logger.warning(tag + msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable) {
        logger.warning(tag + msg + '\n' + tr.stackTraceString)
    }

    override fun e(tag: String, msg: String) {
        logger.severe(tag + msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable) {
        logger.severe(tag + msg + '\n' + tr.stackTraceString)
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