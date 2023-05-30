package me.gegenbauer.catspy.ui.log

import java.util.regex.Pattern

class LogcatRealTimeFilter(
    private val logLevel: LogLevel = LogLevel.VERBOSE,
    private val patternShowLog: Pattern? = null,
    private val patternHideLog: Pattern? = null,
    private val patternShowTag: Pattern? = null,
    private val patternHideTag: Pattern? = null,
    private val patternShowPid: Pattern? = null,
    private val patternHidePid: Pattern? = null,
    private val patternShowTid: Pattern? = null,
    private val patternHideTid: Pattern? = null,
): Filter<LogcatLogItem> {

    override fun filter(item: LogcatLogItem): Boolean {
        if (item.level < logLevel) {
            return false
        }
        val logLine = item.logLine
        val tag = item.tag
        val pid = item.pid
        val tid = item.tid

        return (matchHidePattern(patternHideLog, logLine) &&
                matchShowPattern(patternShowLog, logLine) &&
                matchHidePattern(patternHideTag, tag) &&
                matchShowPattern(patternShowTag, tag) &&
                matchHidePattern(patternHidePid, pid) &&
                matchShowPattern(patternShowPid, pid) &&
                matchHidePattern(patternHideTid, tid) &&
                matchShowPattern(patternShowTid, tid))
    }

    private fun matchShowPattern(pattern: Pattern?, text: String): Boolean {
        return pattern?.matcher(text)?.matches() ?: true
    }

    private fun matchHidePattern(pattern: Pattern?, text: String): Boolean {
        return (pattern?.matcher(text)?.matches() ?: false).not()
    }
}