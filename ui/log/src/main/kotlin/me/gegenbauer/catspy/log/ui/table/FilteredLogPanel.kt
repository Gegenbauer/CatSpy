package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.enabledProperty
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.log.event.FullLogVisibilityChangedEvent
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.event.EventManager
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.view.button.ColorToggleButton
import javax.swing.JLabel
import javax.swing.event.ListSelectionEvent

class FilteredLogPanel(
    tableModel: LogTableModel,
    private val basePanel: LogPanel,
    contexts: Contexts = Contexts.default
) : LogPanel(tableModel, contexts) {

    private val bookmarksBtn = ColorToggleButton(STRINGS.ui.bookmarks) applyTooltip STRINGS.toolTip.viewBookmarksToggle
    private val fullBtn = ColorToggleButton(STRINGS.ui.full) applyTooltip STRINGS.toolTip.viewFullToggle
    private val showFullLogBtn = ColorToggleButton(STRINGS.ui.showFullLogBtn) applyTooltip STRINGS.toolTip.showFullLog

    private val eventManager: EventManager
        get() = kotlin.run {
            val logMainPanel = contexts.getContext(BaseLogMainPanel::class.java)
                ?: error("No BaseLogMainPanel found in contexts")
            ServiceManager.getContextService(logMainPanel, EventManager::class.java)
        }

    init {
        bookmarksBtn.margin = buttonMargin
        fullBtn.margin = buttonMargin
        showFullLogBtn.margin = buttonMargin
        showFullLogBtn.isSelected = true

        createUI()

        observeViewModelProperty()

        bind(binding)

        showFullLogBtn.addActionListener {
            eventManager.publish(FullLogVisibilityChangedEvent(showFullLogBtn.isSelected))
        }
    }

    private fun observeViewModelProperty() {
        binding.fullMode.addObserver {
            if (it == true) {
                binding.bookmarkMode.updateValue(false)
            }
        }
        binding.bookmarkMode.addObserver {
            if (it == true) {
                binding.fullMode.updateValue(false)
            }
        }
    }

    override fun createTableBar() {
        ctrlMainPanel.add(JLabel(STRINGS.ui.filteredLog))
        super.createTableBar()
        ctrlMainPanel.add(fullBtn)
        ctrlMainPanel.add(bookmarksBtn)
        ctrlMainPanel.add(showFullLogBtn)
        ctrlMainPanel.updateUI()
    }

    override fun bind(binding: LogPanelBinding) {
        super.bind(binding)
        binding.apply {
            selectedProperty(fullBtn) bindDual fullMode
            selectedProperty(bookmarksBtn) bindDual bookmarkMode
            enabledProperty(showFullLogBtn) bindDual showFullLogBtnEnabled
        }
    }

    override fun valueChanged(event: ListSelectionEvent) {
        super.valueChanged(event)
        val selectedLogNum = tableModel.getValueAt(table.selectedRow, 0).toString().trim().toInt()
        val baseSelectedLogNum = basePanel.tableModel
            .getValueAt(basePanel.table.selectedRow, 0)
            .toString()
            .trim()
            .toInt()
        if (selectedLogNum != baseSelectedLogNum) {
            val targetRow = basePanel.tableModel.getRowIndexInAllPages(selectedLogNum)
            basePanel.goToRowIndex(targetRow)

            if (table.isLastRowSelected()) {
                setGoToLast(true)
            }
        }
    }
}
