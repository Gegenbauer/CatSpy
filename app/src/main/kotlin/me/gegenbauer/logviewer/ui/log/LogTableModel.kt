package me.gegenbauer.logviewer.ui.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.file.Log
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.manager.BookmarkManager
import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.command.LogCmdManager
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.resource.strings.app
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.combobox.FilterComboBox
import java.awt.Color
import java.io.*
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import kotlin.collections.ArrayList


data class LogTableModelEvent(val source: LogTableModel, val dataChange: Int, val removedCount: Int) {
    companion object {
        const val EVENT_ADDED = 0
        const val EVENT_REMOVED = 1
        const val EVENT_FILTERED = 2
        const val EVENT_CHANGED = 3
        const val EVENT_CLEARED = 4

        const val FLAG_FIRST_REMOVED = 1
    }
}

fun interface LogTableModelListener {
    fun tableChanged(event: LogTableModelEvent)
}

class LogTableModel(private val mainUI: MainUI, private var baseModel: LogTableModel?) : AbstractTableModel() {
    private var patternSearchLog: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)
    private var matcherSearchLog: Matcher = patternSearchLog.matcher("")
    private var normalSearchLogSplit: List<String>? = null
    private var tableColor: ColorManager.TableColor
    private val columnNames = arrayOf("line", "log")
    private var logItems: MutableList<LogItem> = mutableListOf()

    private val eventListeners = ArrayList<LogTableModelListener>()
    private val filteredFGMap = mutableMapOf<String, String>()
    private val filteredBGMap = mutableMapOf<String, String>()

    private var isFilterUpdated = true

    var selectionChanged = false

    var filterLevel: LogLevel = LogLevel.NONE
        set(value) {
            if (field != value) {
                isFilterUpdated = true
            }
            field = value
        }

    var filterLog: String = ""
        set(value) {
            if (field != value) {
                isFilterUpdated = true
                field = value
            }
            mainUI.showLogCombo.errorMsg = ""
            val patterns = parsePattern(value, true)
            filterShowLog = patterns[0]
            filterHideLog = patterns[1]

            if (baseModel != null) {
                baseModel!!.filterLog = value
            }
        }

    private var filterShowLog: String = ""
        set(value) {
            field = value
            patternShowLog = compilePattern(value, patternCase, patternShowLog, mainUI.showLogCombo)
        }

    private var filterHideLog: String = ""
        set(value) {
            field = value
            patternHideLog = compilePattern(value, patternCase, patternHideLog, mainUI.showLogCombo)
        }

    var filterHighlightLog: String = ""
        set(value) {
            val patterns = parsePattern(value, false)
            if (field != patterns[0]) {
                isFilterUpdated = true
                field = patterns[0]
            }
        }

    private fun updateFilterSearchLog(field: String) {
        var normalSearchLog = ""
        val searchLogSplit = field.split("|")
        regexSearchLog = ""

        for (logUnit in searchLogSplit) {
            val hasIt: Boolean = logUnit.chars().anyMatch { c -> "\\.[]{}()*+?^$|".indexOf(c.toChar()) >= 0 }
            if (hasIt) {
                if (regexSearchLog.isEmpty()) {
                    regexSearchLog = logUnit
                } else {
                    regexSearchLog += "|$logUnit"
                }
            } else {
                if (normalSearchLog.isEmpty()) {
                    normalSearchLog = logUnit
                } else {
                    normalSearchLog += "|$logUnit"
                }

                if (searchPatternCase == Pattern.CASE_INSENSITIVE) {
                    normalSearchLog = normalSearchLog.uppercase()
                }
            }
        }

        mainUI.searchPanel.searchCombo.errorMsg = ""
        patternSearchLog =
            compilePattern(regexSearchLog, searchPatternCase, patternSearchLog, mainUI.searchPanel.searchCombo)
        matcherSearchLog = patternSearchLog.matcher("")

        normalSearchLogSplit = normalSearchLog.split("|")
    }

    var filterSearchLog: String = ""
        set(value) {
            val patterns = parsePattern(value, false)
            if (field != patterns[0]) {
                isFilterUpdated = true
                field = patterns[0]

                updateFilterSearchLog(field)
            }

            if (baseModel != null) {
                baseModel!!.filterSearchLog = value
            }
        }

    var filterTag: String = ""
        set(value) {
            if (field != value) {
                isFilterUpdated = true
                field = value
            }
            mainUI.showTagCombo.errorMsg = ""
            val patterns = parsePattern(value, false)
            filterShowTag = patterns[0]
            filterHideTag = patterns[1]
        }

    private var filterShowTag: String = ""
        set(value) {
            field = value
            patternShowTag = compilePattern(value, patternCase, patternShowTag, mainUI.showTagCombo)
        }
    private var filterHideTag: String = ""
        set(value) {
            field = value
            patternHideTag = compilePattern(value, patternCase, patternHideTag, mainUI.showTagCombo)
        }

    var filterPid: String = ""
        set(value) {
            if (field != value) {
                isFilterUpdated = true
                field = value
            }
            mainUI.showPidCombo.errorMsg = ""
            val patterns = parsePattern(value, false)
            filterShowPid = patterns[0]
            filterHidePid = patterns[1]
        }

    private var filterShowPid: String = ""
        set(value) {
            field = value
            patternShowPid = compilePattern(value, patternCase, patternShowPid, mainUI.showPidCombo)
        }

    private var filterHidePid: String = ""
        set(value) {
            field = value
            patternHidePid = compilePattern(value, patternCase, patternHidePid, mainUI.showPidCombo)
        }

    var filterTid: String = ""
        set(value) {
            if (field != value) {
                isFilterUpdated = true
                field = value
            }
            mainUI.showTidCombo.errorMsg = ""
            val patterns = parsePattern(value, false)
            patterns[0].let { filterShowTid = it }
            patterns[1].let { filterHideTid = it }
        }

    private var filterShowTid: String = ""
        set(value) {
            field = value
            patternShowTid = compilePattern(value, patternCase, patternShowTid, mainUI.showTidCombo)
        }
    private var filterHideTid: String = ""
        set(value) {
            field = value
            patternHideTid = compilePattern(value, patternCase, patternHideTid, mainUI.showTidCombo)
        }

    private var patternCase = Pattern.CASE_INSENSITIVE
    var matchCase: Boolean = false
        set(value) {
            if (field != value) {
                patternCase = if (!value) {
                    Pattern.CASE_INSENSITIVE
                } else {
                    0
                }

                mainUI.showLogCombo.errorMsg = ""
                patternShowLog = compilePattern(filterShowLog, patternCase, patternShowLog, mainUI.showLogCombo)
                patternHideLog = compilePattern(filterHideLog, patternCase, patternHideLog, mainUI.showLogCombo)
                mainUI.showTagCombo.errorMsg = ""
                patternShowTag = compilePattern(filterShowTag, patternCase, patternShowTag, mainUI.showTagCombo)
                patternHideTag = compilePattern(filterHideTag, patternCase, patternHideTag, mainUI.showTagCombo)
                mainUI.showPidCombo.errorMsg = ""
                patternShowPid = compilePattern(filterShowPid, patternCase, patternShowPid, mainUI.showPidCombo)
                patternHidePid = compilePattern(filterHidePid, patternCase, patternHidePid, mainUI.showPidCombo)
                mainUI.showTidCombo.errorMsg = ""
                patternShowTid = compilePattern(filterShowTid, patternCase, patternShowTid, mainUI.showTidCombo)
                patternHideTid = compilePattern(filterHideTid, patternCase, patternHideTid, mainUI.showTidCombo)

                isFilterUpdated = true

                field = value
            }
        }

    private var regexSearchLog = ""
    private var searchPatternCase = Pattern.CASE_INSENSITIVE
    var searchMatchCase: Boolean = false
        set(value) {
            if (field != value) {
                searchPatternCase = if (!value) {
                    Pattern.CASE_INSENSITIVE
                } else {
                    0
                }

                isFilterUpdated = true

                field = value

                updateFilterSearchLog(filterSearchLog)

                if (baseModel != null) {
                    baseModel!!.searchMatchCase = value
                }
            }
        }

    var goToLast = true

    var boldTag = false
    var boldPid = false
    var boldTid = false
    var bookmarkMode = false
        set(value) {
            field = value
            if (value) {
                fullMode = false
            }
            isFilterUpdated = true
        }

    var fullMode = false
        set(value) {
            field = value
            if (value) {
                bookmarkMode = false
            }
            isFilterUpdated = true
        }

    var scrollback = 0
        set(value) {
            field = value
            clear()
        }

    var scrollBackSplitFile = false

    var scrollBackKeep = false

    private var patternShowLog: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)
    private var patternHideLog: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)
    private var patternShowTag: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)
    private var patternHideTag: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)
    private var patternShowPid: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)
    private var patternHidePid: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)
    private var patternShowTid: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)
    private var patternHideTid: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)

    private var patternError: Pattern = Pattern.compile("\\bERROR\\b", Pattern.CASE_INSENSITIVE)
    private var patternWarning: Pattern = Pattern.compile("\\bWARNING\\b", Pattern.CASE_INSENSITIVE)
    private var patternInfo: Pattern = Pattern.compile("\\bINFO\\b", Pattern.CASE_INSENSITIVE)
    private var patternDebug: Pattern = Pattern.compile("\\bDEBUG\\b", Pattern.CASE_INSENSITIVE)

    init {
        this.baseModel = baseModel
        loadItems(false)

        tableColor = if (isFullDataModel()) {
            ColorManager.fullTableColor
        } else {
            ColorManager.filterTableColor
        }

        val colorEventListener = object : ColorManager.ColorEventListener {
            override fun colorChanged(event: ColorManager.ColorEvent) {
                parsePattern(filterLog, true) // update color
                isFilterUpdated = true
            }
        }

        ColorManager.addColorEventListener(colorEventListener)
    }

    fun isFullDataModel(): Boolean {
        if (baseModel == null) {
            return true
        }

        return false
    }

    private fun parsePattern(pattern: String, isUpdateColor: Boolean): Array<String> {
        val patterns: Array<String> = Array(2) { "" }

        val strs = pattern.split("|")
        var prevPatternIdx = -1
        if (isUpdateColor) {
            filteredFGMap.clear()
            filteredBGMap.clear()
        }

        for (item in strs) {
            if (prevPatternIdx != -1) {
                patterns[prevPatternIdx] += "|"
                patterns[prevPatternIdx] += item
                if (item.substring(item.length - 1) != "\\") {
                    prevPatternIdx = -1
                }
                continue
            }

            if (item.isNotEmpty()) {
                if (item[0] != '-') {
                    if (patterns[0].isNotEmpty()) {
                        patterns[0] += "|"
                    }

                    if (2 < item.length && item[0] == '#' && item[1].isDigit()) {
                        val key = item.substring(2)
                        patterns[0] += key
                        if (isUpdateColor) {
                            filteredFGMap[key.uppercase()] = tableColor.strFilteredFGs[item[1].digitToInt()]
                            filteredBGMap[key.uppercase()] = tableColor.strFilteredBGs[item[1].digitToInt()]
                        }
                    } else {
                        patterns[0] += item
                    }

                    if (item.substring(item.length - 1) == "\\") {
                        prevPatternIdx = 0
                    }
                } else {
                    if (patterns[1].isNotEmpty()) {
                        patterns[1] += "|"
                    }

                    if (3 < item.length && item[1] == '#' && item[2].isDigit()) {
                        patterns[1] += item.substring(3)
                    } else {
                        patterns[1] += item.substring(1)
                    }

                    if (item.substring(item.length - 1) == "\\") {
                        prevPatternIdx = 1
                    }
                }
            }
        }

        return patterns
    }

    private fun compilePattern(regex: String, flags: Int, prevPattern: Pattern, comboBox: FilterComboBox?): Pattern {
        var pattern = prevPattern
        try {
            pattern = Pattern.compile(regex, flags)
        } catch (ex: java.util.regex.PatternSyntaxException) {
            ex.printStackTrace()
            comboBox?.errorMsg = ex.message.toString()
        }

        return pattern
    }

    private var filteredItemsThread: Thread? = null
    fun loadItems(isAppend: Boolean) {
        if (baseModel == null) {
            AppScope.launch(Dispatchers.UI) {
                loadFile(isAppend)
            }
        } else {
            isFilterUpdated = true

            if (filteredItemsThread == null) {
                filteredItemsThread = Thread {
                    run {
                        while (true) {
                            try {
                                if (isFilterUpdated) {
                                    mainUI.markLine()
                                    makeFilteredItems(true)
                                }
                                Thread.sleep(100)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                filteredItemsThread?.start()
            }
        }
    }

    fun clearItems() {
        GLog.d(TAG, "isEventDispatchThread = ${SwingUtilities.isEventDispatchThread()}")
        fireLogTableDataChanged(LogTableModelEvent(this, LogTableModelEvent.EVENT_CLEARED, 0))
        if (baseModel != null) {
            baseModel!!.goToLast = true
            goToLast = true
            baseModel!!.logItems.clear()
            baseModel!!.logItems = mutableListOf()
            BookmarkManager.clear()
            logItems.clear()
            logItems = mutableListOf()
            isFilterUpdated = true
            System.gc()
        }
    }

    private fun loadFile(isAppend: Boolean) {
        val logFile = Log.file
        if (logFile == null) {
            return
        }

        var num = 0
        if (isAppend) {
            if (logItems.size > 0) {
                val item = logItems.last()
                num = item.num.toInt()
                num++
                logItems.add(LogItem(num.toString(), "LogViewer - APPEND LOG : $logFile", "", "", "", LogLevel.ERROR))
                num++
            }
        } else {
            sIsLogcatLog = false
            logItems.clear()
            logItems = mutableListOf()
            BookmarkManager.clear()
        }

        val bufferedReader = BufferedReader(FileReader(logFile!!))
        var line: String?
        var level: LogLevel
        var tag: String
        var pid: String
        var tid: String

        var logcatLogCount = 0

        line = bufferedReader.readLine()
        while (line != null) {
            val textSplited = line.trim().split(Regex("\\s+"))

            if (textSplited.size > TAG_INDEX) {
                if (Character.isDigit(textSplited[PID_INDEX][0])) {
                    level = getLevelFromFlag(textSplited[LEVEL_INDEX])
                    tag = textSplited[TAG_INDEX]
                    pid = textSplited[PID_INDEX]
                    tid = textSplited[TID_INDEX]
                } else if (Character.isAlphabetic(textSplited[PID_INDEX][0].code)) {
                    level = getLevelFromFlag(textSplited[PID_INDEX][0].toString())
                    tag = ""
                    pid = ""
                    tid = ""
                } else {
                    level = LogLevel.NONE
                    tag = ""
                    pid = ""
                    tid = ""
                }
            } else {
                level = LogLevel.NONE
                tag = ""
                pid = ""
                tid = ""
            }

            if (level != LogLevel.NONE) {
                logcatLogCount++
            }

            logItems.add(LogItem(num.toString(), line, tag, pid, tid, level))
            num++
            line = bufferedReader.readLine()
        }

        if (logcatLogCount > 10) {
            sIsLogcatLog = true
        }

        fireLogTableDataChanged()
    }

    private fun fireLogTableDataChanged(flags: Int) {
        fireLogTableDataChanged(LogTableModelEvent(this, LogTableModelEvent.EVENT_CHANGED, flags))
    }

    private fun fireLogTableDataChanged() {
        fireLogTableDataChanged(LogTableModelEvent(this, LogTableModelEvent.EVENT_CHANGED, 0))
    }

    private fun fireLogTableDataFiltered() {
        fireLogTableDataChanged(LogTableModelEvent(this, LogTableModelEvent.EVENT_FILTERED, 0))
    }

    private fun fireLogTableDataCleared() {
        fireLogTableDataChanged(LogTableModelEvent(this, LogTableModelEvent.EVENT_CLEARED, 0))
    }

    private fun fireLogTableDataChanged(event: LogTableModelEvent) {
        for (listener in eventListeners) {
            listener.tableChanged(event)
        }
    }

    fun addLogTableModelListener(eventListener: LogTableModelListener) {
        eventListeners.add(eventListener)
    }

    override fun getRowCount(): Int {
        return logItems.size
    }

    override fun getColumnCount(): Int {
        return 2 // line + log
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        try {
            if (rowIndex >= 0 && logItems.size > rowIndex) {
                val logItem = logItems[rowIndex]
                if (columnIndex == COLUMN_NUM) {
                    return logItem.num + " "
                } else if (columnIndex == COLUMN_LOG_LINE) {
                    return logItem.logLine
                }
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        return -1
    }

    override fun getColumnName(column: Int): String {
        return columnNames[column]
    }

    private fun checkLevel(item: LogItem): LogLevel {
        if (sIsLogcatLog) {
            return item.level
        } else {
            val logLine = item.logLine

            if (patternError.matcher(logLine).find()) {
                return LogLevel.ERROR
            } else if (patternWarning.matcher(logLine).find()) {
                return LogLevel.WARN
            } else if (patternInfo.matcher(logLine).find()) {
                return LogLevel.INFO
            } else if (patternDebug.matcher(logLine).find()) {
                return LogLevel.DEBUG
            }
        }

        return LogLevel.NONE
    }

    fun getFgColor(row: Int): Color {
        return when (checkLevel(logItems[row])) {
            LogLevel.VERBOSE -> {
                tableColor.logLevelVerbose
            }

            LogLevel.DEBUG -> {
                tableColor.logLevelDebug
            }

            LogLevel.INFO -> {
                tableColor.logLevelInfo
            }

            LogLevel.WARN -> {
                tableColor.logLevelWarning
            }

            LogLevel.ERROR -> {
                tableColor.logLevelError
            }

            LogLevel.FATAL -> {
                tableColor.logLevelFatal
            }

            else -> {
                tableColor.logLevelNone
            }
        }
    }

    private fun getFgStrColor(row: Int): String {
        return when (checkLevel(logItems[row])) {
            LogLevel.VERBOSE -> {
                tableColor.strLogLevelVerbose
            }

            LogLevel.DEBUG -> {
                tableColor.strLogLevelDebug
            }

            LogLevel.INFO -> {
                tableColor.strLogLevelInfo
            }

            LogLevel.WARN -> {
                tableColor.strLogLevelWarning
            }

            LogLevel.ERROR -> {
                tableColor.strLogLevelError
            }

            LogLevel.FATAL -> {
                tableColor.strLogLevelFatal
            }

            else -> tableColor.strLogLevelNone
        }
    }

    private var patternPrintSearch: Pattern? = null
    private var patternPrintHighlight: Pattern? = null
    private var patternPrintFilter: Pattern? = null
    fun getPrintValue(value: String, row: Int, isSelected: Boolean): String {
        var newValue = value
        if (newValue.indexOf("<") >= 0) {
            newValue = newValue.replace("<", "&lt;")
        }
        if (newValue.indexOf(">") >= 0) {
            newValue = newValue.replace(">", "&gt;")
        }

        val stringBuilder = StringBuilder(newValue)

        val searchStarts: Queue<Int> = LinkedList()
        val searchEnds: Queue<Int> = LinkedList()
        if (patternPrintSearch != null) {
            val matcher = patternPrintSearch!!.matcher(stringBuilder.toString())
            while (matcher.find()) {
                searchStarts.add(matcher.start(0))
                searchEnds.add(matcher.end(0))
            }
        }

        val highlightStarts: Queue<Int> = LinkedList()
        val highlightEnds: Queue<Int> = LinkedList()
        if (patternPrintHighlight != null) {
            val matcher = patternPrintHighlight!!.matcher(stringBuilder.toString())
            while (matcher.find()) {
                highlightStarts.add(matcher.start(0))
                highlightEnds.add(matcher.end(0))
            }
        }

        val filterStarts: Queue<Int> = LinkedList()
        val filterEnds: Queue<Int> = LinkedList()
        if (patternPrintFilter != null) {
            val matcher = patternPrintFilter!!.matcher(stringBuilder.toString())
            while (matcher.find()) {
                filterStarts.add(matcher.start(0))
                filterEnds.add(matcher.end(0))
            }
        }

        val boldStarts: Queue<Int> = LinkedList()
        val boldEnds: Queue<Int> = LinkedList()
        var boldStartTag = -1
        var boldEndTag = -1
        var boldStartPid = -1
        var boldEndPid = -1
        var boldStartTid = -1
        var boldEndTid = -1

        if (boldPid) {
            val item = logItems[row]
            if (item.pid.isNotEmpty()) {
                boldStartPid = newValue.indexOf(item.pid)
                boldEndPid = boldStartPid + item.pid.length
                boldStarts.add(boldStartPid)
                boldEnds.add(boldEndPid)
            }
        }

        if (boldTid) {
            val item = logItems[row]
            if (item.tid.isNotEmpty()) {
                boldStartTid = newValue.indexOf(item.tid, newValue.indexOf(item.pid) + 1)
                boldEndTid = boldStartTid + item.tid.length
                boldStarts.add(boldStartTid)
                boldEnds.add(boldEndTid)
            }
        }

        if (boldTag) {
            val item = logItems[row]
            if (item.tag.isNotEmpty()) {
                boldStartTag = newValue.indexOf(item.tag)
                boldEndTag = boldStartTag + item.tag.length
                boldStarts.add(boldStartTag)
                boldEnds.add(boldEndTag)
            }
        }

        val starts = Stack<Int>()
        val ends = Stack<Int>()
        val fgColors = Stack<String>()
        val bgColors = Stack<String>()

        var searchS = -1
        var searchE = -1
        var highlightS = -1
        var highlightE = -1
        var highlightSNext = -1
        var highlightENext = -1
        var filterS = -1
        var filterSNext = -1
        var filterENext = -1
        var filterE = -1
        var boldS = -1
        var boldE = -1
        var boldSNext = -1
        var boldENext = -1
        var boldSCheck = -1
        var boldECheck = -1

        for (idx in newValue.indices) {
            while (searchE <= idx) {
                if (searchStarts.size > 0) {
                    searchS = searchStarts.poll()
                    searchE = searchEnds.poll()

                    if (idx in (searchS + 1) until searchE) {
                        searchS = idx
                    }
                } else {
                    searchS = -1
                    searchE = -1
                    break
                }
            }
            while (highlightE <= idx) {
                if (highlightStarts.size > 0) {
                    highlightS = highlightStarts.poll()
                    highlightE = highlightEnds.poll()

                    if (idx in (highlightS + 1) until highlightE) {
                        highlightS = idx
                    }
                } else {
                    highlightS = -1
                    highlightE = -1
                    break
                }
            }
            while (filterE <= idx) {
                if (filterStarts.size > 0) {
                    filterS = filterStarts.poll()
                    filterE = filterEnds.poll()

                    if (idx in (filterS + 1) until filterE) {
                        filterS = idx
                    }
                } else {
                    filterS = -1
                    filterE = -1
                    break
                }
            }
            while (boldE <= idx) {
                if (boldStarts.size > 0) {
                    boldS = boldStarts.poll()
                    boldE = boldEnds.poll()

                    if (idx in (boldS + 1) until boldE) {
                        boldS = idx
                    }
                } else {
                    boldS = -1
                    boldE = -1
                    break
                }
            }

            if (idx == searchS) {
                if (searchE in (highlightS + 1) until highlightE) {
                    highlightS = searchE
                }

                if (searchE in (filterS + 1) until filterE) {
                    filterS = searchE
                }

                if (searchE in (boldS + 1) until boldE) {
                    boldS = searchE
                }
                starts.push(searchS)
                ends.push(searchE)
                fgColors.push(tableColor.strSearchFG)
                bgColors.push(tableColor.strSearchBG)
            }

            if (idx in searchS until searchE) {
                continue
            }

            if (idx == highlightS) {
                if (highlightE in (filterS + 1) until filterE) {
                    filterS = highlightE
                }

                if (highlightE in (boldS + 1) until boldE) {
                    boldS = highlightE
                }

                if (searchS in 1 until highlightE) {
                    if (highlightE > searchE) {
                        highlightSNext = searchE
                        highlightENext = highlightE
                    }
                    highlightE = searchS
                }

                starts.push(highlightS)
                ends.push(highlightE)
                fgColors.push(tableColor.strHighlightFG)
                bgColors.push(tableColor.strHighlightBG)

                if (highlightS < highlightSNext) {
                    highlightS = highlightSNext
                }
                if (highlightE < highlightENext) {
                    highlightE = highlightENext
                }
            }

            if (idx in highlightS until highlightE) {
                continue
            }

            if (idx == filterS) {
                if (filterE in (boldS + 1) until boldE) {
                    boldS = filterE
                }

                if (searchS > filterS && highlightS > filterS) {
                    if (searchS < highlightS) {
                        if (searchS in filterS until filterE) {
                            if (filterE > searchE) {
                                filterSNext = searchE
                                filterENext = filterE
                            }
                            filterE = searchS
                        }
                    } else {
                        if (highlightS in filterS until filterE) {
                            if (filterE > highlightE) {
                                filterSNext = highlightE
                                filterENext = filterE
                            }
                            filterE = highlightS
                        }
                    }
                } else if (searchS > filterS) {
                    if (searchS in filterS until filterE) {
                        if (filterE > searchE) {
                            filterSNext = searchE
                            filterENext = filterE
                        }
                        filterE = searchS
                    }
                } else if (highlightS > filterS) {
                    if (highlightS in filterS until filterE) {
                        if (filterE > highlightE) {
                            filterSNext = highlightE
                            filterENext = filterE
                        }
                        filterE = highlightS
                    }
                }

                starts.push(filterS)
                ends.push(filterE)
                val key = newValue.substring(filterS, filterE).uppercase()
                if (filteredFGMap[key] != null) {
                    fgColors.push(filteredFGMap[key])
                    bgColors.push(filteredBGMap[key])
                } else {
                    fgColors.push(tableColor.strFilteredFGs[0])
                    bgColors.push(tableColor.strFilteredBGs[0])
                }

                if (filterS < filterSNext) {
                    filterS = filterSNext
                }
                if (filterE < filterENext) {
                    filterE = filterENext
                }
            }

            if (idx in filterS until filterE) {
                continue
            }

            if (idx == boldS) {
                boldSCheck = -1
                boldECheck = -1
                if (highlightS in (boldS + 1) until boldE) {
                    boldSCheck = highlightS
                    boldECheck = highlightE
                }

                if (filterS in (boldS + 1) until boldE && filterS < highlightS) {
                    boldSCheck = filterS
                    boldECheck = filterE
                }

                if (boldSCheck in 1 until boldE) {
                    if (boldE > boldECheck) {
                        boldSNext = boldECheck
                        boldENext = boldE
                    }
                    boldE = boldSCheck
                }

                starts.push(boldS)
                ends.push(boldE)

                when (boldS) {
                    in boldStartTag until boldEndTag -> {
                        fgColors.push(tableColor.strTagFG)
                        bgColors.push(tableColor.strLogBG)
                    }

                    in boldStartPid until boldEndPid -> {
                        fgColors.push(tableColor.strPidFG)
                        bgColors.push(tableColor.strLogBG)
                    }

                    in boldStartTid until boldEndTid -> {
                        fgColors.push(tableColor.strTidFG)
                        bgColors.push(tableColor.strLogBG)
                    }
                }

                if (boldS < boldSNext) {
                    boldS = boldSNext
                }
                if (boldE < boldENext) {
                    boldE = boldENext
                }
            }
        }

        if (starts.size == 0) {
            if (newValue == value) {
                return ""
            }
            stringBuilder.replace(0, newValue.length, newValue.replace(" ", "&nbsp;"))
        } else {
            var beforeStart = 0
            var isFirst = true
            while (!starts.isEmpty()) {
                val start = starts.pop()
                val end = ends.pop()

                val fgColor = fgColors.pop()
                var bgColor = bgColors.pop()

                if (isFirst) {
                    if (end < newValue.length) {
                        stringBuilder.replace(
                            end,
                            newValue.length,
                            newValue.substring(end, newValue.length).replace(" ", "&nbsp;")
                        )
                    }
                    isFirst = false
                }
                if (beforeStart > end) {
                    stringBuilder.replace(
                        end,
                        beforeStart,
                        newValue.substring(end, beforeStart).replace(" ", "&nbsp;")
                    )
                }
                if (start >= 0 && end >= 0) {
                    if (isSelected) {
                        val tmpColor = Color.decode(bgColor)
                        Color(
                            tmpColor.red / 2 + tableColor.selectedBG.red / 2,
                            tmpColor.green / 2 + tableColor.selectedBG.green / 2,
                            tmpColor.blue / 2 + tableColor.selectedBG.blue / 2
                        )
                        bgColor = "#" + Integer.toHexString(
                            Color(
                                tmpColor.red / 2 + tableColor.selectedBG.red / 2,
                                tmpColor.green / 2 + tableColor.selectedBG.green / 2,
                                tmpColor.blue / 2 + tableColor.selectedBG.blue / 2
                            ).rgb
                        ).substring(2).uppercase()
                    }

                    stringBuilder.replace(
                        end,
                        end,
                        newValue.substring(end, end) + "</font></b>"
                    )
                    stringBuilder.replace(
                        start,
                        end,
                        newValue.substring(start, end).replace(" ", "&nbsp;")
                    )
                    stringBuilder.replace(
                        start,
                        start,
                        "<b><font style=\"color: $fgColor; background-color: $bgColor\">" + newValue.substring(
                            start,
                            start
                        )
                    )
                }
                beforeStart = start
            }
            if (beforeStart > 0) {
                stringBuilder.replace(0, beforeStart, newValue.substring(0, beforeStart).replace(" ", "&nbsp;"))
            }
        }

        val color = getFgStrColor(row)
        stringBuilder.replace(0, 0, "<html><p><nobr><font color=$color>")
        stringBuilder.append("</font></nobr></p></html>")
        return stringBuilder.toString()
    }

    internal inner class LogItem(
        val num: String,
        val logLine: String,
        val tag: String,
        val pid: String,
        val tid: String,
        val level: LogLevel
    )

    private fun makePattenPrintValue() {
        if (baseModel == null) {
            return
        }

        baseModel?.filterSearchLog = filterSearchLog
        if (filterSearchLog.isEmpty()) {
            patternPrintSearch = null
            baseModel?.patternPrintSearch = null
        } else {
            var start = 0
            var index = 0
            var skip = false

            while (index != -1) {
                index = filterSearchLog.indexOf('|', start)
                start = index + 1
                if (index == 0 || index == filterSearchLog.lastIndex || filterSearchLog[index + 1] == '|') {
                    skip = true
                    break
                }
            }

            if (!skip) {
                patternPrintSearch = Pattern.compile(filterSearchLog, searchPatternCase)
                baseModel?.patternPrintSearch = patternPrintSearch
            }
        }

        baseModel?.filterHighlightLog = filterHighlightLog
        if (filterHighlightLog.isEmpty()) {
            patternPrintHighlight = null
            baseModel?.patternPrintHighlight = null
        } else {
            var start = 0
            var index = 0
            var skip = false

            while (index != -1) {
                index = filterHighlightLog.indexOf('|', start)
                start = index + 1
                if (index == 0 || index == filterHighlightLog.lastIndex || filterHighlightLog[index + 1] == '|') {
                    skip = true
                    break
                }
            }

            if (!skip) {
                patternPrintHighlight = Pattern.compile(filterHighlightLog, patternCase)
                baseModel?.patternPrintHighlight = patternPrintHighlight
            }
        }

        if (filterShowLog.isEmpty()) {
            patternPrintFilter = null
            baseModel?.patternPrintFilter = null
        } else {
            var start = 0
            var index = 0
            var skip = false

            while (index != -1) {
                index = filterShowLog.indexOf('|', start)
                start = index + 1
                if (index == 0 || index == filterShowLog.lastIndex || filterShowLog[index + 1] == '|') {
                    skip = true
                    break
                }
            }

            if (!skip) {
                patternPrintFilter = Pattern.compile(filterShowLog, patternCase)
                baseModel?.patternPrintFilter = patternPrintFilter
            }
        }

        return
    }

    private fun makeFilteredItems(isRedraw: Boolean) {
        if (baseModel == null || !isFilterUpdated) {
            GLog.d(TAG, "skip makeFilteredItems $baseModel, $isFilterUpdated")
            return
        } else {
            isFilterUpdated = false
        }
        AppScope.launch(Dispatchers.UI) {
            logItems.clear()
            logItems = mutableListOf()

            val logItems: MutableList<LogItem> = mutableListOf()
            if (bookmarkMode) {
                for (item in baseModel!!.logItems) {
                    if (BookmarkManager.bookmarks.contains(item.num.toInt())) {
                        logItems.add(item)
                    }
                }
            } else {
                makePattenPrintValue()
                var isShow: Boolean

                var regexShowLog = ""
                var normalShowLog = ""
                val showLogSplit = filterShowLog.split("|")

                for (logUnit in showLogSplit) {
                    val hasIt: Boolean = logUnit.chars().anyMatch { c -> "\\.[]{}()*+?^$|".indexOf(c.toChar()) >= 0 }
                    if (hasIt) {
                        if (regexShowLog.isEmpty()) {
                            regexShowLog = logUnit
                        } else {
                            regexShowLog += "|$logUnit"
                        }
                    } else {
                        if (normalShowLog.isEmpty()) {
                            normalShowLog = logUnit
                        } else {
                            normalShowLog += "|$logUnit"
                        }

                        if (patternCase == Pattern.CASE_INSENSITIVE) {
                            normalShowLog = normalShowLog.uppercase()
                        }
                    }
                }

                val patternShowLog = Pattern.compile(regexShowLog, patternCase)
                val matcherShowLog = patternShowLog.matcher("")
                val normalShowLogSplit = normalShowLog.split("|")

                GLog.d(TAG, "Show Log $normalShowLog, $regexShowLog")
                for (item in baseModel!!.logItems) {
                    if (isFilterUpdated) {
                        break
                    }

                    isShow = true

                    if (!fullMode) {
                        if (item.level != LogLevel.NONE && item.level < filterLevel) {
                            isShow = false
                        } else if ((filterHideLog.isNotEmpty() && patternHideLog.matcher(item.logLine).find())
                            || (filterHideTag.isNotEmpty() && patternHideTag.matcher(item.tag).find())
                            || (filterHidePid.isNotEmpty() && patternHidePid.matcher(item.pid).find())
                            || (filterHideTid.isNotEmpty() && patternHideTid.matcher(item.tid).find())
                        ) {
                            isShow = false
                        } else if (filterShowLog.isNotEmpty()) {
                            var isFound = false
                            if (normalShowLog.isNotEmpty()) {
                                var logLine = ""
                                logLine = if (patternCase == Pattern.CASE_INSENSITIVE) {
                                    item.logLine.uppercase()
                                } else {
                                    item.logLine
                                }
                                for (sp in normalShowLogSplit) {
                                    if (logLine.contains(sp)) {
                                        isFound = true
                                        break
                                    }
                                }
                            }

                            if (!isFound) {
                                if (regexShowLog.isEmpty()) {
                                    isShow = false
                                } else {
                                    matcherShowLog.reset(item.logLine)
                                    if (!matcherShowLog.find()) {
                                        isShow = false
                                    }
                                }
                            }
                        }

                        if (isShow) {
                            if ((filterShowTag.isNotEmpty() && !patternShowTag.matcher(item.tag).find())
                                || (filterShowPid.isNotEmpty() && !patternShowPid.matcher(item.pid).find())
                                || (filterShowTid.isNotEmpty() && !patternShowTid.matcher(item.tid).find())
                            ) {
                                isShow = false
                            }
                        }
                    }

                    if (isShow || BookmarkManager.bookmarks.contains(item.num.toInt())) {
                        logItems.add(item)
                    }
                }
            }

            this@LogTableModel.logItems = logItems
        }

        if (!isFilterUpdated && isRedraw) {
            fireLogTableDataFiltered()
            baseModel?.fireLogTableDataFiltered()
        }
    }

    internal data class LogFilterItem(val item: LogItem, val isShow: Boolean)

    private var scanThread: Thread? = null
    private var fileWriter: FileWriter? = null
    private var isPause = false

    fun isScanning(): Boolean {
        return scanThread != null
    }

    fun startScan() {
        sIsLogcatLog = true
        val logFile = Log.file ?: return

        AppScope.launch(Dispatchers.UI) {
            scanThread?.interrupt()
        }

        goToLast = true
        baseModel?.goToLast = true

        scanThread = Thread {
            run {
                clear()
                fireLogTableDataChanged()
                baseModel!!.fireLogTableDataChanged()
                makePattenPrintValue()

                var currLogFile: File? = logFile
                var bufferedReader = BufferedReader(InputStreamReader(LogCmdManager.processLogcat!!.inputStream))
                var line: String?
                var num = 0
                var saveNum = 0
                var level: LogLevel
                var tag: String
                var pid: String
                var tid: String

                var isShow: Boolean
                var nextUpdateTime: Long = 0

                var removedCount = 0
                var baseRemovedCount = 0

                var item: LogItem
                val logLines: MutableList<String> = mutableListOf()
                val logFilterItems: MutableList<LogFilterItem> = mutableListOf()

                line = bufferedReader.readLine()
                while (line != null || (line == null && mainUI.isRestartAdbLogcat())) {
                    try {
                        nextUpdateTime = System.currentTimeMillis() + 100
                        logLines.clear()
                        logFilterItems.clear()

                        if (line == null && mainUI.isRestartAdbLogcat()) {
                            GLog.d(TAG, "line is Null : $line")
                            if (LogCmdManager.processLogcat == null || !LogCmdManager.processLogcat!!.isAlive) {
                                if (mainUI.isRestartAdbLogcat()) {
                                    Thread.sleep(5000)
                                    mainUI.restartAdbLogcat()
                                    if (LogCmdManager.processLogcat?.inputStream != null) {
                                        bufferedReader =
                                            BufferedReader(InputStreamReader(LogCmdManager.processLogcat?.inputStream!!))
                                    } else {
                                        GLog.d(TAG, "startScan : inputStream is Null")
                                    }
                                    line = "LogViewer - RESTART LOGCAT"
                                }
                            }
                        }

                        if (!isPause) {
                            while (line != null) {
                                if (currLogFile != logFile) {
                                    try {
                                        fileWriter?.flush()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                    fileWriter?.close()
                                    fileWriter = null
                                    currLogFile = logFile
                                    saveNum = 0
                                }

                                if (fileWriter == null) {
                                    fileWriter = FileWriter(logFile)
                                }
                                fileWriter?.write(line + "\n")
                                saveNum++

                                if (scrollBackSplitFile && scrollback > 0 && saveNum >= scrollback) {
                                    mainUI.setSaveLogFile()
                                    GLog.d(TAG, "Change save file : ${logFile?.absolutePath}")
                                }

                                logLines.add(line)
                                line = bufferedReader.readLine()
                                if (System.currentTimeMillis() > nextUpdateTime) {
                                    break
                                }
                            }
                        } else {
                            Thread.sleep(1000)
                        }

                        synchronized(this@LogTableModel) {
                            for (tempLine in logLines) {
                                val textSplited = tempLine.trim().split(Regex("\\s+"))
                                if (textSplited.size > TAG_INDEX) {
                                    level = getLevelFromFlag(textSplited[LEVEL_INDEX])
                                    tag = textSplited[TAG_INDEX]
                                    pid = textSplited[PID_INDEX]
                                    tid = textSplited[TID_INDEX]
                                } else {
                                    level = if (tempLine.startsWith(STRINGS.ui.app)) {
                                        LogLevel.ERROR
                                    } else {
                                        LogLevel.VERBOSE
                                    }
                                    tag = ""
                                    pid = ""
                                    tid = ""
                                }

                                item = LogItem(num.toString(), tempLine, tag, pid, tid, level)

                                isShow = true

                                if (bookmarkMode) {
                                    isShow = false
                                }

                                if (!fullMode) {
                                    if (isShow && item.level < filterLevel) {
                                        isShow = false
                                    }
                                    if (isShow
                                        && (filterHideLog.isNotEmpty() && patternHideLog.matcher(item.logLine)
                                            .find())
                                        || (filterShowLog.isNotEmpty() && !patternShowLog.matcher(item.logLine)
                                            .find())
                                    ) {
                                        isShow = false
                                    }
                                    if (isShow
                                        && ((filterHideTag.isNotEmpty() && patternHideTag.matcher(item.tag).find())
                                                || (filterShowTag.isNotEmpty() && !patternShowTag.matcher(item.tag)
                                            .find()))
                                    ) {
                                        isShow = false
                                    }
                                    if (isShow
                                        && ((filterHidePid.isNotEmpty() && patternHidePid.matcher(item.pid).find())
                                                || (filterShowPid.isNotEmpty() && !patternShowPid.matcher(item.pid)
                                            .find()))
                                    ) {
                                        isShow = false
                                    }
                                    if (isShow
                                        && ((filterHideTid.isNotEmpty() && patternHideTid.matcher(item.tid).find())
                                                || (filterShowTid.isNotEmpty() && !patternShowTid.matcher(item.tid)
                                            .find()))
                                    ) {
                                        isShow = false
                                    }
                                }
                                logFilterItems.add(LogFilterItem(item, isShow))
                                num++
                            }
                        }

                        AppScope.launch(Dispatchers.UI) {
                            if (scanThread == null) {
                                return@launch
                            }

                            val filterItems = ArrayList(logFilterItems)
                            for (filterItem in filterItems) {
                                if (selectionChanged) {
                                    baseRemovedCount = 0
                                    removedCount = 0
                                    selectionChanged = false
                                }

                                baseModel!!.logItems.add(filterItem.item)
                                while (!scrollBackKeep && scrollback > 0 && baseModel!!.logItems.count() > scrollback) {
                                    baseModel!!.logItems.removeAt(0)
                                    baseRemovedCount++
                                }
                                if (filterItem.isShow || BookmarkManager.bookmarks.contains(filterItem.item.num.toInt())) {
                                    logItems.add(filterItem.item)
                                    while (!scrollBackKeep && scrollback > 0 && logItems.count() > scrollback) {
                                        logItems.removeAt(0)
                                        removedCount++
                                    }
                                }
                            }
                        }
                        fireLogTableDataChanged(removedCount)
                        removedCount = 0

                        baseModel!!.fireLogTableDataChanged(baseRemovedCount)
                        baseRemovedCount = 0
                    } catch (e: Exception) {
                        GLog.d(TAG, "Start scan : ${e.stackTraceToString()}")
                        if (e !is InterruptedException) {
                            JOptionPane.showMessageDialog(mainUI, e.message, "Error", JOptionPane.ERROR_MESSAGE)
                        }

                        try {
                            fileWriter?.flush()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        fileWriter?.close()
                        fileWriter = null
                        return@run
                    }
                }
            }
        }

        scanThread?.start()

        return
    }

    private fun clear() {
        AppScope.launch(Dispatchers.UI) {
            logItems.clear()
            logItems = mutableListOf()
            baseModel!!.logItems.clear()
            baseModel!!.logItems = mutableListOf()
            BookmarkManager.clear()
            fireLogTableDataCleared()
            baseModel!!.fireLogTableDataCleared()
        }
    }

    fun stopScan() {
        AppScope.launch(Dispatchers.UI) {
            scanThread?.interrupt()
        }
        scanThread = null
        if (fileWriter != null) {
            try {
                fileWriter?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            fileWriter?.close()
            fileWriter?.close()
            fileWriter = null
        }
        return
    }

    fun pauseScan(pause: Boolean) {
        GLog.d(TAG, "Pause adb scan $pause")
        isPause = pause
    }

    private var followThread: Thread? = null
    private var isFollowPause = false
    private var isKeepReading = true

    fun isFollowing(): Boolean {
        return followThread != null
    }

    internal inner class MyFileInputStream(currLogFile: File?) : FileInputStream(currLogFile) {
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            var input = super.read(b, off, len)
            while (input == -1) {
                Thread.sleep(1000)
                input = super.read(b, off, len)
            }
            return input
        }
    }

    fun startFollow() {
        sIsLogcatLog = false
        val logFile = Log.file ?: return

        AppScope.launch(Dispatchers.UI) {
            followThread?.interrupt()
        }

        goToLast = true
        baseModel?.goToLast = true

        followThread = Thread {
            run {
                isKeepReading = true
                clear()
                fireLogTableDataChanged()
                baseModel!!.fireLogTableDataChanged()
                makePattenPrintValue()

                val currLogFile: File = logFile
                val scanner = Scanner(MyFileInputStream(currLogFile))
                var line: String? = null
                var num = 0
                var level: LogLevel
                var tag: String
                var pid: String
                var tid: String

                var isShow: Boolean
                var nextUpdateTime: Long = 0

                var removedCount = 0
                var baseRemovedCount = 0

                var item: LogItem
                val logLines: MutableList<String> = mutableListOf()
                val logFilterItems: MutableList<LogFilterItem> = mutableListOf()

                var logcatLogCount = 0

                while (isKeepReading) {
                    try {
                        nextUpdateTime = System.currentTimeMillis() + 100
                        logLines.clear()
                        logFilterItems.clear()
                        if (!isPause) {
                            while (isKeepReading) {
                                line = if (scanner.hasNextLine()) {
                                    try {
                                        scanner.nextLine()
                                    } catch (e: NoSuchElementException) {
                                        null
                                    }
                                } else {
                                    null
                                }
                                if (line == null) {
                                    Thread.sleep(1000)
                                } else {
                                    break
                                }
                            }

                            while (line != null) {
                                logLines.add(line)

                                line = if (scanner.hasNextLine()) {
                                    try {
                                        scanner.nextLine()
                                    } catch (e: NoSuchElementException) {
                                        null
                                    }
                                } else {
                                    null
                                }
                                if (System.currentTimeMillis() > nextUpdateTime) {
                                    if (line != null) {
                                        logLines.add(line)
                                    }
                                    break
                                }
                            }
                        } else {
                            Thread.sleep(1000)
                        }

                        synchronized(this@LogTableModel) {
                            for (tempLine in logLines) {
                                val textSplited = tempLine.trim().split(Regex("\\s+"))
                                if (textSplited.size > TAG_INDEX) {
                                    level = getLevelFromFlag(textSplited[LEVEL_INDEX])
                                    tag = textSplited[TAG_INDEX]
                                    pid = textSplited[PID_INDEX]
                                    tid = textSplited[TID_INDEX]
                                } else {
                                    level = if (tempLine.startsWith(STRINGS.ui.app)) {
                                        LogLevel.ERROR
                                    } else {
                                        LogLevel.VERBOSE
                                    }
                                    tag = ""
                                    pid = ""
                                    tid = ""
                                }

                                item = LogItem(num.toString(), tempLine, tag, pid, tid, level)

                                isShow = true

                                if (bookmarkMode) {
                                    isShow = false
                                }

                                if (!fullMode) {
                                    if (isShow && item.level < filterLevel) {
                                        isShow = false
                                    }
                                    if (isShow
                                        && (filterHideLog.isNotEmpty() && patternHideLog.matcher(item.logLine)
                                            .find())
                                        || (filterShowLog.isNotEmpty() && !patternShowLog.matcher(item.logLine)
                                            .find())
                                    ) {
                                        isShow = false
                                    }
                                    if (isShow
                                        && ((filterHideTag.isNotEmpty() && patternHideTag.matcher(item.tag).find())
                                                || (filterShowTag.isNotEmpty() && !patternShowTag.matcher(item.tag)
                                            .find()))
                                    ) {
                                        isShow = false
                                    }
                                    if (isShow
                                        && ((filterHidePid.isNotEmpty() && patternHidePid.matcher(item.pid).find())
                                                || (filterShowPid.isNotEmpty() && !patternShowPid.matcher(item.pid)
                                            .find()))
                                    ) {
                                        isShow = false
                                    }
                                    if (isShow
                                        && ((filterHideTid.isNotEmpty() && patternHideTid.matcher(item.tid).find())
                                                || (filterShowTid.isNotEmpty() && !patternShowTid.matcher(item.tid)
                                            .find()))
                                    ) {
                                        isShow = false
                                    }
                                }
                                logFilterItems.add(LogFilterItem(item, isShow))
                                num++
                            }
                        }

                        AppScope.launch(Dispatchers.UI) {
                            if (followThread == null) {
                                return@launch
                            }

                            val filterItems = ArrayList(logFilterItems)
                            for (filterItem in filterItems) {
                                if (selectionChanged) {
                                    baseRemovedCount = 0
                                    removedCount = 0
                                    selectionChanged = false
                                }

                                if (filterItem.item.level != LogLevel.NONE) {
                                    logcatLogCount++
                                }

                                if (logcatLogCount > 10) {
                                    sIsLogcatLog = true
                                }
                                baseModel!!.logItems.add(filterItem.item)
                                while (!scrollBackKeep && scrollback > 0 && baseModel!!.logItems.count() > scrollback) {
                                    baseModel!!.logItems.removeAt(0)
                                    baseRemovedCount++
                                }
                                if (filterItem.isShow || BookmarkManager.bookmarks.contains(filterItem.item.num.toInt())) {
                                    logItems.add(filterItem.item)
                                    while (!scrollBackKeep && scrollback > 0 && logItems.count() > scrollback) {
                                        logItems.removeAt(0)
                                        removedCount++
                                    }
                                }
                            }
                        }

                        fireLogTableDataChanged(removedCount)
                        removedCount = 0

                        baseModel!!.fireLogTableDataChanged(baseRemovedCount)
                        baseRemovedCount = 0
                    } catch (e: Exception) {
                        GLog.d(TAG, "Start follow : ${e.stackTraceToString()}")
                        if (e !is InterruptedException) {
                            JOptionPane.showMessageDialog(mainUI, e.message, "Error", JOptionPane.ERROR_MESSAGE)
                        }

                        return@run
                    }
                }
                GLog.d(TAG, "Exit follow")
            }
        }

        followThread?.start()

        return
    }

    fun stopFollow() {
        AppScope.launch(Dispatchers.UI) {
            isKeepReading = false
            followThread?.interrupt()
        }
        followThread = null
        return
    }

    fun pauseFollow(pause: Boolean) {
        GLog.d(TAG, "Pause file follow $pause")
        isFollowPause = pause
    }

    fun moveToNextSearch() {
        moveToSearch(true)
    }

    fun moveToPrevSearch() {
        moveToSearch(false)
    }

    private infix fun Int.toward(to: Int): IntProgression {
        val step = if (this > to) -1 else 1
        return IntProgression.fromClosedRange(this, to, step)
    }

    private fun moveToSearch(isNext: Boolean) {
        val selectedRow = if (baseModel != null) {
            mainUI.splitLogPane.filteredLogPanel.getSelectedRow()
        } else {
            mainUI.splitLogPane.fullLogPanel.getSelectedRow()
        }

        var startRow = 0
        var endRow = 0

        if (isNext) {
            startRow = selectedRow + 1
            endRow = logItems.count() - 1
            if (startRow >= endRow) {
                mainUI.showSearchResultTooltip(isNext, "\"${filterSearchLog}\" ${STRINGS.ui.notFound}")
                return
            }
        } else {
            startRow = selectedRow - 1
            endRow = 0

            if (startRow < endRow) {
                mainUI.showSearchResultTooltip(isNext, "\"${filterSearchLog}\" ${STRINGS.ui.notFound}")
                return
            }
        }

        var idxFound = -1
        for (idx in startRow toward endRow) {
            val item = logItems[idx]
            if (normalSearchLogSplit != null) {
                var logLine = ""
                logLine = if (searchPatternCase == Pattern.CASE_INSENSITIVE) {
                    item.logLine.uppercase()
                } else {
                    item.logLine
                }
                for (sp in normalSearchLogSplit!!) {
                    if (sp.isNotEmpty() && logLine.contains(sp)) {
                        idxFound = idx
                        break
                    }
                }
            }

            if (idxFound < 0 && regexSearchLog.isNotEmpty() && matcherSearchLog != null) {
                matcherSearchLog.reset(item.logLine)
                if (matcherSearchLog.find()) {
                    idxFound = idx
                }
            }

            if (idxFound >= 0) {
                break
            }
        }

        if (idxFound >= 0) {
            if (baseModel != null) {
                mainUI.splitLogPane.filteredLogPanel.goToRow(idxFound, 0)
            } else {
                mainUI.splitLogPane.fullLogPanel.goToRow(idxFound, 0)
            }
        } else {
            mainUI.showSearchResultTooltip(isNext, "\"${filterSearchLog}\" ${STRINGS.ui.notFound}")
        }
    }

    companion object {

        private const val TAG = "LogTableModel"
        private const val COLUMN_NUM = 0
        private const val COLUMN_LOG_LINE = 1

        private const val PID_INDEX = 2
        private const val TID_INDEX = 3
        private const val LEVEL_INDEX = 4
        private const val TAG_INDEX = 5

        var sIsLogcatLog = false
    }
}
