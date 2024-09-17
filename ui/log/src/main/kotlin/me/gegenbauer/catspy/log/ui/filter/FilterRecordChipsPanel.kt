package me.gegenbauer.catspy.log.ui.filter

import me.gegenbauer.catspy.log.filter.FilterRecord
import me.gegenbauer.catspy.view.chip.Chip
import me.gegenbauer.catspy.view.chip.OnChipClickedListener
import me.gegenbauer.catspy.view.panel.HorizontalFlexibleHeightLayout
import me.gegenbauer.catspy.view.panel.ScrollConstrainedScrollablePanel
import javax.swing.BorderFactory

class FilterRecordChipsPanel : ScrollConstrainedScrollablePanel(verticalScrollable = false),
    OnChipClickedListener {
    private val filterRecords = mutableListOf<FilterRecord>()
    private val filterChips = mutableListOf<Chip>()
    private val listeners = mutableListOf<FilterRecordEventListener>()

    init {
        border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        layout = HorizontalFlexibleHeightLayout(4)
    }

    fun setFilterRecords(record: List<FilterRecord>) {
        removeAll()
        filterRecords.clear()
        filterChips.clear()
        record.forEach { addFilterChip(it) }
    }

    fun addFilterRecordEventListener(listener: FilterRecordEventListener) {
        listeners.add(listener)
    }

    private fun notifyFilterRecordAdded(filterRecord: FilterRecord) {
        listeners.forEach { it.onFilterRecordAdded(filterRecord) }
    }

    private fun notifyFilterRecordRemoved(filterRecord: FilterRecord) {
        listeners.forEach { it.onFilterRecordRemoved(filterRecord) }
    }

    private fun notifyFilterRecordSelected(filterRecord: FilterRecord) {
        listeners.forEach { it.onFilterRecordSelected(filterRecord) }
    }

    private fun createFilterChip(filterRecord: FilterRecord): Chip {
        return Chip(filterRecord.name).apply {
            setTooltip(filterRecord.toString())
            setOnDeleteClicked {
                removeRecord(filterRecord)
            }
            setOnChipClickedListener(this@FilterRecordChipsPanel)
        }
    }

    fun addRecord(filterRecord: FilterRecord) {
        addFilterChip(filterRecord)
        notifyFilterRecordAdded(filterRecord)
    }

    fun removeRecord(filterRecord: FilterRecord) {
        val res = removeFilterChip(filterRecord.name)
        if (res) {
            notifyFilterRecordRemoved(filterRecord)
        }
    }

    private fun addFilterChip(filterRecord: FilterRecord) {
        val chip = createFilterChip(filterRecord)
        filterRecords.add(filterRecord)
        filterChips.add(chip)
        add(chip)
        revalidate()
        repaint()
    }

    private fun removeFilterChip(name: String): Boolean {
        val targetIndex = filterRecords.indexOfFirst { it.name == name }
        if (targetIndex != -1) {
            val targetRecord = filterRecords[targetIndex]
            filterRecords.remove(targetRecord)
            remove(filterChips[targetIndex])
            filterChips.removeAt(targetIndex)
            revalidate()
            repaint()
        }

        return targetIndex >= 0
    }

    override fun onChipClicked(chip: Chip) {
        val index = filterChips.indexOf(chip)
        if (index != -1) {
            notifyFilterRecordSelected(filterRecords[index])
        }
    }
}

fun interface OnFilterRecordSelectedListener {
    fun onRecordSelected(filterRecord: FilterRecord)
}

interface FilterRecordEventListener {
    fun onFilterRecordAdded(filterRecord: FilterRecord)

    fun onFilterRecordRemoved(filterRecord: FilterRecord)

    fun onFilterRecordSelected(filterRecord: FilterRecord)
}

fun interface FilterRecordProvider {
    fun getFilterRecord(name: String): FilterRecord
}