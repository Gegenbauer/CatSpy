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
package me.gegenbauer.catspy.log

interface ILogger {
    fun v(tag: String, msg: String)

    fun d(tag: String, msg: String)

    fun i(tag: String, msg: String)

    fun w(tag: String, msg: String)

    fun w(tag: String, msg: String, tr: Throwable)

    fun e(tag: String, msg: String)

    fun e(tag: String, msg: String, tr: Throwable)
}