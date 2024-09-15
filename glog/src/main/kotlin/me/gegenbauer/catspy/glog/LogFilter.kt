package me.gegenbauer.catspy.glog

interface LogFilter {

    fun filter(tag: String, message: String, level: LogLevel): Boolean

    companion object {
        val DEFAULT = object : LogFilter {
            override fun filter(tag: String, message: String, level: LogLevel): Boolean {
                return true
            }
        }
    }
}