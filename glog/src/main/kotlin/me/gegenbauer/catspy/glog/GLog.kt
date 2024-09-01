/**
 * Copyright (C), 2020-2022, FlyingLight
 * FileName: GLog
 * Author: Gegenbauer
 * Date: 2022/12/18 21:07
 * Description:
 * History:
 * <author>    <time>            <version> <desc>
 * FlyingLight 2022/12/18 21:07   0.0.1     *
 */
package me.gegenbauer.catspy.glog

import me.gegenbauer.catspy.glog.interceptor.GSlf4jLoggerFactoryAdapter
import me.gegenbauer.catspy.glog.logback.LogbackConfiguration
import me.gegenbauer.catspy.java.ext.EMPTY_STRING

object GLog : GLogger {
    var debug = false
        set(value) {
            field = value
            GSlf4jLoggerFactoryAdapter.logConfig.setLevel(getLevel(value))
        }

    private val loggerFactory = GSlf4jLoggerFactoryAdapter

    init {
        debug = true
    }

    private fun getLevel(debug: Boolean): LogLevel = if (debug) {
        LogLevel.VERBOSE
    } else {
        LogLevel.INFO
    }

    fun init(path: String, name: String) {
        GSlf4jLoggerFactoryAdapter.logConfig = LogbackConfiguration(path, name)
    }

    override fun v(tag: String, msg: String) {
        getLogger(tag).v(EMPTY_STRING, msg)
    }

    override fun d(tag: String, msg: String) {
        getLogger(tag).d(EMPTY_STRING, msg)
    }

    override fun i(tag: String, msg: String) {
        getLogger(tag).i(EMPTY_STRING, msg)
    }

    override fun w(tag: String, msg: String) {
        getLogger(tag).w(EMPTY_STRING, msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        getLogger(tag).w(EMPTY_STRING, msg, tr)
    }

    override fun e(tag: String, msg: String) {
        getLogger(tag).e(EMPTY_STRING, msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        getLogger(tag).e(EMPTY_STRING, msg, tr)
    }

    override fun flush() {
        GSlf4jLoggerFactoryAdapter.logConfig.flush()
    }

    private fun getLogger(tag: String): GLogger {
        return loggerFactory.getGLogger(tag)
    }
}