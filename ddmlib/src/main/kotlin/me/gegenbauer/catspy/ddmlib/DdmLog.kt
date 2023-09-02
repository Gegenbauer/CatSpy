package me.gegenbauer.catspy.ddmlib

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.glog.ILogger

object DdmLog : ILogger by GLog {
    var ddmDebug = false

    override fun v(tag: String, msg: String) {
        if (ddmDebug) {
            GLog.v(tag, msg)
        }
    }

    override fun d(tag: String, msg: String) {
        if (ddmDebug) {
            GLog.d(tag, msg)
        }
    }
}