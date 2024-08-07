package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.Bindings
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.view.button.ColorToggleButton
import java.awt.Insets
import javax.swing.event.ListSelectionEvent

class FilteredLogPanel(
    tableModel: LogTableModel,
    private val basePanel: LogPanel,
    contexts: Contexts = Contexts.default
) : LogPanel(tableModel, contexts) {

    private val bookmarksBtn = ColorToggleButton(STRINGS.ui.bookmarks) applyTooltip STRINGS.toolTip.viewBookmarksToggle
    private val fullBtn = ColorToggleButton(STRINGS.ui.full) applyTooltip STRINGS.toolTip.viewFullToggle

    init {
        bookmarksBtn.margin = Insets(0, 3, 0, 3)
        fullBtn.margin = Insets(0, 3, 0, 3)

        createUI()

        observeViewModelProperty()

        bind(binding)
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

    override fun updateTableBar() {
        super.updateTableBar()
        ctrlMainPanel.add(fullBtn)
        ctrlMainPanel.add(bookmarksBtn)
        ctrlMainPanel.updateUI()
    }

    override fun bind(binding: LogPanelBinding) {
        super.bind(binding)
        binding.apply {
            Bindings.bind(selectedProperty(fullBtn), fullMode)
            Bindings.bind(selectedProperty(bookmarksBtn), bookmarkMode)
        }
    }

    override fun valueChanged(event: ListSelectionEvent) {
        super.valueChanged(event)
        val selectedLogNum = tableModel.getValueAt(table.selectedRow, 0).toString().trim().toInt()
        val baseSelectedLogNum = basePanel.tableModel.getValueAt(basePanel.table.selectedRow, 0).toString().trim().toInt()
        if (selectedLogNum != baseSelectedLogNum) {
            val targetRow = basePanel.tableModel.getRowIndexInAllPages(selectedLogNum)
            basePanel.goToRowIndex(targetRow)

            if (table.isLastRowSelected()) {
                setGoToLast(true)
            }
        }
    }
}
