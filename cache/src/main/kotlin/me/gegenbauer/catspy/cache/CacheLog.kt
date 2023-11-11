package me.gegenbauer.catspy.cache

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.glog.ILogger

object CacheLog: ILogger by GLog {
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