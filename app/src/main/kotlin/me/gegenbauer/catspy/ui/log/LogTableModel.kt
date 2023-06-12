package me.gegenbauer.catspy.ui.log

import me.gegenbauer.catspy.data.model.log.FilterItem
import me.gegenbauer.catspy.data.model.log.FilterItem.Companion.isNotEmpty
import me.gegenbauer.catspy.data.model.log.LogcatLogItem.Companion.fgColor
import me.gegenbauer.catspy.data.model.log.LogcatRealTimeFilter
import me.gegenbauer.catspy.data.repo.log.FullLogcatRepository
import me.gegenbauer.catspy.data.repo.log.LogRepository
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.ColorScheme
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.utils.toHtml
import java.awt.Color
import java.util.*
import java.util.regex.Pattern
import javax.swing.event.TableModelEvent
import javax.swing.table.AbstractTableModel


// TODO refactor

fun interface LogTableModelListener {
    fun tableChanged(event: TableModelEvent)
}

/**
 * TODO 整理任务停止和启动的逻辑
 */
class LogTableModel(
    private val mainUI: MainUI,
    private val logRepository: LogRepository
) : AbstractTableModel(), LogRepository.LogChangeListener {
    private val columnNames = arrayOf("line", "log")
    private val eventListeners = ArrayList<LogTableModelListener>()

    var selectionChanged = false

    var highlightFilterItem: FilterItem = FilterItem.emptyItem
        set(value) {
            if (field != value) {
                field = value
                fireTableDataChanged()
            }
        }
    var searchFilterItem: FilterItem = FilterItem.emptyItem

    private var searchPatternCase = Pattern.CASE_INSENSITIVE
    var searchMatchCase: Boolean = false
        set(value) {
            if (field != value) {
                searchPatternCase = if (!value) {
                    Pattern.CASE_INSENSITIVE
                } else {
                    0
                }
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
            logRepository.bookmarkMode = value
            if (value) {
                fullMode = false
            }
        }

    var fullMode = false
        set(value) {
            field = value
            logRepository.fullMode = value
            if (value) {
                bookmarkMode = false
            }
        }

    init {
        logRepository.addLogChangeListener(this)
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
        return logRepository.getLogCount()
    }

    override fun getColumnCount(): Int {
        return 2 // line + log
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return logRepository.accessLogItems {logItems ->
            if (rowIndex >= 0 && logRepository.getLogCount() > rowIndex) {
                val logItem = logItems[rowIndex]
                if (columnIndex == COLUMN_NUM) {
                    return@accessLogItems "${logItem.num} "
                } else if (columnIndex == COLUMN_LOG) {
                    return@accessLogItems logItem.logLine
                }
            }

            return@accessLogItems -1
        }
    }

    override fun getColumnName(column: Int): String {
        return columnNames[column]
    }

    fun getFgColor(row: Int): Color {
        return logRepository.accessLogItems { logItems ->
            logItems[row].fgColor
        }
    }

    fun getPrintValue(value: String, row: Int, isSelected: Boolean): String {
        return logRepository.accessLogItems {logItems ->
            val newValue = value.replace("<", "&lt;").replace(">", "&gt;")
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
            val logcatRealTimeFilter = logRepository.logFilter as LogcatRealTimeFilter
            logcatRealTimeFilter.filterLog.takeIf { it.isNotEmpty() }?.let {
                val matcher = logcatRealTimeFilter.filterLog.positiveFilter.matcher(stringBuilder.toString())
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
                    if (searchStarts.isNotEmpty()) {
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
                    if (highlightStarts.isNotEmpty()) {
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
                    if (filterStarts.isNotEmpty()) {
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
                    if (boldStarts.isNotEmpty()) {
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

            if (starts.isNotEmpty()) {
                if (newValue == value) {
                    return@accessLogItems ""
                }
                stringBuilder.replace(0, newValue.length, newValue.replace(" ", UNBREAKABLE_SPACE))
            } else {
                var beforeStart = 0
                var isFirst = true
                while (starts.isNotEmpty()) {
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
            return@accessLogItems stringBuilder.toString()
        }
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
        val selectedRow = logRepository.selectedRow

        logRepository.accessLogItems { logItems ->
            var startRow = 0
            var endRow = 0

            if (isNext) {
                startRow = selectedRow + 1
                endRow = logItems.count() - 1
                if (startRow >= endRow) {
                    mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
                    return@accessLogItems
                }
            } else {
                startRow = selectedRow - 1
                endRow = 0

                if (startRow < endRow) {
                    mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
                    return@accessLogItems
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
                if (logRepository is FullLogcatRepository) {
                    mainUI.splitLogPane.filteredLogPanel.goToRow(idxFound, 0)
                } else {
                    mainUI.splitLogPane.fullLogPanel.goToRow(idxFound, 0)
                }
            } else {
                mainUI.showSearchResultTooltip(isNext, "\"$searchFilterItem\" ${STRINGS.ui.notFound}")
            }
        }
    }

    override fun onLogChanged(event: LogRepository.LogChangeEvent) {
        when (event.type) {
            TableModelEvent.INSERT -> {
                fireTableRowsInserted(event.startRow, event.endRow)
            }

            TableModelEvent.UPDATE -> {
                GLog.d(TAG, "[onLogChanged] UPDATE")
                fireTableRowsUpdated(event.startRow, event.endRow)
            }

            TableModelEvent.DELETE -> {
                if (event.endRow == Int.MAX_VALUE) {
                    goToLast = true
                    BookmarkManager.clear()
                    fireTableDataChanged()
                    Runtime.getRuntime().gc()
                }
                fireTableRowsDeleted(event.startRow, event.endRow)
            }
        }
    }

    companion object {
        const val COLUMN_NUM = 0
        const val COLUMN_LOG = 1
        private const val TAG = "LogTableModel"
        private const val UNBREAKABLE_SPACE = "&nbsp;"
    }

}
