/**
 * Copyright (C), 2020-2022, FlyingLight
 * FileName: ILogger
 * Author: Gegenbauer
 * Date: 2022/12/18 21:11
 * Description:
 * History:
 * <author>    <time>            <version> <desc>
 * FlyingLight 2022/12/18 21:11   0.0.1     *
 */
package me.gegenbauer.catspy.glog

interface GLogger {
    fun v(tag: String, msg: String)

    fun d(tag: String, msg: String)

    fun i(tag: String, msg: String)

    fun w(tag: String, msg: String)

    fun w(tag: String, msg: String, tr: Throwable?)

    fun e(tag: String, msg: String)

    fun e(tag: String, msg: String, tr: Throwable?)

    fun flush() {}
}

object EmptyLogger : GLogger {
    override fun v(tag: String, msg: String) {}

    override fun d(tag: String, msg: String) {}

    override fun i(tag: String, msg: String) {}

    override fun w(tag: String, msg: String) {}

    override fun w(tag: String, msg: String, tr: Throwable?) {}

    override fun e(tag: String, msg: String) {}

    override fun e(tag: String, msg: String, tr: Throwable?) {}
}