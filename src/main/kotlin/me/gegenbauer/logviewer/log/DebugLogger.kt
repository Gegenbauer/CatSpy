/**
 * Copyright (C), 2020-2023, FlyingLight
 * FileName: DebugLogger
 * Author: Gegenbauer
 * Date: 2023/1/1 19:47
 * Description:
 * History:
 * <author>    <time>            <version> <desc>
 * FlyingLight 2023/1/1 19:47   0.0.1     *
 */
package me.gegenbauer.logviewer.log

object DebugLogger: ILogger {
    override fun v(tag: String, msg: String) {
        println("$tag: $msg")
    }

    override fun v(tag: String, msg: String, tr: Throwable) {
        println("$tag: $msg")
    }

    override fun d(tag: String, msg: String) {
        println("$tag: $msg")
    }

    override fun d(tag: String, msg: String, tr: Throwable) {
        println("$tag: $msg")
    }

    override fun i(tag: String, msg: String) {
        println("$tag: $msg")
    }

    override fun i(tag: String, msg: String, tr: Throwable) {
        println("$tag: $msg")
    }

    override fun w(tag: String, msg: String) {
        println("$tag: $msg")
    }

    override fun w(tag: String, msg: String, tr: Throwable) {
        println("$tag: $msg")
    }

    override fun e(tag: String, msg: String) {
        println("$tag: $msg")
    }

    override fun e(tag: String, msg: String, tr: Throwable) {
        println("$tag: $msg")
    }
}