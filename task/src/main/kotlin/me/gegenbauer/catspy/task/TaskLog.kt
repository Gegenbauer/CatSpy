package me.gegenbauer.catspy.task

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.glog.GLogger

object TaskLog: GLogger by GLog {
    var debug = true

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