package me.gegenbauer.catspy.log.ui.search

import com.github.weisj.darklaf.iconset.AllIcons
import kotlinx.coroutines.Dispatchers
import me.gegenbauer.catspy.concurrency.IgnoreFastCallbackScheduler
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.*
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.filter.FilterProperty
import me.gegenbauer.catspy.log.filter.FilterProperty.Companion.FILTER_ID_MATCH_CASE
import me.gegenbauer.catspy.log.ui.filter.FilterPropertyObserver
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.*
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.button.IconBarButton
import me.gegenbauer.catspy.view.combobox.filterComboBox
import me.gegenbauer.catspy.view.filter.FilterItem
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JRadioButton

interface ISearchPanel {

    fun setOnSearchRequestReceivedListener(listener: SearchRequestReceivedListener)

    fun getSearchPanel(): SearchPanel

    fun requestFocusOnSearchEditor()

    fun moveToPrevSearchResult()

    fun moveToNextSearchResult()

    fun registerKeyEvents()

    interface SearchRequestReceivedListener {
        fun moveToPrevSearchResult(isFilteredLog: Boolean) {}

        fun moveToNextSearchResult(isFilteredLog: Boolean) {}
    }
}

interface ISearchFilterController {

    fun bind(searchPanel: SearchPanel)

    fun getSearchFilterItem(): FilterItem

    fun getSearchContentProperty(): FilterProperty

    fun addSearchContentObserver(observer: FilterPropertyObserver)

    fun removeSearchContentObserver(observer: FilterPropertyObserver)
}

class SearchFilterController : ISearchFilterController, FilterPropertyObserver {

    private val contentProperty = FilterProperty("SearchContent")
    private val matchCaseProperty =
        FilterProperty(GlobalStrings.MATCH_CASE, FILTER_ID_MATCH_CASE, storeKeyPrefix = "Search")
    private val visibilityProperty = ObservableValueProperty(false)
    private val ignoreFastCallbackScheduler = IgnoreFastCallbackScheduler(Dispatchers.UI, 2000)

    override fun bind(searchPanel: SearchPanel) {
        searchPanel.bindProperties(contentProperty, matchCaseProperty, visibilityProperty)
    }

    override fun getSearchFilterItem(): FilterItem {
        val filterItem = if (!visibilityProperty.getValueNonNull()) {
            FilterItem.EMPTY_ITEM
        } else {
            contentProperty.processToFilterItem(matchCaseProperty.enabled.getValueNonNull())
        }
        onSearchFilterChanged()
        return filterItem
    }

    override fun getSearchContentProperty(): FilterProperty {
        return contentProperty
    }

    private fun onSearchFilterChanged() {
        ignoreFastCallbackScheduler.schedule {
            contentProperty.addCurrentContentToList()
        }
    }

    override fun addSearchContentObserver(observer: FilterPropertyObserver) {
        contentProperty.addPropertyObserver(observer)
        matchCaseProperty.addPropertyObserver(observer)
    }

    override fun removeSearchContentObserver(observer: FilterPropertyObserver) {
        contentProperty.removePropertyObserver(observer)
        matchCaseProperty.removePropertyObserver(observer)
    }

    override fun onFilterPropertyChanged(property: ObservableValueProperty<*>) {
        contentProperty.processToFilterItem(matchCaseProperty.enabled.getValueNonNull())
    }
}

class SearchPanel(override val contexts: Contexts = Contexts.default) : JPanel(), ISearchPanel, Context {
    private val closeBtn = IconBarButton(AllIcons.Navigation.Close.get()) applyTooltip STRINGS.toolTip.searchCloseBtn
    private val searchCombo = filterComboBox()
    private val searchMatchCaseToggle = ColorToggleButton(GlobalStrings.MATCH_CASE) applyTooltip STRINGS.toolTip.searchCaseToggle

    private val upBtn = IconBarButton(GIcons.Action.Up.get()) applyTooltip STRINGS.toolTip.searchPrevBtn
    private val downBtn = IconBarButton(GIcons.Action.Down.get()) applyTooltip STRINGS.toolTip.searchNextBtn
    private val logPanelSelector = LogPanelSelector()

