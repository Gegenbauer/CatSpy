package me.gegenbauer.catspy.log

import me.gegenbauer.catspy.glog.LogFilter
import me.gegenbauer.catspy.glog.LogLevel

class CatSpyLogFilter: LogFilter {
    private val ignoredPackages = setOf(
        "io.netty",
        "io.vertx",
        "oshi"
    )

    override fun filter(tag: String, message: String, level: LogLevel): Boolean {
        if (level >= LogLevel.WARN) {
            return true
        }
        if (ignoredPackages.any { tag.startsWith(it) }) {
            return false
        }
        return true
    }

}