package me.gegenbauer.catspy.task

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.glog.ILogger

object TaskLog: ILogger by GLog {
    var taskDebug = true

    override fun v(tag: String, msg: String) {
        if (taskDebug) {
            GLog.v(tag, msg)
        }
    }

    override fun d(tag: String, msg: String) {
        if (taskDebug) {
            GLog.d(tag, msg)
        }
    }

    override fun i(tag: String, msg: String) {
        if (taskDebug) {
            GLog.i(tag, msg)
        }
    }
}