package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.Bindings
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.BookmarkChangeListener
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.view.button.IconBarButton
import me.gegenbauer.catspy.view.button.IconBarToggleButton
import me.gegenbauer.catspy.view.container.WrapablePanel
import me.gegenbauer.catspy.view.table.PageIndicator
import me.gegenbauer.catspy.view.table.PageMetadata
import me.gegenbauer.catspy.view.table.RowNavigation
import me.gegenbauer.catspy.view.table.isAtLastPage
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.event.TableModelEvent

open class LogPanel(
    val tableModel: LogTableModel,
    override val contexts: Contexts = Contexts.default,
    val table: LogTable = LogTable(tableModel)
) : JPanel(), Context, ListSelectionListener, RowNavigation by table {

    val binding = LogPanelBinding()
    protected val ctrlMainPanel = WrapablePanel()
    protected val buttonMargin = Insets(0, 3, 0, 3)

    private val topBtn = IconBarButton(GIcons.Action.Top.get()) applyTooltip STRINGS.toolTip.viewFirstBtn
    private val bottomBtn = IconBarButton(GIcons.Action.Bottom.get()) applyTooltip STRINGS.toolTip.viewLastBtn
    private val scrollToEndIcon = GIcons.State.ScrollEnd.get(24, 24)
    private val scrollToEndSelectedIcon = GIcons.State.ScrollEnd.selected(24, 24)
    private val scrollToEndBtn = IconBarToggleButton(scrollToEndIcon, scrollToEndSelectedIcon).apply {
        toolTipText = STRINGS.toolTip.keepScrollToEnd
    }

    private val scrollPane = LogScrollPane(table)
    private val vStatusPanel = VStatusPanel()
    private val pageNavigationPanel = PageIndicator(tableModel)
    private val adjustmentHandler = AdjustmentHandler()
    private val tableModelHandler = TableModelHandler()
    private val bookmarkHandler = BookmarkHandler()

    private var lastPosition = -1
    private var lastPageMetaData: PageMetadata = PageMetadata()

    init {
        registerEvent()

        observeViewModelProperty()
    }

    private fun registerEvent() {
        topBtn.addActionListener { moveToFirstRow() }
        bottomBtn.addActionListener { moveToLastRow() }
        tableModel.addLogTableModelListener(tableModelHandler)
        scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentHandler)
        scrollPane.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                table.onReceiveMouseReleaseEvent(e)
                super.mouseReleased(e)
            }
        })
    }

    private fun observeViewModelProperty() {
        binding.scrollToEnd.addObserver {
            if (it == true) {
                scrollToEnd()
            }
        }
        tableModel.pageMetadata.addObserver {
            it ?: return@addObserver
            if (lastPageMetaData.isIndicatorDataEquals(it)) {
                return@addObserver
            }
            if (lastPageMetaData.isAtLastPage && it.isAtLastPage.not()) {
                binding.scrollToEnd.updateValue(false)
            }
            lastPageMetaData = it
        }
    }

    class LogPanelBinding {
        val scrollToEnd = ObservableValueProperty(true)
        val fullMode = ObservableValueProperty(false)
        val bookmarkMode = ObservableValueProperty(false)
        val showFullLogBtnEnabled = ObservableValueProperty(true)
    }

    protected open fun bind(binding: LogPanelBinding) {
        binding.apply {
            Bindings.bind(selectedProperty(scrollToEndBtn), scrollToEnd)
        }
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        table.setParent(this)
        vStatusPanel.setParent(this)

        getBookmarkManager()?.addBookmarkEventListener(bookmarkHandler)
    }

    private fun getBookmarkManager(): BookmarkManager? {
        return contexts.getContext(BaseLogMainPanel::class.java)?.let {
            ServiceManager.getContextService(it, BookmarkManager::class.java)
        }
    }

    protected open fun createUI() {
        layout = BorderLayout()
        scrollPane.minimumSize = Dimension(0, 0)
        buttonMargin.apply {
            topBtn.margin = this
            bottomBtn.margin = this
            scrollToEndBtn.margin = this
        }
        ctrlMainPanel.border = BorderFactory.createEmptyBorder(0, 4, 0, 0)

        table.columnSelectionAllowed = true
        table.listSelectionHandler = this
        scrollPane.verticalScrollBar.unitIncrement = 20

        add(ctrlMainPanel, BorderLayout.NORTH)
        add(vStatusPanel, BorderLayout.WEST)
        add(scrollPane, BorderLayout.CENTER)
        add(pageNavigationPanel, BorderLayout.SOUTH)
    }

    open fun createTableBar() {
        ctrlMainPanel.add(topBtn)
        ctrlMainPanel.add(bottomBtn)
        ctrlMainPanel.add(scrollToEndBtn)
    }

    fun goToRowIndex(rowIndex: Int, setSelected: Boolean = true) {
        table.moveRowToCenter(rowIndex, setSelected)
    }

    fun setGoToLast(value: Boolean) {
        binding.scrollToEnd.updateValue(value)
    }

    override fun scrollToEnd() {
        scrollPane.verticalScrollBar.removeAdjustmentListener(adjustmentHandler)
        table.scrollToEnd()
        scrollPane.verticalScrollBar.addAdjustmentListener(adjustmentHandler)
    }

    override fun moveToFirstRow() {
        table.moveToFirstRow()
        setGoToLast(false)
    }

    override fun moveToLastRow() {
        table.moveToLastRow()
    }

    override fun destroy() {
        super.destroy()
        getBookmarkManager()?.removeBookmarkEventListener(bookmarkHandler)
    }

    internal inner class AdjustmentHandler : AdjustmentListener {
        override fun adjustmentValueChanged(event: AdjustmentEvent) {
            val currentPosition = event.value
            if (lastPosition < 0) {
                lastPosition = currentPosition
                return
            }

            val isScrollingDown = currentPosition - lastPosition >= 0
            takeIf { currentPosition != lastPosition } ?: return
            lastPosition = currentPosition

            val scrollBar = event.adjustable as JScrollBar
            val extent = scrollBar.model.extent
            val maximum = scrollBar.model.maximum
            if (!isScrollingDown) {
                setGoToLast(false)
            }

            val valueIsAtMaximum = (event.value + extent) >= maximum
            if (valueIsAtMaximum && tableModel.isAtLastPage) {
                setGoToLast(true)
            }
            vStatusPanel.repaint()
        }
    }

    internal inner class TableModelHandler : LogTableModelListener {

        override fun onLogDataChanged(event: TableModelEvent) {
            lastPosition = -1

            if (binding.scrollToEnd.value == true) {
                scrollToEnd()
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
}
