package me.gegenbauer.catspy.databinding

import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.log.ILogger

/**
 * Binding module print too much logs, only print logs when debugging it.
 */
object BindingLog: ILogger by GLog {
    private val bindingDebug = true

    override fun v(tag: String, msg: String) {
        if (bindingDebug) {
            GLog.v(tag, msg)
        }
    }

    override fun d(tag: String, msg: String) {
        if (bindingDebug) {
            GLog.d(tag, msg)
        }
    }

    override fun i(tag: String, msg: String) {
        if (bindingDebug) {
            GLog.i(tag, msg)
        }
    }
}