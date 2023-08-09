package me.gegenbauer.catspy.log.ui.panel

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.catspy.common.configuration.UIConfManager
import me.gegenbauer.catspy.common.support.ColorScheme
import me.gegenbauer.catspy.common.ui.button.ColorToggleButton
import me.gegenbauer.catspy.common.ui.button.StatefulToggleButton
import me.gegenbauer.catspy.common.ui.container.WrapablePanel
import me.gegenbauer.catspy.common.ui.table.PageIndicator
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.Bindings
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.databinding.bind.withName
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.log.BookmarkChangeListener
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.log.ui.table.LogTable
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.log.ui.table.LogTableModelListener
import me.gegenbauer.catspy.common.ui.table.RowNavigation
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.utils.applyTooltip
import me.gegenbauer.catspy.utils.loadIcon
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import java.awt.event.FocusListener
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.JScrollPane
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.event.TableModelEvent


abstract class LogPanel(
    val tableModel: LogTableModel,
    private val focusChangeListener: FocusListener,
    override val contexts: Contexts = Contexts.default,
    val table: LogTable = LogTable(tableModel)
) : JPanel(), Context, ListSelectionListener, RowNavigation by table {

    val viewModel = LogPanelViewModel()
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
    private val pageNavigationPanel = PageIndicator(tableModel)
    private val adjustmentHandler = AdjustmentHandler()
    private val tableModelHandler = TableModelHandler()
    private val bookmarkHandler = BookmarkHandler()

    private var lastPosition = -1

    init {
        layout = BorderLayout()
        scrollPane.minimumSize = Dimension(0, 0)
        Insets(2, 3, 1, 3).apply {
            firstCb.margin = this
            lastCb.margin = this
        }
        Insets(0, 3, 0, 3).apply {
            tagBtn.margin = this
            pidBtn.margin = this
            tidBtn.margin = this
        }
        observeViewModelProperty()
    }

    private fun observeViewModelProperty() {
        viewModel.goToLast.addObserver { if (it == true) moveToLastRow() }
        viewModel.goToFirst.addObserver { if (it == true) moveToFirstRow() }
        viewModel.boldPid.addObserver {
            table.repaint()
        }
        viewModel.boldTid.addObserver {
            table.repaint()
        }
        viewModel.boldTag.addObserver {
            table.repaint()
        }
    }

    class LogPanelViewModel {
        val goToLast = ObservableViewModelProperty(false)
        val goToFirst = ObservableViewModelProperty(false)
        val boldPid = ObservableViewModelProperty(false)
        val boldTid = ObservableViewModelProperty(false)
        val boldTag = ObservableViewModelProperty(false)
        val fullMode = ObservableViewModelProperty(false)
        val bookmarkMode = ObservableViewModelProperty(false)
    }

    protected open fun bind(viewModel: LogPanelViewModel) {
        viewModel.apply {
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

        contexts.getContext(LogTabPanel::class.java)?.apply {
            val bookmarkManager = ServiceManager.getContextService(this, BookmarkManager::class.java)
            bookmarkManager.addBookmarkEventListener(bookmarkHandler)
        }
    }

    protected open fun createUI() {
        table.addFocusListener(focusChangeListener)
        tableModel.addLogTableModelListener(tableModelHandler)
        table.columnSelectionAllowed = true
        table.listSelectionHandler = this
        scrollPane.verticalScrollBar.unitIncrement = 20

        scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentHandler)

        val ctrlPanel = JPanel()
        ctrlPanel.layout = BoxLayout(ctrlPanel, BoxLayout.Y_AXIS)
        ctrlPanel.add(ctrlMainPanel)

        add(ctrlPanel, BorderLayout.NORTH)
        add(vStatusPanel, BorderLayout.WEST)
        add(scrollPane, BorderLayout.CENTER)
        add(pageNavigationPanel, BorderLayout.SOUTH)
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

    fun goToLineIndex(logNum: Int) {
        table.moveToRow(logNum)
    }

    fun setGoToLast(value: Boolean) {
        viewModel.goToLast.updateValue(value)
    }

    fun setGoToFirst(value: Boolean) {
        viewModel.goToFirst.updateValue(value)
    }

    override fun moveToFirstRow() {
        table.moveToFirstRow()
        setGoToLast(false)
    }

    override fun moveToLastRow() {
        table.moveToLastRow()
        setGoToFirst(false)
    }

    internal inner class AdjustmentHandler : AdjustmentListener {
        override fun adjustmentValueChanged(event: AdjustmentEvent) {
            val currentPosition = event.value
            val isScrollingDown = currentPosition - lastPosition > 0
            val isScrollingUp = currentPosition - lastPosition < 0
            takeIf { currentPosition != lastPosition } ?: return
            lastPosition = currentPosition

            val scrollBar = event.adjustable as JScrollBar
            val extent = scrollBar.model.extent
            val maximum = scrollBar.model.maximum
            val minimum = scrollBar.model.minimum
            false.takeUnless { isScrollingDown }?.let {
                setGoToLast(false)
            }
            false.takeUnless { isScrollingUp }?.let {
                setGoToFirst(false)
            }

            val valueIsAtMaximum = (event.value + extent) >= maximum
            val valueIsAtMinimum = event.value <= minimum
            true.takeIf { valueIsAtMaximum }?.let { 
                if (tableModel.currentPage == tableModel.pageCount - 1) {
                    setGoToLast(true)
                }
            }
            true.takeIf { valueIsAtMinimum }?.let { 
                if (tableModel.currentPage == 0) {
                    setGoToFirst(true)
                }
            }
            vStatusPanel.repaint()
        }
    }

    internal inner class TableModelHandler : LogTableModelListener {

        override fun onLogDataChanged(event: TableModelEvent) {
            if ((event.source as LogTableModel).rowCount == 0) {
                lastPosition = -1
            }
        }
    }

    override fun valueChanged(event: ListSelectionEvent) {
        // Empty implementation
    }

    internal inner class BookmarkHandler : BookmarkChangeListener {
        override fun bookmarkChanged() {
            vStatusPanel.repaint()
            table.repaint()
        }
    }

    companion object {
        private const val TAG = "LogPanel"
    }
}
