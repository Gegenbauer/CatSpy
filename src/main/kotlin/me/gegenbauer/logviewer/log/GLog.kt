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
package me.gegenbauer.logviewer.log

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

object GLog : ILogger {
    var DEBUG = true
        set(value) {
            field = value
            loggers.forEach { it.value.setLevel(level) }
        }
    private val loggers = ConcurrentHashMap<String, GLogger>()
    private val level: Level
        get() = if (DEBUG) {
            Level.ALL
        } else {
            Level.INFO
        }

    override fun v(tag: String, msg: String) {
        getLogger(tag).v("", msg)
    }

    override fun d(tag: String, msg: String) {
        getLogger(tag).d("", msg)
    }

    override fun i(tag: String, msg: String) {
        getLogger(tag).i("", msg)
    }

    override fun w(tag: String, msg: String) {
        getLogger(tag).w("", msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable) {
        getLogger(tag).w("", msg, tr)
    }

    override fun e(tag: String, msg: String) {
        getLogger(tag).e("", msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable) {
        getLogger(tag).e("", msg, tr)
    }

    private fun getLogger(tag: String): GLogger {
        return loggers.getOrPut(tag) {
            GLogger(level, tag)
        }
    }
}