package me.gegenbauer.catspy.log.ui.panel

import me.gegenbauer.catspy.common.ui.button.ColorToggleButton
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.Bindings
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.utils.applyTooltip
import java.awt.Insets
import java.awt.event.FocusListener
import javax.swing.event.ListSelectionEvent

class FilteredLogPanel(
    tableModel: LogTableModel,
    focusHandler: FocusListener,
    private val basePanel: LogPanel,
    contexts: Contexts = Contexts.default
) : LogPanel(tableModel, focusHandler, contexts) {

    private val bookmarksBtn = ColorToggleButton(STRINGS.ui.bookmarks) applyTooltip STRINGS.toolTip.viewBookmarksToggle
    private val fullBtn = ColorToggleButton(STRINGS.ui.full) applyTooltip STRINGS.toolTip.viewFullToggle

    init {
        bookmarksBtn.margin = Insets(0, 3, 0, 3)
        fullBtn.margin = Insets(0, 3, 0, 3)

        createUI()

        observeViewModelProperty()

        bind(viewModel)
    }

    private fun observeViewModelProperty() {
        viewModel.fullMode.addObserver {
            if (it == true) {
                viewModel.bookmarkMode.updateValue(false)
            }
            tableModel.logRepository.onFilterUpdate()
        }
        viewModel.bookmarkMode.addObserver {
            if (it == true) {
                viewModel.fullMode.updateValue(false)
            }
            tableModel.logRepository.onFilterUpdate()
        }
    }

    override fun updateTableBar() {
        super.updateTableBar()
        ctrlMainPanel.add(fullBtn)
        ctrlMainPanel.add(bookmarksBtn)
        ctrlMainPanel.updateUI()
    }

    override fun bind(viewModel: LogPanelViewModel) {
        super.bind(viewModel)
        viewModel.apply {
            Bindings.bind(selectedProperty(fullBtn), fullMode)
            Bindings.bind(selectedProperty(bookmarksBtn), bookmarkMode)
        }
    }

    override fun onListSelectionChanged(event: ListSelectionEvent) {
        super.onListSelectionChanged(event)
        val value = tableModel.getValueAt(table.selectedRow, 0)
        val selectedRow = value.toString().trim().toInt()

        val baseValue = basePanel.table.tableModel.getValueAt(basePanel.table.selectedRow, 0)
        val baseSelectedRow = baseValue.toString().trim().toInt()

        if (selectedRow != baseSelectedRow) {
            setGoToLast(false)
            basePanel.setGoToLast(false)
            basePanel.goToRowByNum(selectedRow, -1)
            tableModel.selectionChanged = true

            if (table.selectedRow == table.rowCount - 1) {
                setGoToLast(true)
            }
        }
    }

    companion object {
        private const val TAG = "FilterLogPanel"
    }
}