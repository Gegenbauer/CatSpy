package me.gegenbauer.catspy.databinding

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.glog.ILogger

/**
 * Binding module print too much logs, only print logs when debugging it.
 */
object BindingLog: ILogger by GLog {
    var bindingDebug = false

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
}