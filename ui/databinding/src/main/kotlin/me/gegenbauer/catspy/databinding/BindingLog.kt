package me.gegenbauer.catspy.databinding

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.glog.GLogger

/**
 * Binding module print too much logs, only print logs when debugging it.
 */
object BindingLog: GLogger by GLog {
    var debug = false

    override fun v(tag: String, msg: String) {
        if (debug) {
            GLog.v(tag, msg)
        }
    }

    override fun d(tag: String, msg: String) {
        if (debug) {
            GLog.d(tag, msg)
        }
    }
}