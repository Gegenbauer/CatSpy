package me.gegenbauer.catspy.database.log

import me.gegenbauer.catspy.log.LogLevel
import me.gegenbauer.catspy.log.getLevelFromFlag

data class LogEntry(
    val num: Int = 0,
    val time: String = "",
    val pid: String = "",
    val tid: String = "",
    val level: LogLevel = LogLevel.VERBOSE,
    val tag: String = "",
    val message: String = ""
) {
    companion object {
        private const val DATE_INDEX = 0
        private const val TIME_INDEX = 1
        private const val PID_INDEX = 2
        private const val TID_INDEX = 3
        private const val LEVEL_INDEX = 4
        private const val TAG_INDEX = 5
        private const val MESSAGE_INDEX = 6

        fun from(line: String, num: Int): LogEntry {
            val items = splitLineIntoParts(line)
            return runCatching {
                if (items.size > TAG_INDEX) {
                    val time = items[DATE_INDEX] + " " + items[TIME_INDEX]
                    val pid = items[PID_INDEX]
                    val tid = items[TID_INDEX]
                    val level = getLevelFromFlag(items[LEVEL_INDEX])
                    val tag = items[TAG_INDEX]
                    val message = items[MESSAGE_INDEX]
                    LogEntry(num, time, pid, tid, level, tag, message)
                } else {
                    val level = LogLevel.VERBOSE
                    LogEntry(num, level = level, message = line)
                }
            }.getOrElse {
                LogEntry(num, message = line)
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
    }
}