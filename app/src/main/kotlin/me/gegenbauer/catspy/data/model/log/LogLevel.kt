package me.gegenbauer.catspy.data.model.log

sealed class LogLevel(
    val logName: String,
    val logLevel: Int
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
        return logLevel.compareTo(level.logLevel)
    }
}

private val levelToLogLevelMap = LogLevel::class.nestedClasses.filter { clazz ->
    LogLevel::class.java.isAssignableFrom(clazz.java) && (clazz.java != LogLevel::class.java)
}.map {
    it.objectInstance as LogLevel
}.associateBy {
    it.logLevel
}

private val flagToLevelMap = LogLevel::class.nestedClasses.filter { clazz ->
    LogLevel::class.java.isAssignableFrom(clazz.java) && (clazz.java != LogLevel::class.java)
}.map {
    it.objectInstance as LogLevel
}.associateBy {
    it.flag
}

val nameToLogLevel = LogLevel::class.nestedClasses.filter { clazz ->
    LogLevel::class.java.isAssignableFrom(clazz.java) && (clazz.java != LogLevel::class.java)
}.map {
    it.objectInstance as LogLevel
}.associateBy {
    it.logName
}

fun getLevel(logLevel: Int): LogLevel {
    return levelToLogLevelMap[logLevel] ?: LogLevel.VERBOSE
}

fun getLevelFromFlag(flag: String): LogLevel {
    return flagToLevelMap[flag] ?: LogLevel.VERBOSE
}

fun getLevelFromName(flag: String): LogLevel {
    return nameToLogLevel[flag] ?: LogLevel.VERBOSE
}

val LogLevel.flag: String
    get() = logName.first().toString()