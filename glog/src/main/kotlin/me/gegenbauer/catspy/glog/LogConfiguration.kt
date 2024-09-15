package me.gegenbauer.catspy.glog

interface LogConfiguration<T: GLogger> {
    val logPath: String

    val logName: String

    fun setLevel(level: LogLevel)

    fun setFilter(filter: LogFilter)

    fun configure(logger: T)

    fun flush()
}