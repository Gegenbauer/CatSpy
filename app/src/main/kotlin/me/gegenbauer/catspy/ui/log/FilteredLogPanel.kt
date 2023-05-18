package me.gegenbauer.catspy.ui.log

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.catspy.manager.CustomListManager
import me.gegenbauer.catspy.manager.FiltersManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.button.ColorToggleButton
import me.gegenbauer.catspy.ui.button.TableBarButton
import me.gegenbauer.catspy.utils.addVSeparator1
import me.gegenbauer.catspy.utils.applyTooltip
import me.gegenbauer.catspy.utils.loadIcon
import java.awt.Insets
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusListener
import javax.swing.event.ListSelectionEvent
import javax.swing.event.TableModelEvent

class FilteredLogPanel(
    mainUI: MainUI,
    tableModel: LogTableModel,
    private val focusHandler: FocusListener,
    private val basePanel: LogPanel
) : LogPanel(mainUI, tableModel) {

    private val bookmarksBtn = ColorToggleButton(STRINGS.ui.bookmarks) applyTooltip STRINGS.toolTip.viewBookmarksToggle
    private val fullBtn = ColorToggleButton(STRINGS.ui.full) applyTooltip STRINGS.toolTip.viewFullToggle
    private val filtersManager = FiltersManager(mainUI, this)
    private val actionHandler = ActionHandler()

    init {
        bookmarksBtn.margin = Insets(0, 3, 0, 3)
        bookmarksBtn.addActionListener(actionHandler)
        fullBtn.margin = Insets(0, 3, 0, 3)
        fullBtn.addActionListener(actionHandler)

        createUI()
    }

    private fun updateTableBarFilters(customArray: ArrayList<CustomListManager.CustomElement>) {
        ctrlMainPanel.add(getCustomActionButton(customArray))

        val icon = loadIcon<DerivableImageIcon>("filterscmdsitem.png")
        customArray.filter { it.tableBar }.forEach { item ->
            val button = TableBarButton(item.title) applyTooltip "<html>${item.title} " +
                    ": <b>\"${item.value}\"</b><br><br>* Append : Ctrl + Click</html>"
            button.icon = icon
            button.value = item.value
            button.margin = Insets(0, 3, 0, 3)
            button.addActionListener(customBtActionListener)
            ctrlMainPanel.add(button)
        }
    }

    override fun createUI() {
        super.createUI()
        table.addFocusListener(focusHandler)
    }

    private val customBtActionListener = ActionListener { event ->
        if ((ActionEvent.CTRL_MASK and event.modifiers) != 0) {
            val filterText = mainUI.getTextShowLogCombo()
            if (filterText.isEmpty()) {
                mainUI.setTextShowLogCombo((event.source as TableBarButton).value)
            } else {
                if (filterText.substring(filterText.length - 1) == "|") {
                    mainUI.setTextShowLogCombo(filterText + (event.source as TableBarButton).value)
                } else {
                    mainUI.setTextShowLogCombo(filterText + "|" + (event.source as TableBarButton).value)
                }
            }
        } else {
            mainUI.setTextShowLogCombo((event.source as TableBarButton).value)
        }
        mainUI.applyShowLogCombo()
    }

    override fun updateTableBar(customArray: ArrayList<CustomListManager.CustomElement>) {
        super.updateTableBar(customArray)
        ctrlMainPanel.add(fullBtn)
        ctrlMainPanel.add(bookmarksBtn)
        ctrlMainPanel.addVSeparator1(10)
        updateTableBarFilters(customArray)
        ctrlMainPanel.updateUI()
    }

    override fun getCustomActionButton(customArray: ArrayList<CustomListManager.CustomElement>): TableBarButton {
        val filtersBtn = TableBarButton(STRINGS.ui.filters) applyTooltip STRINGS.toolTip.addFilterBtn
        filtersBtn.icon = loadIcon("filterscmds.png")
        filtersBtn.margin = Insets(0, 3, 0, 3)
        filtersBtn.addActionListener {
            filtersManager.showDialog()
        }
        return filtersBtn
    }

    override fun onTableFilterStateChanged(event: TableModelEvent) {
        super.onTableFilterStateChanged(event)
        val selectedLine = mainUI.getMarkLine()
        if (selectedLine >= 0) {
            var num: Int
            for (idx in 0 until table.rowCount) {
                num = table.getValueAt(idx, 0).toString().trim().toInt()
                if (selectedLine <= num) {
                    table.setRowSelectionInterval(idx, idx)
                    val viewRect: Rectangle = table.getCellRect(idx, 0, true)
                    table.scrollRectToVisible(viewRect)
                    table.scrollRectToVisible(viewRect) // sometimes not work
                    break
                }
            }
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