package me.gegenbauer.catspy.ui.log

import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.ui.ColorScheme
import java.awt.Color
import java.util.regex.Pattern

internal class LogItem(
    val logLine: String,
    val num: Int = 0,
    val tag: String = "",
    val pid: String = "",
    val tid: String = "",
    val level: LogLevel = LogLevel.VERBOSE,
) {
    companion object {
        private const val PID_INDEX = 2
        private const val TID_INDEX = 3
        private const val LEVEL_INDEX = 4
        private const val TAG_INDEX = 5
        private val splitRegex = Pattern.compile("\\s+")

        fun from(line: String, num: Int): LogItem {
            val items = line.split(splitRegex)
            return runCatching {
                if (items.size > TAG_INDEX) {
                    val pid = items[PID_INDEX]
                    val tid = items[TID_INDEX]
                    val level = getLevelFromFlag(items[LEVEL_INDEX])
                    val tag = items[TAG_INDEX]
                    LogItem(line, num, tag, pid, tid, level)
                } else {
                    val level = if (line.startsWith(STRINGS.ui.app)) {
                        LogLevel.ERROR
                    } else if (Character.isAlphabetic(items[PID_INDEX][0].code)) {
                        getLevelFromFlag(items[PID_INDEX][0].toString()) // 目前不知含义
                    } else {
                        LogLevel.VERBOSE
                    }
                    LogItem(line, num, level = level)
                }
            }.getOrElse {
                LogItem(line, num)
            }
        }

        inline val LogItem.fgColor: Color
            get() = when (level) {
                LogLevel.VERBOSE -> {
                    ColorScheme.logLevelVerbose
                }

                LogLevel.DEBUG -> {
                    ColorScheme.logLevelDebug
                }

                LogLevel.INFO -> {
                    ColorScheme.logLevelInfo
                }

                LogLevel.WARN -> {
                    ColorScheme.logLevelWarning
                }

                LogLevel.ERROR -> {
                    ColorScheme.logLevelError
                }

                LogLevel.FATAL -> {
                    ColorScheme.logLevelFatal
                }

                else -> {
                    ColorScheme.logLevelNone
                }
            }
    }
}