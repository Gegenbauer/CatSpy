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

object GLog: ILogger {
    val DEBUG = true
    private const val TAG = "FLLogger"

    private val logger = if (DEBUG) {
        DebugLogger
    } else {
        FormalLogger
    }

    override fun v(tag: String, msg: String) {
        logger.v(tag, msg)
    }

    override fun v(tag: String, msg: String, tr: Throwable) {
        logger.v(tag, msg, tr)
    }

    override fun d(tag: String, msg: String) {
        logger.d(tag, msg)
    }

    override fun d(tag: String, msg: String, tr: Throwable) {
        logger.d(tag, msg, tr)
    }

    override fun i(tag: String, msg: String) {
        logger.i(tag, msg)
    }

    override fun i(tag: String, msg: String, tr: Throwable) {
        logger.i(tag, msg, tr)
    }

    override fun w(tag: String, msg: String) {
        logger.w(tag, msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable) {
        logger.w(tag, msg, tr)
    }

    override fun e(tag: String, msg: String) {
        logger.e(tag, msg)
    }

    override fun e(tag: String, msg: String, tr: Throwable) {
        logger.e(tag, msg, tr)
    }
}