package me.gegenbauer.catspy.log.ui.customize

import info.clearthought.layout.TableLayout
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.metadata.LogColorScheme
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.serialize.ColumnModel
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.log.serialize.SerializableLogParser
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.OnScrollToEndListener
import me.gegenbauer.catspy.utils.ui.ScrollToEndListenerSupport
import me.gegenbauer.catspy.view.panel.ScrollConstrainedScrollablePanel
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class LogMetadataDetailContentPanel : ScrollConstrainedScrollablePanel(horizontalScrollable = false), LogMetadataEditor,
    ScrollToEndListenerSupport, EditEventSource, EditEventListener {

    private val nameVerifier = NameVerifier()
    private val logTypePanel: StringValueEditPanel = stringValueEditPanel {
        label { STRINGS.ui.logTypeLabel }
        tooltip { STRINGS.toolTip.logType }
        logMetadataHandler { value = it.logType }
        alwaysUnEditable { it.isBuiltIn }
        verifier {
            it as JTextField
            if (logMetadata.isNew && logMetadataManager.isCustomized(it.text)) {
                return@verifier ParamVerifier.Result.Invalid(STRINGS.toolTip.logTypeDuplicateWarning)
            }
            return@verifier nameVerifier.verify(it)
        }
    }
    private val descriptionPanel: StringValueEditPanel = stringValueEditPanel {
        label { STRINGS.ui.logDescriptionLabel }
        tooltip { STRINGS.toolTip.logDescription }
        logMetadataHandler { value = it.description }
        alwaysUnEditable { it.isBuiltIn }
    }

    private val samplePanel: StringValueEditPanel = stringValueEditPanel {
        label { STRINGS.ui.logSampleLabel }
        tooltip { STRINGS.toolTip.logSample }
        maxCharCount { 60 }
        logMetadataHandler { value = it.sample }
        alwaysUnEditable { it.isBuiltIn }
        verifier {
            it as JTextField
            if (it.text.isBlank()) {
                ParamVerifier.Result.Invalid(STRINGS.toolTip.contentBlankWarning)
            } else {
                ParamVerifier.Result.Valid
            }
        }
    }

    private val columnsPanel = ColumnsEditPanel()
    private val levelPanel = LevelsEditPanel()
    private val filterPanel = FiltersEditPanel()
    private val parserPanel = ParserEditPanel()
    private val colorSchemePanel = ColorSchemeEditPanel()
    private val logMetadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    private val editPanels = listOf<JPanel>(
        logTypePanel,
        descriptionPanel,
        samplePanel,
        columnsPanel,
        levelPanel,
        filterPanel,
        parserPanel,
        colorSchemePanel
    )
    private var logMetadata: LogMetadataEditModel = LogMetadataModel.default.toEditModel()

    init {
        layout = TableLayout(
            doubleArrayOf(
                TableLayout.FILL
            ),
            doubleArrayOf(
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
            )
        )
        border = BorderFactory.createEmptyBorder(10, 10, 200, 10)
        addEditPanels(editPanels)

        configureEditableTablePanels()
        addEditEventListener(this)
    }

    override fun addEditEventListener(listener: EditEventListener) {
        editPanels.filterIsInstance<EditEventSource>().forEach { it.addEditEventListener(listener) }
    }

    override fun startEditing() {
        editPanels.filterIsInstance<LogMetadataEditor>().forEach { it.startEditing() }
    }

    override fun stopEditing() {
        editPanels.filterIsInstance<LogMetadataEditor>().forEach { it.stopEditing() }
    }

    override fun isEditValid(): Boolean {
        return editPanels.filterIsInstance<LogMetadataEditor>().all { it.isEditValid() }
    }

    private fun addEditPanels(panels: List<JPanel>) {
        panels.forEachIndexed { index, panel ->
            add(panel, "0, $index")
        }
    }

    override fun setLogMetadata(metadata: LogMetadataEditModel) {
        editPanels.filterIsInstance<LogMetadataEditor>().forEach { it.setLogMetadata(metadata) }
        logMetadata = metadata
    }

    override fun onNightModeChanged(isDark: Boolean) {
        editPanels.filterIsInstance<LogMetadataEditor>().forEach { it.onNightModeChanged(isDark) }
    }

    override fun isModified(): Boolean {
        return editPanels.filterIsInstance<LogMetadataEditor>().any { it.isModified() }
    }

    fun getUpdatedLogMetadata(old: LogMetadataModel): LogMetadataModel {
        val filterUIConfs = filterPanel.items
        val columns = getUpdatedColumns(columnsPanel.items, filterUIConfs)
        val colorScheme = getUpdatedColorScheme(colorSchemePanel.items)
        val parser = parserPanel.getParser() as SerializableLogParser
        val metadata = LogMetadataModel(
            logType = logTypePanel.value,
            description = descriptionPanel.value,
            sample = samplePanel.value,
            isBuiltIn = old.isBuiltIn,
            columns = columns,
            parser = parser,
            levels = levelPanel.items,
            isDeviceLog = old.isDeviceLog,
            colorScheme = colorScheme,
            version = LogMetadata.VERSION
        )
        parser.setLogMetadata(metadata)
        return metadata
    }

    private fun getUpdatedColumns(
        columns: List<ColumnModel>,
        filters: List<ColumnModel.FilterUIConf>
    ): List<ColumnModel> {
        return columns.map { column ->
            if (column.uiConf.column.isHidden || column.supportFilter.not()) {
                return@map column.copy(
                    uiConf = column.uiConf.copy(
                        filter = column.uiConf.filter.copy(
                            columnId = column.id,
                            columnName = column.name
                        )
                    )
                )
            }
            val filterUIConf = filters.firstOrNull { it.columnId == column.id }
            if (filterUIConf != null) {
                val filterName = filterUIConf.name.ifBlank { column.name }
                column.copy(
                    uiConf = column.uiConf.copy(
                        filter = filterUIConf.copy(
                            name = filterName,
                            columnName = column.name
                        )
                    )
                )
            } else {
                column.copy(
                    uiConf = column.uiConf.copy(
                        filter = column.uiConf.filter.copy(
                            columnId = column.id,
                            name = column.name
                        )
                    )
                )
            }
        }
    }

    private fun getUpdatedColorScheme(colorSchemeItems: List<ColorSchemeItem>): LogColorScheme {
        return LogColorScheme().apply {
            colorSchemeItems.forEach {
                val field = javaClass.getDeclaredField(it.name)
                field.isAccessible = true
                field.set(this, it.color)
            }
        }
    }

    private fun configureEditableTablePanels() {
        columnsPanel.configure()
        levelPanel.configure()
        filterPanel.configure()
        colorSchemePanel.configure()
    }

    override fun addOnScrollToEndListener(listener: OnScrollToEndListener) {
        editPanels.filterIsInstance<ScrollToEndListenerSupport>()
            .forEach { it.addOnScrollToEndListener(listener) }
    }

    override fun onEditDone(component: JComponent) {
        editPanels
            .filter { it != component }
            .filterIsInstance<LogMetadataEditor>()
            .forEach {
                it.setLogMetadata(
                    getUpdatedLogMetadata(logMetadata.model).toEditModel(
                        id = logMetadata.id,
                        isDeleted = logMetadata.isDeleted,
                        isNightMode = logMetadata.isDarkMode,
                        isNew = logMetadata.isNew,
                    )
                )
            }
    }
}