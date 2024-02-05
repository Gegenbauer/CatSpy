package me.gegenbauer.catspy.glog.logback

import ch.qos.logback.classic.Logger
import me.gegenbauer.catspy.glog.GLogger

class LogbackLogger(tag: String): GLogger {
    private val loggerImpl = LogbackConfiguration.logContext.getLogger(tag) as Logger

    override fun v(tag: String, msg: String) {
        loggerImpl.trace(msg)
    }

    override fun d(tag: String, msg: String) {
        loggerImpl.debug(msg)
    }

    override fun i(tag: String, msg: String) {
        loggerImpl.info(msg)
    }

    override fun w(tag: String, msg: String) {
        loggerImpl.warn(msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        loggerImpl.warn(msg, tr)
    }

    override fun e(tag: String, msg: String) {
        loggerImpl.error(msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        loggerImpl.error(msg, tr)
    }
}