    private val contentPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
    private val statusPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 2))

    private val searchActionHandler = SearchActionHandler()
    private val searchKeyHandler = SearchKeyHandler()

    private var searchRequestReceivedListener: ISearchPanel.SearchRequestReceivedListener =
        object : ISearchPanel.SearchRequestReceivedListener {}

    init {
        configureUI()
        registerEvent()
    }

    private fun configureUI() {
        searchCombo.isEditable = true
        searchCombo.setWidth(400)

        contentPanel.add(searchCombo)
        contentPanel.add(searchMatchCaseToggle)
        contentPanel.add(upBtn)
        contentPanel.add(downBtn)
        contentPanel.add(logPanelSelector)

        statusPanel.add(closeBtn)

        layout = BorderLayout()
        add(contentPanel, BorderLayout.WEST)
        add(statusPanel, BorderLayout.EAST)
    }

    private fun registerEvent() {
        registerKeyEvents()
        upBtn.addActionListener(searchActionHandler)
        downBtn.addActionListener(searchActionHandler)
        closeBtn.addActionListener(searchActionHandler)

        registerStrokeWhenFocused(Key.ENTER, "Move to next search result") { moveToNextSearchResult() }
        registerStrokeWhenFocused(Key.S_ENTER, "Move to previous search result") { moveToPrevSearchResult() }
    }

    override fun setOnSearchRequestReceivedListener(listener: ISearchPanel.SearchRequestReceivedListener) {
        searchRequestReceivedListener = listener
    }

    override fun getSearchPanel(): SearchPanel {
        return this
    }

    override fun registerKeyEvents() {
        searchCombo.keyListener = searchKeyHandler
    }

    fun bindProperties(
        searchContentProperty: FilterProperty,
        matchCaseProperty: FilterProperty,
        visibilityProperty: ObservableValueProperty<Boolean>
    ) {
        textProperty(searchCombo.editorComponent) bindDual searchContentProperty.content
        listProperty(searchCombo) bindDual searchContentProperty.contentList
        selectedItemProperty(searchCombo) bindDual searchContentProperty.selectedItem
        selectedProperty(searchMatchCaseToggle) bindDual matchCaseProperty.enabled
        visibilityProperty(this) bindDual visibilityProperty
        customProperty<FilterItem>(searchCombo, "filterItem", FilterItem.EMPTY_ITEM) bindDual searchContentProperty.filterItem
    }

    override fun requestFocusOnSearchEditor() {
        super.requestFocus()
        searchCombo.requestFocus()
        searchCombo.editor.selectAll()
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)

        if (visible) {
            searchCombo.requestFocus()
            searchCombo.editor.selectAll()
        } else {
            searchCombo.editorComponent.text = ""
            searchCombo.hidePopup()
        }
    }

    override fun moveToNextSearchResult() {
        if (!isVisible) return
        searchRequestReceivedListener.moveToNextSearchResult(logPanelSelector.isFilteredLogSelected())
    }

    override fun moveToPrevSearchResult() {
        if (!isVisible) return
        searchRequestReceivedListener.moveToPrevSearchResult(logPanelSelector.isFilteredLogSelected())
    }

    private inner class SearchActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                upBtn -> {
                    moveToPrevSearchResult()
                }

                downBtn -> {
                    moveToNextSearchResult()
                }

                closeBtn -> {
                    isVisible = false
                }
            }
        }
    }

    private inner class SearchKeyHandler : KeyAdapter() {
        override fun keyReleased(event: KeyEvent) {
            if (event.keyEventInfo == Key.ENTER.released() || event.keyEventInfo == Key.S_ENTER.released()) {
                this@SearchPanel::moveToPrevSearchResult
                    .takeIf { KeyEvent.SHIFT_DOWN_MASK == event.modifiersEx }
                    ?.invoke() ?: this@SearchPanel::moveToNextSearchResult.invoke()
            }
        }
    }

    private class LogPanelSelector : JPanel() {
        private val fullLogRadioButton = JRadioButton(STRINGS.ui.fullLog)
        private val filteredLogRadioButton = JRadioButton(STRINGS.ui.filteredLog)
        private val group = ButtonGroup()

        fun isFilteredLogSelected(): Boolean {
            return filteredLogRadioButton.isSelected
        }

        init {
            layout = FlowLayout(FlowLayout.LEFT, 5, 2)
            add(fullLogRadioButton)
            add(filteredLogRadioButton)
            group.add(fullLogRadioButton)
            group.add(filteredLogRadioButton)
            group.setSelected(filteredLogRadioButton.model, true)
        }
    }
}
