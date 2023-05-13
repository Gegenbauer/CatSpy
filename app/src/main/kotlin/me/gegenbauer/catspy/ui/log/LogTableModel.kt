package me.gegenbauer.catspy.ui.log

import kotlinx.coroutines.*
import me.gegenbauer.catspy.file.Log
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.command.LogCmdManager
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.task.LogcatTask
import me.gegenbauer.catspy.task.Task
import me.gegenbauer.catspy.task.TaskListener
import me.gegenbauer.catspy.task.TaskManager
import me.gegenbauer.catspy.ui.ColorScheme
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.combobox.FilterComboBox
import me.gegenbauer.catspy.ui.log.LogItem.Companion.fgColor
import me.gegenbauer.catspy.utils.toHtml
import java.awt.Color
import java.io.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import kotlin.collections.ArrayList
import kotlin.concurrent.write


// TODO refactor
data class LogTableModelEvent(val source: LogTableModel, val dataChange: Int, val removedCount: Int) {
    companion object {
        const val EVENT_FILTERED = 2
        const val EVENT_CHANGED = 3
        const val EVENT_CLEARED = 4
    }
}

fun interface LogTableModelListener {
    fun tableChanged(event: LogTableModelEvent)
}

class LogTableModel(private val mainUI: MainUI, private var baseModel: LogTableModel?) : AbstractTableModel(),
    TaskListener {
    private var patternSearchLog: Pattern = Pattern.compile("", Pattern.CASE_INSENSITIVE)
    private var matcherSearchLog: Matcher = patternSearchLog.matcher("")
    private var normalSearchLogSplit: List<String>? = null
    private val columnNames = arrayOf("line", "log")
    private val logItems: MutableList<LogItem> = CopyOnWriteArrayList()
    private var cachedItems = ArrayList<LogFilterItem>()
    private val logNum = AtomicInteger(0)

    private val scope = ModelScope()
    private val eventListeners = ArrayList<LogTableModelListener>()
    private val filteredFGMap = mutableMapOf<String, Color>()
    private val filteredBGMap = mutableMapOf<String, Color>()
    private val taskManager = TaskManager()
    private var logcatTask: LogcatTask? = null
    private var updateTableUITask: Job? = null

    private var isFilterUpdated = true

    var selectionChanged = false

    var filterLevel: LogLevel = LogLevel.NONE
        set(value) {
            if (field != value) {
                isFilterUpdated = true
                field = value
            }
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

    fun isFullDataModel(): Boolean {
        return baseModel == null
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
                            filteredFGMap[key.uppercase()] = ColorScheme.filteredFGs[item[1].digitToInt()]
                            filteredBGMap[key.uppercase()] = ColorScheme.filteredBGs[item[1].digitToInt()]
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
        this.baseModel?.let {
            it.goToLast = true
            goToLast = true
            it.logItems.clear()
            it.logNum.set(0)
            BookmarkManager.clear()
            logItems.clear()
            logNum.set(0)
            isFilterUpdated = true
            Runtime.getRuntime().gc()
        }
    }

    private fun loadFile(isAppend: Boolean) {
        val logFile = Log.file ?: return

        if (isAppend) {
            if (logItems.size > 0) {
                logItems.add(LogItem.from("${STRINGS.ui.app} - APPEND LOG : $logFile", logNum.getAndIncrement()))
            }
        } else {
            logItems.clear()
            logNum.set(0)
            BookmarkManager.clear()
        }

        val bufferedReader = BufferedReader(FileReader(logFile))
        var line: String?

        line = bufferedReader.readLine()
        while (line != null) {
            val item = LogItem.from(line, logNum.getAndIncrement())
            logItems.add(item)
            line = bufferedReader.readLine()
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
                    return "${logItem.num} "
                } else if (columnIndex == COLUMN_LOG) {
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

    fun getFgColor(row: Int): Color {
        return logItems[row].fgColor
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
        val fgColors = Stack<Color>()
        val bgColors = Stack<Color>()

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
                fgColors.push(ColorScheme.searchFG)
                bgColors.push(ColorScheme.searchBG)
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
                fgColors.push(ColorScheme.highlightFG)
                bgColors.push(ColorScheme.highlightBG)

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
                    fgColors.push(ColorScheme.filteredFGs[0])
                    bgColors.push(ColorScheme.filteredBGs[0])
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
                        fgColors.push(ColorScheme.tagFG)
                        bgColors.push(ColorScheme.logBG)
                    }

                    in boldStartPid until boldEndPid -> {
                        fgColors.push(ColorScheme.pidFG)
                        bgColors.push(ColorScheme.logBG)
                    }

                    in boldStartTid until boldEndTid -> {
                        fgColors.push(ColorScheme.tidFG)
                        bgColors.push(ColorScheme.logBG)
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
            stringBuilder.replace(0, newValue.length, newValue.replace(" ", UNBREAKABLE_SPACE))
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
                            newValue.substring(end, newValue.length).replace(" ", UNBREAKABLE_SPACE)
                        )
                    }
                    isFirst = false
                }
                if (beforeStart > end) {
                    stringBuilder.replace(
                        end,
                        beforeStart,
                        newValue.substring(end, beforeStart).replace(" ", UNBREAKABLE_SPACE)
                    )
                }
                if (start >= 0 && end >= 0) {
                    if (isSelected) {
                        val tmpColor = bgColor
                        bgColor = Color(
                            tmpColor.red / 2 + ColorScheme.selectedBG.red / 2,
                            tmpColor.green / 2 + ColorScheme.selectedBG.green / 2,
                            tmpColor.blue / 2 + ColorScheme.selectedBG.blue / 2
                        )
                    }

                    stringBuilder.replace(
                        end,
                        end,
                        newValue.substring(end, end) + "</font></b>"
                    )
                    stringBuilder.replace(
                        start,
                        end,
                        newValue.substring(start, end).replace(" ", UNBREAKABLE_SPACE)
                    )
                    stringBuilder.replace(
                        start,
                        start,
                        "<b><font style=\"color: ${fgColor.toHtml()}; background-color: ${bgColor.toHtml()}\">" + newValue.substring(
                            start,
                            start
                        )
                    )
                }
                beforeStart = start
            }
            if (beforeStart > 0) {
                stringBuilder.replace(
                    0,
                    beforeStart,
                    newValue.substring(0, beforeStart).replace(" ", UNBREAKABLE_SPACE)
                )
            }
        }

        val color = getFgColor(row)
        stringBuilder.replace(0, 0, "<html><p><nobr><font color=${color.toHtml()}>")
        stringBuilder.append("</font></nobr></p></html>")
        return stringBuilder.toString()
    }

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
            logNum.set(0)

            val logItems: MutableList<LogItem> = mutableListOf()
            if (bookmarkMode) {
                for (item in baseModel!!.logItems) {
                    if (BookmarkManager.isBookmark(item.num)) {
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

                    if (isShow || BookmarkManager.isBookmark(item.num)) {
                        logItems.add(item)
                    }
                }
            }

            this@LogTableModel.logItems.clear()
            this@LogTableModel.logItems.addAll(logItems)
        }

        if (!isFilterUpdated && isRedraw) {
            fireLogTableDataFiltered()
            baseModel?.fireLogTableDataFiltered()
        }
    }

    internal data class LogFilterItem(val item: LogItem, val isShow: Boolean)

    fun isScanning(): Boolean {
        return logcatTask?.isTaskRunning() ?: false
    }

    fun startScan() {
        goToLast = true
        baseModel?.goToLast = true

        clear()
        fireLogTableDataChanged()
        baseModel?.fireLogTableDataChanged()
        makePattenPrintValue()
        startLogcatTask()

        updateTableUITask = scope.launch(Dispatchers.UI) {
            repeat(Int.MAX_VALUE) {
                delay(500)
                updateTableUI()
            }
        }
    }

    private fun startLogcatTask() {
        val logcatTask = LogcatTask(LogCmdManager.targetDevice)
        this.logcatTask = logcatTask
        logcatTask.addListener(this)
        taskManager.exec(logcatTask)
    }

    override fun onFinalResult(task: Task, data: Any) {
        if (mainUI.isRestartAdbLogcat()) {
            Thread.sleep(5000)
            mainUI.restartAdbLogcat()
            startLogcatTask()
            logItems.add(LogItem("${STRINGS.ui.app} - RESTART LOGCAT", level = LogLevel.ERROR))
        }
    }

    private val updateUILock = ReentrantReadWriteLock()

    override fun onStart(task: Task) {
        super.onStart(task)
        count.set(0)
    }

    private fun updateTableUI() {
        updateUILock.write {
            baseModel?.cachedItems?.forEach {
                baseModel!!.logItems.add(it.item)
            }
            if (!scrollBackKeep && scrollback > 0 && baseModel!!.logItems.count() > scrollback) {
                baseModel!!.logItems.removeIf { it.num in (baseModel!!.logItems.size - scrollback)..baseModel!!.logItems.size }
            }
            cachedItems.forEach {
                if (it.isShow || BookmarkManager.isBookmark(it.item.num)) logItems.add(it.item)
            }
            if (!scrollBackKeep && scrollback > 0 && logItems.count() > scrollback) {
                logItems.removeIf { it.num in (logItems.size - scrollback)..logItems.size }
            }
            cachedItems.clear()
            baseModel!!.cachedItems.clear()
        }
        GLog.d(TAG, "[updateTableUITask] items count=${logItems.size}, baseModel=${baseModel!!.logItems.size}")
        if (logItems.isNotEmpty()) {
            fireLogTableDataChanged(0)
        }
        if (baseModel!!.logItems.isNotEmpty()) {
            baseModel!!.fireLogTableDataChanged(0)
        }
    }

    override fun onStop(task: Task) {
        super.onStop(task)
        updateTableUI()
        GLog.e(TAG, "[onStop] count=${count.get()}")
    }

    private val count = AtomicInteger(0)
    private val cachedCount = AtomicInteger(0)
    override fun onProgress(task: Task, data: Any) {
        super.onProgress(task, data)
        count.incrementAndGet()
        cachedCount.incrementAndGet()
        val line = data as String
        var isShow: Boolean
        val item = LogItem.from(line, logNum.getAndIncrement())

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
        val filterItem = LogFilterItem(item, isShow)
        if (selectionChanged) {
            selectionChanged = false
        }

        updateUILock.write {
            baseModel?.cachedItems?.add(filterItem)
            cachedItems.add(filterItem)
        }
    }

    private fun clear() {
        scope.launch(Dispatchers.Main.immediate) {
            logItems.clear()
            logNum.set(0)
            baseModel?.logNum?.set(0)
            baseModel?.logItems?.clear()
            BookmarkManager.clear()
            fireLogTableDataCleared()
            baseModel?.fireLogTableDataCleared()
        }
    }

    fun stopScan() {
        logcatTask?.cancel()
        updateTableUITask?.cancel()
    }

    fun pauseScan(pause: Boolean) {
        GLog.d(TAG, "[pauseScan]")
        if (pause) {
            logcatTask?.pause()
        } else {
            logcatTask?.resume()
        }
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
        val logFile = Log.file ?: return

        scope.launch(Dispatchers.UI) {
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

                var isShow: Boolean
                var nextUpdateTime: Long = 0

                var removedCount = 0
                var baseRemovedCount = 0

                val logLines: MutableList<String> = mutableListOf()
                val logFilterItems: MutableList<LogFilterItem> = mutableListOf()

                while (isKeepReading) {
                    try {
                        nextUpdateTime = System.currentTimeMillis() + 100
                        logLines.clear()
                        logFilterItems.clear()
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

                        synchronized(this@LogTableModel) {
                            for (tempLine in logLines) {
                                val item = LogItem.from(tempLine, logNum.getAndIncrement())
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
                                baseModel!!.logItems.add(filterItem.item)
                                while (!scrollBackKeep && scrollback > 0 && baseModel!!.logItems.count() > scrollback) {
                                    baseModel!!.logItems.removeAt(0)
                                    baseRemovedCount++
                                }
                                if (filterItem.isShow || BookmarkManager.isBookmark(filterItem.item.num)) {
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

            if (idxFound < 0 && regexSearchLog.isNotEmpty()) {
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

        const val COLUMN_NUM = 0
        const val COLUMN_LOG = 1
        private const val TAG = "LogTableModel"
        private const val UNBREAKABLE_SPACE = "&nbsp;"
    }

}
