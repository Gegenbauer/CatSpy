package me.gegenbauer.catspy.glog

data class LogRecord(
    val millis: Long,
    val level: LogLevel,
    val loggerName: String,
    val message: String,
    val thrown: Throwable? = null
)