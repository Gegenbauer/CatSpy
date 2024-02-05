package me.gegenbauer.catspy.glog

fun interface GLoggerFactory {
    fun getGLogger(tag: String): GLogger
}