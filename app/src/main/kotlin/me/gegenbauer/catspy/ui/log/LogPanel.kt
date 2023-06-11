package me.gegenbauer.catspy.ui.log

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.databinding.bind.withName
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.manager.BookmarkChangeListener
import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.manager.CustomListManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.ColorScheme
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.button.ColorToggleButton
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.ui.button.TableBarButton
import me.gegenbauer.catspy.ui.container.WrapablePanel
import me.gegenbauer.catspy.ui.panel.VStatusPanel
import me.gegenbauer.catspy.ui.popup.PopUpLogPanel
import me.gegenbauer.catspy.utils.applyTooltip
import me.gegenbauer.catspy.utils.loadIcon
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Insets
import java.awt.Rectangle
import java.awt.event.*
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.event.TableModelEvent


// TODO refactor
abstract class LogPanel(
    val mainUI: MainUI,
    protected val tableModel: LogTableModel
) : JPanel() {
    val table = LogTable(tableModel)
    protected val ctrlMainPanel: WrapablePanel = WrapablePanel() withName "ctrlMainPanel"

    private val firstBtn = GButton(loadIcon<DerivableImageIcon>("top.png")) applyTooltip STRINGS.toolTip.viewFirstBtn
    private val lastBtn = GButton(loadIcon<DerivableImageIcon>("bottom.png")) applyTooltip STRINGS.toolTip.viewLastBtn
    private val tagBtn = ColorToggleButton(STRINGS.ui.tag) applyTooltip STRINGS.toolTip.viewTagToggle
    private val pidBtn = ColorToggleButton(STRINGS.ui.pid) applyTooltip STRINGS.toolTip.viewPidToggle
    private val tidBtn = ColorToggleButton(STRINGS.ui.tid) applyTooltip STRINGS.toolTip.viewTidToggle

    private val scrollPane = JScrollPane(table)
    private val vStatusPanel = VStatusPanel(table)
    private val adjustmentHandler = AdjustmentHandler()
    private val listSelectionHandler = ListSelectionHandler()
    private val tableModelHandler = TableModelHandler()
    private val actionHandler = ActionHandler()
    private val bookmarkHandler = BookmarkHandler()
    private val componentHandler = ComponentHandler()

    private var oldLogVPos = -1
    private var oldLogHPos = -1

    init {
        layout = BorderLayout()
        Insets(2, 3, 1, 3).apply {
            firstBtn.margin = this
            lastBtn.margin = this
        }
        Insets(0, 3, 0, 3).apply {
            tagBtn.margin = this
            pidBtn.margin = this
            tidBtn.margin = this
        }
        firstBtn.addActionListener(actionHandler)
        lastBtn.addActionListener(actionHandler)
        tagBtn.addActionListener(actionHandler)
        pidBtn.addActionListener(actionHandler)
        tidBtn.addActionListener(actionHandler)
    }

    protected open fun createUI() {
        updateTableBar(arrayListOf())
        tableModel.addLogTableModelListener(tableModelHandler)
        table.columnSelectionAllowed = true
        table.selectionModel.addListSelectionListener(listSelectionHandler)
        BookmarkManager.addBookmarkEventListener(bookmarkHandler)
        scrollPane.verticalScrollBar.unitIncrement = 20

        scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentHandler)
        scrollPane.horizontalScrollBar.addAdjustmentListener(adjustmentHandler)
        scrollPane.addMouseListener(MouseHandler())

        val ctrlPanel = JPanel()
        ctrlPanel.layout = BoxLayout(ctrlPanel, BoxLayout.Y_AXIS)
        ctrlPanel.add(ctrlMainPanel)

        add(ctrlPanel, BorderLayout.NORTH)
        add(vStatusPanel, BorderLayout.WEST)
        add(scrollPane, BorderLayout.CENTER)

        addComponentListener(componentHandler)
    }

    open fun updateTableBar(customArray: ArrayList<CustomListManager.CustomElement>) {
        ctrlMainPanel.removeAll()
        ctrlMainPanel.add(firstBtn)
        ctrlMainPanel.add(lastBtn)
        ctrlMainPanel.add(pidBtn)
        ctrlMainPanel.add(tidBtn)
        ctrlMainPanel.add(tagBtn)
    }

    protected abstract fun getCustomActionButton(customArray: ArrayList<CustomListManager.CustomElement>): TableBarButton

    var customFont: Font = Font(
        UIConfManager.uiConf.logFontName,
        UIConfManager.uiConf.logFontStyle,
        UIConfManager.uiConf.logFontSize
    )
        set(value) {
            field = value
            table.font = value
            table.rowHeight = value.size + 4

            repaint()
        }

    override fun repaint() {
        background = ColorScheme.logBG
        super.repaint()
    }

    fun goToRow(idx: Int, column: Int) {
        if (idx < 0 || idx >= table.rowCount) {
            GLog.d(TAG, "[goToRow] invalid idx")
            return
        }
        table.setRowSelectionInterval(idx, idx)
        val viewRect: Rectangle
        if (column < 0) {
            viewRect = table.getCellRect(idx, 0, true)
            viewRect.x = table.visibleRect.x
        } else {
            viewRect = table.getCellRect(idx, column, true)
        }
        table.scrollRectToVisible(viewRect)
    }

    fun goToRowByNum(num: Int, column: Int) {
        val firstNum = table.getValueAt(0, 0).toString().trim().toInt()
        val idx = (num - firstNum).coerceAtLeast(0)
        goToRow(idx, column)
    }

    fun setGoToLast(value: Boolean) {
        tableModel.goToLast = value
    }

    fun getGoToLast(): Boolean {
        return tableModel.goToLast
    }

    fun goToFirst() {
        setGoToLast(false)
        goToRow(0, -1)
        updateTableUI()
        return
    }

    fun goToLast() {
        if (table.rowCount > 0) {
            goToRow(table.rowCount - 1, -1)
            setGoToLast(true)
            updateTableUI()
        }
        return
    }

    fun updateTableUI() {
        table.updateUI()
    }

    fun getSelectedLine(): Int {
        return table.getValueAt(table.selectedRow, 0).toString().trim().toInt()
    }

    internal inner class AdjustmentHandler : AdjustmentListener {
        override fun adjustmentValueChanged(event: AdjustmentEvent) {
            if (event.source == scrollPane.verticalScrollBar) {
                val vPos = scrollPane.verticalScrollBar.value
                if (vPos != oldLogVPos) {
                    if (vPos < oldLogVPos && getGoToLast()) {
                        setGoToLast(false)
                    } else if (vPos > oldLogVPos
                        && !getGoToLast()
                        && (vPos + scrollPane.verticalScrollBar.size.height) == scrollPane.verticalScrollBar.maximum
                    ) {
                        setGoToLast(true)
                    }
                    oldLogVPos = vPos
                    vStatusPanel.repaint()
                }
            } else if (event.source == scrollPane.horizontalScrollBar) {
                val hPos = scrollPane.horizontalScrollBar.value
                if (hPos != oldLogHPos) {
                    oldLogHPos = hPos
                }
            }

        }
    }

    internal inner class TableModelHandler : LogTableModelListener {

        override fun tableChanged(event: TableModelEvent) {
            if ((event.source as LogTableModel).rowCount == 0) {
                oldLogVPos = -1
            } else {
                tableChangedInternal(event)
            }
        }

        private fun tableChangedInternal(event: TableModelEvent) {
            updateTableUI()
            table.updateColumnWidth(this@LogPanel.width, scrollPane.verticalScrollBar.width)
            onTableContentChanged(event)
        }
    }

    protected fun onTableContentChanged(event: TableModelEvent) {
        if (getGoToLast() && table.rowCount > 0) {
            val viewRect = table.getCellRect(table.rowCount - 1, 0, true)
            viewRect.x = table.visibleRect.x
            table.scrollRectToVisible(viewRect)
        }
    }

    protected open fun onTableFilterStateChanged(event: TableModelEvent) {
        // Empty implementation
    }

    internal inner class ListSelectionHandler : ListSelectionListener {
        override fun valueChanged(event: ListSelectionEvent) {
            onListSelectionChanged(event)
            return
        }
    }

    protected open fun onListSelectionChanged(event: ListSelectionEvent) {
        // Empty implementation
    }

    internal inner class ActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                firstBtn -> {
                    goToFirst()
                }

                lastBtn -> {
                    goToLast()
                }

                tagBtn -> {
                    val selected = tagBtn.model.isSelected
                    tableModel.boldTag = selected
                    table.repaint()
                }

                pidBtn -> {
                    val selected = pidBtn.model.isSelected
                    tableModel.boldPid = selected
                    table.repaint()
                }

                tidBtn -> {
                    val selected = tidBtn.model.isSelected
                    tableModel.boldTid = selected
                    table.repaint()
                }
            }
        }
    }

    internal inner class BookmarkHandler : BookmarkChangeListener {
        override fun bookmarkChanged() {
            vStatusPanel.repaint()
            if (tableModel.bookmarkMode) {
                tableModel.bookmarkMode = true
            }
            table.repaint()
        }
    }

    internal inner class ComponentHandler : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
            table.updateColumnWidth(e.component.width, scrollPane.verticalScrollBar.width)
            super.componentResized(e)
        }
    }

    internal inner class MouseHandler : MouseAdapter() {

        private val popupMenu: JPopupMenu = PopUpLogPanel(mainUI)

        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                popupMenu.show(event.component, event.x, event.y)
            } else {
                popupMenu.isVisible = false
            }

            super.mouseReleased(event)
        }
    }

    companion object {
        private const val TAG = "LogPanel"
    }
}
