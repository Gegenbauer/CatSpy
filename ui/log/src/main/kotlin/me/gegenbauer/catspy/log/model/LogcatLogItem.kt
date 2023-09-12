package me.gegenbauer.catspy.log.model

import me.gegenbauer.catspy.configuration.LogColorScheme
import me.gegenbauer.catspy.log.LogLevel
import me.gegenbauer.catspy.log.getLevelFromFlag
import me.gegenbauer.catspy.strings.Configuration
import java.awt.Color

data class LogcatLogItem(
    val logLine: String,
    val num: Int = 0,
    val time: String = "",
    val pid: String = "",
    val tid: String = "",
    val level: LogLevel = LogLevel.VERBOSE,
    val tag: String = "",
    val message: String = ""
): LogItem {
    private var hidden = false

    override fun setHidden(hidden: Boolean) {
        this.hidden = hidden
    }

    override fun isHidden(): Boolean {
        return hidden
    }

    override fun isFromFile(): Boolean {
        return false
    }

    override fun getDisplayText(): String {
        return logLine
    }

    companion object {
        private const val DATE_INDEX = 0
        private const val TIME_INDEX = 1
        private const val PID_INDEX = 2
        private const val TID_INDEX = 3
        private const val LEVEL_INDEX = 4
        private const val TAG_INDEX = 5
        private const val MESSAGE_INDEX = 6

        fun LogcatLogItem.isShow(): Boolean {
            return !hidden
        }

        fun from(line: String, num: Int): LogcatLogItem {
            val items = splitLineIntoParts(line)
            return runCatching {
                if (items.size > TAG_INDEX) {
                    val time = items[DATE_INDEX] + " " + items[TIME_INDEX]
                    val pid = items[PID_INDEX]
                    val tid = items[TID_INDEX]
                    val level = getLevelFromFlag(items[LEVEL_INDEX])
                    val tag = items[TAG_INDEX]
                    val message = items[MESSAGE_INDEX]
                    LogcatLogItem(line, num, time, pid, tid, level, tag, message)
                } else {
                    val level = if (line.startsWith(Configuration.APP_NAME)) {
                        LogLevel.ERROR
                    } else if (Character.isAlphabetic(items[PID_INDEX][0].code)) {
                        getLevelFromFlag(items[PID_INDEX][0].toString()) // 目前不知含义
                    } else {
                        LogLevel.VERBOSE
                    }
                    LogcatLogItem(line, num, level = level, message = line)
                }
            }.getOrElse {
                LogcatLogItem(line, num)
            }
        }

        private fun splitLineIntoParts(line: String): List<String> {
            val words = line.split(Regex("\\s+"))

            val firstFiveWords = mutableListOf<String>()
            val stringBuilder = StringBuilder()

            for ((index, word) in words.withIndex()) {
                if (index < 6) {
                    firstFiveWords.add(word)
                } else {
                    stringBuilder.append(word).append(" ")
                }
            }
            return firstFiveWords + stringBuilder.toString()
        }

        inline val LogcatLogItem.fgColor: Color
            get() = when (level) {
                LogLevel.VERBOSE -> {
                    LogColorScheme.logLevelVerbose
                }

                LogLevel.DEBUG -> {
                    LogColorScheme.logLevelDebug
                }

                LogLevel.INFO -> {
                    LogColorScheme.logLevelInfo
                }

                LogLevel.WARN -> {
                    LogColorScheme.logLevelWarning
                }

                LogLevel.ERROR -> {
                    LogColorScheme.logLevelError
                }

                LogLevel.FATAL -> {
                    LogColorScheme.logLevelFatal
                }

                else -> {
                    LogColorScheme.logLevelNone
                }
            }
    }
}