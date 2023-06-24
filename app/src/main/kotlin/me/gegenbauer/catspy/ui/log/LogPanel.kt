package me.gegenbauer.catspy.ui.log

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.Bindings
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.databinding.bind.withName
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.manager.BookmarkChangeListener
import me.gegenbauer.catspy.manager.BookmarkManager
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.ColorScheme
import me.gegenbauer.catspy.ui.button.ColorToggleButton
import me.gegenbauer.catspy.ui.button.StatefulToggleButton
import me.gegenbauer.catspy.ui.container.WrapablePanel
import me.gegenbauer.catspy.ui.panel.VStatusPanel
import me.gegenbauer.catspy.ui.popup.PopUpLogPanel
import me.gegenbauer.catspy.utils.applyTooltip
import me.gegenbauer.catspy.utils.loadIcon
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Insets
import java.awt.Rectangle
import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.event.TableModelEvent


// TODO refactor
abstract class LogPanel(
    protected val tableModel: LogTableModel,
    override val contexts: Contexts = Contexts.default
) : JPanel(), Context {

    val table = LogTable(tableModel)
    protected val ctrlMainPanel: WrapablePanel = WrapablePanel() withName "ctrlMainPanel"

    private val firstCb =
        StatefulToggleButton(loadIcon<DerivableImageIcon>("top.png")) applyTooltip STRINGS.toolTip.viewFirstBtn
    private val lastCb =
        StatefulToggleButton(loadIcon<DerivableImageIcon>("bottom.png")) applyTooltip STRINGS.toolTip.viewLastBtn
    private val tagBtn = ColorToggleButton(STRINGS.ui.tag) applyTooltip STRINGS.toolTip.viewTagToggle
    private val pidBtn = ColorToggleButton(STRINGS.ui.pid) applyTooltip STRINGS.toolTip.viewPidToggle
    private val tidBtn = ColorToggleButton(STRINGS.ui.tid) applyTooltip STRINGS.toolTip.viewTidToggle

    private val scrollPane = JScrollPane(table)
    private val vStatusPanel = VStatusPanel()
    private val adjustmentHandler = AdjustmentHandler()
    private val listSelectionHandler = ListSelectionHandler()
    private val tableModelHandler = TableModelHandler()
    private val bookmarkHandler = BookmarkHandler()

    private val viewModel = LogPanelViewModel()

    private var oldLogVPos = -1
    private var oldLogHPos = -1

    init {
        layout = BorderLayout()
        Insets(2, 3, 1, 3).apply {
            firstCb.margin = this
            lastCb.margin = this
        }
        Insets(0, 3, 0, 3).apply {
            tagBtn.margin = this
            pidBtn.margin = this
            tidBtn.margin = this
        }

        viewModel.bind()

        viewModel.goToLast.addObserver { if (it == true) goToLast() }
        viewModel.goToFirst.addObserver { if (it == true) goToFirst() }
        viewModel.boldPid.addObserver {
            tableModel.boldPid = it == true
            table.repaint()
        }
        viewModel.boldTid.addObserver {
            tableModel.boldTid = it == true
            table.repaint()
        }
        viewModel.boldTag.addObserver {
            tableModel.boldTag = it == true
            table.repaint()
        }
    }

    private inner class LogPanelViewModel {
        val goToLast = ObservableViewModelProperty(false)
        val goToFirst = ObservableViewModelProperty(false)
        val boldPid = ObservableViewModelProperty(false)
        val boldTid = ObservableViewModelProperty(false)
        val boldTag = ObservableViewModelProperty(false)

        fun bind() {
            Bindings.bind(selectedProperty(firstCb), goToFirst)
            Bindings.bind(selectedProperty(lastCb), goToLast)
            Bindings.bind(selectedProperty(pidBtn), boldPid)
            Bindings.bind(selectedProperty(tidBtn), boldTid)
            Bindings.bind(selectedProperty(tagBtn), boldTag)
        }
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        table.setContexts(contexts)
        vStatusPanel.setContexts(contexts)

        contexts.getContext(LogMainUI::class.java)?.apply {
            val bookmarkManager = ServiceManager.getContextService(this, BookmarkManager::class.java)
            bookmarkManager.addBookmarkEventListener(bookmarkHandler)
        }
    }

    protected open fun createUI() {
        tableModel.addLogTableModelListener(tableModelHandler)
        table.columnSelectionAllowed = true
        table.selectionModel.addListSelectionListener(listSelectionHandler)
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
    }

    open fun updateTableBar() {
        ctrlMainPanel.removeAll()
        ctrlMainPanel.add(firstCb)
        ctrlMainPanel.add(lastCb)
        ctrlMainPanel.add(pidBtn)
        ctrlMainPanel.add(tidBtn)
        ctrlMainPanel.add(tagBtn)
    }

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
        viewModel.goToLast.updateValue(value)
    }

    fun setGoToFirst(value: Boolean) {
        viewModel.goToFirst.updateValue(value)
    }

    fun getGoToLast(): Boolean {
        return viewModel.goToLast.getValueNonNull()
    }

    fun goToFirst() {
        setGoToLast(false)
        goToRow(0, -1)
        updateTableUI()
        return
    }

    fun goToLast() {
        setGoToFirst(false)
        if (table.rowCount > 0) {
            goToRow(table.rowCount - 1, -1)
            updateTableUI()
        }
        return
    }

    fun updateTableUI() {
        table.updateUI()
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

    internal inner class ListSelectionHandler : ListSelectionListener {
        override fun valueChanged(event: ListSelectionEvent) {
            onListSelectionChanged(event)
            return
        }
    }

    protected open fun onListSelectionChanged(event: ListSelectionEvent) {
        // Empty implementation
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

    internal inner class MouseHandler : MouseAdapter() {

        private val popupMenu: JPopupMenu = PopUpLogPanel()

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
