package me.gegenbauer.catspy.ui.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.command.LogCmdManager
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.ModelScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.task.*
import me.gegenbauer.catspy.ui.ColorScheme
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.log.FilterItem.Companion.isNotEmpty
import me.gegenbauer.catspy.ui.log.LogcatLogItem.Companion.fgColor
import me.gegenbauer.catspy.ui.log.LogcatLogItem.Companion.isShow
import me.gegenbauer.catspy.ui.log.LogcatRealTimeFilter.Companion.emptyRealTimeFilter
import me.gegenbauer.catspy.utils.toHtml
import java.awt.Color
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.Pattern
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel
import kotlin.concurrent.write


// TODO refactor

fun interface LogTableModelListener {
    fun tableChanged(event: TableModelEvent)
}

/**
 * TODO 整理任务停止和启动的逻辑
 */
class LogTableModel(private val mainUI: MainUI, private var baseModel: LogTableModel?) : AbstractTableModel(),
    TaskListener {
    private val columnNames = arrayOf("line", "log")
    private val logItems: MutableList<LogcatLogItem> = ArrayList()
    private val cachedItems = ArrayList<LogcatLogItem>()
    private val logNum = AtomicInteger(0)

    private val scope = ModelScope()
    private val eventListeners = ArrayList<LogTableModelListener>()
    private val taskManager = TaskManager()
    private val updateUITask = PeriodicTask(DURATION_UPDATE_UI, ::updateTableUI, "updateTableUI")
    private var logcatTask: LogcatTask? = null
    private var followFileTask: ReadFileTask? = null

    private var isFilterUpdated = true

    var selectionChanged = false

    var highlightFilterItem: FilterItem = FilterItem.emptyItem
    var searchFilterItem: FilterItem = FilterItem.emptyItem

    private var patternCase = Pattern.CASE_INSENSITIVE
    var matchCase: Boolean = false
        set(value) {
            if (field != value) {
                patternCase = if (!value) {
                    Pattern.CASE_INSENSITIVE
                } else {
                    0
                }
            }

            updateLogWithFilter()

            field = value
        }

    private var searchPatternCase = Pattern.CASE_INSENSITIVE
    var searchMatchCase: Boolean = false
        set(value) {
            if (field != value) {
                searchPatternCase = if (!value) {
                    Pattern.CASE_INSENSITIVE
                } else {
                    0
                }

                baseModel?.searchMatchCase = value
            }
            field = value
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
    var logFilter: LogcatRealTimeFilter = emptyRealTimeFilter
        set(value) {
            if (value != field) {
                isFilterUpdated = true
                updateLogWithFilter()
            }
            field = value
        }

    fun loadFromFile(file: File, isAppend: Boolean) {
        logTempFile = file
        if (baseModel == null) {
            loadFile(file, isAppend)
        } else {
            isFilterUpdated = true
        }
        updateLogWithFilter()
    }

    fun clearItems() {
        GLog.d(TAG, "isEventDispatchThread = ${SwingUtilities.isEventDispatchThread()}")
        this.baseModel?.let {
            it.goToLast = true
            goToLast = true
            it.logItems.clear()
            it.logNum.set(0)
            BookmarkManager.clear()
            logItems.clear()
            logNum.set(0)
            isFilterUpdated = true
            fireTableDataChanged()
            Runtime.getRuntime().gc()
        }
    }

    private fun loadFile(file: File, isAppend: Boolean) {
        if (isAppend) {
            if (logItems.size > 0) {
                logItems.add(
                    LogcatLogItem.from(
                        "${STRINGS.ui.app} - APPEND LOG : ${file.absoluteFile}",
                        logNum.getAndIncrement()
                    )
                )
            }
        } else {
            logItems.clear()
            logNum.set(0)
            BookmarkManager.clear()
        }

        val bufferedReader = BufferedReader(FileReader(file))
        var line: String?

        line = bufferedReader.readLine()
        while (line != null) {
            val item = LogcatLogItem.from(line, logNum.getAndIncrement())
            logItems.add(item)
            line = bufferedReader.readLine()
        }

        fireTableDataChanged()
    }

    fun addLogTableModelListener(eventListener: LogTableModelListener) {
        eventListeners.add(eventListener)
    }

    override fun fireTableChanged(e: TableModelEvent) {
        super.fireTableChanged(e)
        for (eventListener in eventListeners) {
            eventListener.tableChanged(e)
        }
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
        searchFilterItem.takeIf { it.isNotEmpty() }?.let {
            val matcher = searchFilterItem.positiveFilter.matcher(stringBuilder.toString())
            while (matcher.find()) {
                searchStarts.add(matcher.start(0))
                searchEnds.add(matcher.end(0))
            }
        }



        val highlightStarts: Queue<Int> = LinkedList()
        val highlightEnds: Queue<Int> = LinkedList()
        highlightFilterItem.takeIf { it.isNotEmpty() }?.let {
            val matcher = highlightFilterItem.positiveFilter.matcher(stringBuilder.toString())
            while (matcher.find()) {
                highlightStarts.add(matcher.start(0))
                highlightEnds.add(matcher.end(0))
            }
        }

        val filterStarts: Queue<Int> = LinkedList()
        val filterEnds: Queue<Int> = LinkedList()
        logFilter.filterLog.takeIf { it.isNotEmpty() }?.let {
            val matcher = logFilter.filterLog.positiveFilter.matcher(stringBuilder.toString())
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
                fgColors.push(ColorScheme.filteredFGs[0])
                bgColors.push(ColorScheme.filteredBGs[0])

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

    private fun updateLogWithFilter() {
        if (baseModel != null && baseModel?.logItems?.isEmpty() == true) return
        if (baseModel == null || !isFilterUpdated) {
            GLog.d(TAG, "[updateLogWithFilter] skip makeFilteredItems $baseModel, $isFilterUpdated")
            return
        } else {
            isFilterUpdated = false
        }
        GLog.d(TAG, "[updateLogWithFilter] filter: $logFilter")
        // TODO use [Dispatchers.UI.immediate] will return wrong filter data
        AppScope.launch(Dispatchers.UI) {
            logItems.clear()
            logNum.set(0)

            val logItems: MutableList<LogcatLogItem> = mutableListOf()
            if (bookmarkMode) {
                for (item in baseModel!!.logItems) {
                    if (BookmarkManager.isBookmark(item.num)) {
                        logItems.add(item)
                    }
                }
            } else {
                for (item in baseModel!!.logItems) {
                    if (isFilterUpdated) {
                        return@launch
                    }

                    if (fullMode || BookmarkManager.isBookmark(item.num) || logFilter.filter(item)) {
                        logItems.add(item)
                    }
                }
            }

            this@LogTableModel.logItems.clear()
            this@LogTableModel.logItems.addAll(logItems)
            if (!isFilterUpdated) {
                fireTableDataChanged()
                baseModel?.fireTableDataChanged()
            }
        }
    }

    fun isScanning(): Boolean {
        return logcatTask?.isTaskRunning() ?: false
    }

    // TODO 没有连接设备时不进行扫描
    fun startScan() {
        resetTable()
        startLogcatTask()
        taskManager.exec(updateUITask)
    }

    private fun resetTable() {
        goToLast = true
        baseModel?.goToLast = true
        clear()
        fireTableDataChanged()
        baseModel?.fireTableDataChanged()
    }

    private fun startLogcatTask() {
        logcatTask = LogcatTask(LogCmdManager.targetDevice).apply {
            logTempFile = tempFile
            addListener(this@LogTableModel)
            taskManager.exec(this)
        }
    }

    fun startFollow(logFile: File) {
        if (logFile.exists().not()) return
        resetTable()
        startFollowFileTask(logFile)

        return
    }

    private fun startFollowFileTask(logFile: File) {
        followFileTask = ReadFileTask(logFile).apply {
            addListener(this@LogTableModel)
            taskManager.exec(this)
        }
    }

    override fun onError(task: Task, error: String, t: Throwable?) {
        super.onError(task, error, t)
        scope.launch(Dispatchers.UI) {
            JOptionPane.showMessageDialog(
                mainUI,
                error,
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    override fun onFinalResult(task: Task, data: Any) {
        if (mainUI.isRestartAdbLogcat()) {
            Thread.sleep(5000)
            mainUI.restartAdbLogcat()
            startLogcatTask()
            logItems.add(LogcatLogItem("${STRINGS.ui.app} - RESTART LOGCAT", level = LogLevel.ERROR))
        }
    }

    private val updateUILock = ReentrantReadWriteLock()
    private var logTempFile: File? = null

    override fun onStart(task: Task) {
        super.onStart(task)
        count.set(0)
    }

    private fun updateTableUI() {
        scope.launch(Dispatchers.UI) {
            val oldSize = logItems.size
            val baseModelOldSize = baseModel!!.logItems.size
            updateUILock.write {
                GLog.d(TAG, "[updateTableUITask] get lock")
                baseModel?.cachedItems?.forEach {
                    baseModel!!.logItems.add(it)
                }
                if (!scrollBackKeep && scrollback > 0 && baseModel!!.logItems.count() > scrollback) {
                    baseModel!!.logItems.removeIf { it.num in (baseModel!!.logItems.size - scrollback)..baseModel!!.logItems.size }
                }
                cachedItems.forEach {
                    if (it.isShow() || BookmarkManager.isBookmark(it.num)) logItems.add(it)
                }
                if (!scrollBackKeep && scrollback > 0 && logItems.count() > scrollback) {
                    logItems.removeIf { it.num in (logItems.size - scrollback)..logItems.size }
                }
                cachedItems.clear()
                baseModel!!.cachedItems.clear()
            }
            GLog.d(TAG, "[updateTableUITask] items count=${logItems.size}, baseModel=${baseModel!!.logItems.size}")
            if (logItems.isNotEmpty()) {
                fireTableRowsInserted(oldSize, logItems.size - 1)
            }
            if (baseModel!!.logItems.isNotEmpty()) {
                baseModel!!.fireTableRowsInserted(baseModelOldSize, baseModel!!.logItems.size - 1)
            }
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
        if (task is LogcatTask) {
            processForLogcatTask(data)
        } else if (task is ReadFileTask) {
            processForReadFileTask(data)
        }
    }

    private fun processForLogcatTask(data: Any) {
        val line = data as String
        processLine(line)
    }

    private fun processLine(line: String) {
        count.incrementAndGet()
        cachedCount.incrementAndGet()
        val item = LogcatLogItem.from(line, logNum.getAndIncrement())

        if (!fullMode) {
            item.setHidden(logFilter.filter(item).not())
        }
        if (selectionChanged) {
            selectionChanged = false
        }

        updateUILock.write {
            baseModel?.cachedItems?.add(item)
            cachedItems.add(item)
        }
    }

    private fun processForReadFileTask(data: Any) {
        val line = data as List<String>
        line.forEach(::processLine)
        updateTableUI()
    }

    private fun clear() {
        scope.launch(Dispatchers.Main.immediate) {
            logItems.clear()
            logNum.set(0)
            baseModel?.logNum?.set(0)
            baseModel?.logItems?.clear()
            BookmarkManager.clear()
            fireTableDataChanged()
            baseModel?.fireTableDataChanged()
        }
    }

    fun stopAll() {
        logcatTask?.cancel()
        updateUITask.pause()
        followFileTask?.cancel()
    }

    fun pause(pause: Boolean) {
        GLog.d(TAG, "[pauseScan]")
        taskManager.updatePauseState(pause)
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
                mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
                return
            }
        } else {
            startRow = selectedRow - 1
            endRow = 0

            if (startRow < endRow) {
                mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
                return
            }
        }

        var idxFound = -1
        for (idx in startRow toward endRow) {
            val item = logItems[idx]
            if (searchFilterItem.positiveFilter.matcher(item.logLine).find()) {
                idxFound = idx
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
            mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
        }
    }

    override fun fireTableDataChanged() {
        super.fireTableDataChanged()
        GLog.d(TAG, "[fireTableDataChanged]")
    }

    companion object {
        const val COLUMN_NUM = 0
        const val COLUMN_LOG = 1
        private const val TAG = "LogTableModel"
        private const val UNBREAKABLE_SPACE = "&nbsp;"
        private const val DURATION_UPDATE_UI = 500L
    }

}
