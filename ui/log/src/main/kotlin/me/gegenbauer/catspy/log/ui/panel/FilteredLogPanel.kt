package me.gegenbauer.catspy.log.ui.panel

import me.gegenbauer.catspy.common.ui.button.ColorToggleButton
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.utils.applyTooltip
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusListener
import javax.swing.event.ListSelectionEvent

class FilteredLogPanel(
    tableModel: LogTableModel,
    private val focusHandler: FocusListener,
    private val basePanel: LogPanel,
    contexts: Contexts = Contexts.default
) : LogPanel(tableModel, contexts) {

    private val bookmarksBtn = ColorToggleButton(STRINGS.ui.bookmarks) applyTooltip STRINGS.toolTip.viewBookmarksToggle
    private val fullBtn = ColorToggleButton(STRINGS.ui.full) applyTooltip STRINGS.toolTip.viewFullToggle
    private val actionHandler = ActionHandler()

    init {
        bookmarksBtn.margin = Insets(0, 3, 0, 3)
        bookmarksBtn.addActionListener(actionHandler)
        fullBtn.margin = Insets(0, 3, 0, 3)
        fullBtn.addActionListener(actionHandler)

        createUI()
    }

    override fun createUI() {
        super.createUI()
        table.addFocusListener(focusHandler)
    }

    override fun updateTableBar() {
        super.updateTableBar()
        ctrlMainPanel.add(fullBtn)
        ctrlMainPanel.add(bookmarksBtn)
        ctrlMainPanel.updateUI()
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

    private inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                bookmarksBtn -> {
                    val selected = bookmarksBtn.model.isSelected
                    if (selected) {
                        fullBtn.model.isSelected = false
                    }
                    tableModel.bookmarkMode = selected
                    table.repaint()
                }

                fullBtn -> {
                    val selected = fullBtn.model.isSelected
                    if (selected) {
                        bookmarksBtn.model.isSelected = false
                    }
                    tableModel.fullMode = selected
                    table.repaint()
                }
            }
        }
    }

    companion object {
        private const val TAG = "FilterLogPanel"
    }
}