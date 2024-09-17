package me.gegenbauer.catspy.log.ui.filter

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.filter.FilterRecord
import me.gegenbauer.catspy.utils.persistence.Preferences
import me.gegenbauer.catspy.utils.persistence.UserPreferences
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel
import javax.swing.JScrollPane

interface IFavoriteFilterPanel {
    fun getFavoriteFilterPanel(): JPanel

    fun setCurrentFilterRecordProvider(provider: FilterRecordProvider)
}

class FavoriteFilterPanel(override val contexts: Contexts = Contexts.default) : JPanel(), IFavoriteFilterPanel,
    FilterRecordEventListener, UserPreferences.PreferencesChangeListener, Context {

    override val key: String = KEY_FILTER_RECORDS

    private val filterRecordChipsPanel = FilterRecordChipsPanel()
    private val chipsScrollPane = JScrollPane(filterRecordChipsPanel)
    private val filterRecordActionsPanel = FilterRecordActionsPanel()
    private var currentFilterRecordProvider: FilterRecordProvider? = null
    private var onFilterRecordSelectedListener: OnFilterRecordSelectedListener? = null

    init {
        layout = BorderLayout()
        chipsScrollPane.border = null
        add(chipsScrollPane, BorderLayout.CENTER)
        add(filterRecordActionsPanel, BorderLayout.EAST)

        filterRecordActionsPanel.setOnAddRequestListener(::onCreateFilterRecordClicked)
        filterRecordChipsPanel.addFilterRecordEventListener(this)
        Preferences.addChangeListener(this)

        loadFilterRecords()

        // 添加 ComponentListener 以调整 JScrollPane 的高度
        chipsScrollPane.viewport.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                adjustScrollPaneHeight()
            }
        })
    }

    private fun loadFilterRecords() {
        val records = Preferences.get<List<FilterRecord>>(KEY_FILTER_RECORDS, emptyList())
        setFilterRecords(records)
    }

    private fun setFilterRecords(record: List<FilterRecord>) {
        filterRecordChipsPanel.setFilterRecords(record)
    }

    fun setOnFilterRecordSelectedListener(onFilterClicked: OnFilterRecordSelectedListener) {
        onFilterRecordSelectedListener = onFilterClicked
    }

    private fun onCreateFilterRecordClicked(filterName: String) {
        val filterRecord = currentFilterRecordProvider?.getFilterRecord(filterName)
        if (filterRecord != null) {
            val records = Preferences.get(KEY_FILTER_RECORDS, emptyList<FilterRecord>())
            val existingRecord = records.find { it.equalsIgnoreName(filterRecord) }
            if (existingRecord != null) {
                filterRecordChipsPanel.removeRecord(existingRecord)
            }
            filterRecordChipsPanel.addRecord(filterRecord)
        }
    }

    private fun adjustScrollPaneHeight() {
        val viewport = chipsScrollPane.viewport
        val view = viewport.view
        val preferredHeight = view.preferredSize.height
        val viewportHeight = viewport.height
        val scrollBarHeight = if (chipsScrollPane.horizontalScrollBar.isVisible) {
            chipsScrollPane.horizontalScrollBar.preferredSize.height
        } else {
            0
        }
        val newHeight = preferredHeight + scrollBarHeight
        if (newHeight != viewportHeight) {
            chipsScrollPane.preferredSize = Dimension(chipsScrollPane.width, newHeight)
            revalidate()
        }
    }

    override fun getFavoriteFilterPanel(): JPanel {
        return this
    }

    override fun setCurrentFilterRecordProvider(provider: FilterRecordProvider) {
        currentFilterRecordProvider = provider
        filterRecordActionsPanel.setCurrentFilterRecordProvider(provider)
    }

    override fun onFilterRecordAdded(filterRecord: FilterRecord) {
        val records = Preferences.get(KEY_FILTER_RECORDS, emptyList<FilterRecord>()).toMutableList()
        records.add(0, filterRecord)
        Preferences.put(KEY_FILTER_RECORDS, records)
        adjustScrollPaneHeight()
    }

    override fun onFilterRecordRemoved(filterRecord: FilterRecord) {
        val records = Preferences.get(KEY_FILTER_RECORDS, emptyList<FilterRecord>()).toMutableList()
        records.removeIf { it.name == filterRecord.name }
        Preferences.put(KEY_FILTER_RECORDS, records)
        adjustScrollPaneHeight()
    }

    override fun onFilterRecordSelected(filterRecord: FilterRecord) {
        onFilterRecordSelectedListener?.onRecordSelected(filterRecord)
    }

    override fun onPreferencesChanged() {
        loadFilterRecords()
        adjustScrollPaneHeight()
    }

    companion object {
        const val KEY_FILTER_RECORDS = "favorite_filter/filter_records"
    }
}