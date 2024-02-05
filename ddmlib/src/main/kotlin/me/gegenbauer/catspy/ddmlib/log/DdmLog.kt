package me.gegenbauer.catspy.ddmlib.log

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.glog.GLogger

object DdmLog : GLogger by GLog {
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