package me.gegenbauer.catspy.glog

sealed class LogLevel(
    val logName: String,
    val intValue: Int
) {
    object NONE : LogLevel("None", 0)
    object VERBOSE : LogLevel("Verbose", 2)
    object DEBUG : LogLevel("Debug", 3)
    object INFO : LogLevel("Info", 4)
    object WARN : LogLevel("Warn", 5)
    object ERROR : LogLevel("Error", 6)
    object FATAL : LogLevel("Fatal", 7)

    operator fun compareTo(level: LogLevel?): Int {
        level ?: return 1
        return intValue.compareTo(level.intValue)
    }
}

private val logLevels = arrayListOf(
    LogLevel.NONE,
    LogLevel.VERBOSE,
    LogLevel.DEBUG,
    LogLevel.INFO,
    LogLevel.WARN,
    LogLevel.ERROR,
    LogLevel.FATAL
)

private val flagToLevelMap = logLevels.associateBy({ it.flag }, { it })

val nameToLogLevel = logLevels.associateBy({ it.logName }, { it })

fun getLevelFromFlag(flag: String): LogLevel {
    return flagToLevelMap[flag] ?: LogLevel.VERBOSE
}

fun getLevelFromName(flag: String): LogLevel {
    return nameToLogLevel[flag] ?: LogLevel.VERBOSE
}

val LogLevel.flag: String
    get() = logName.first().toString()