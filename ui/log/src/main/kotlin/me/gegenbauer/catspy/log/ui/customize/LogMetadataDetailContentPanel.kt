package me.gegenbauer.catspy.log.ui.customize

import info.clearthought.layout.TableLayout
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
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
                ParamVerifier.Result.Invalid(STRINGS.toolTip.logTypeBlankWarning)
            } else {
                ParamVerifier.Result.Valid
            }
        }
    }

    private val columnsPanel = ColumnsEditPanel()
    private val levelPanel = LevelsEditPanel()
    private val filterPanel = FiltersEditPanel()
    private val parserPanel = ParserEditPanel()
    private val logMetadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    private val editPanels = listOf<JPanel>(
        logTypePanel, descriptionPanel, samplePanel, columnsPanel, levelPanel, filterPanel, parserPanel
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
        editPanels.forEach { (it as LogMetadataEditor).startEditing() }
    }

    override fun stopEditing() {
        editPanels.map { (it as LogMetadataEditor).stopEditing() }
    }

    override fun isEditValid(): Boolean {
        return editPanels.map { (it as LogMetadataEditor).isEditValid() }.all { it }
    }

    private fun addEditPanels(panels: List<JPanel>) {
        panels.forEachIndexed { index, panel ->
            add(panel, "0, $index")
        }
    }

    override fun setLogMetadata(metadata: LogMetadataEditModel) {
        editPanels.forEach { (it as LogMetadataEditor).setLogMetadata(metadata) }
        logMetadata = metadata
    }

    fun getUpdatedLogMetadata(old: LogMetadataModel): LogMetadataModel {
        val filterUIConfs = filterPanel.items
        val columns = columnsPanel.items.map { column ->
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
            val filterUIConf = filterUIConfs.firstOrNull { it.columnId == column.id }
            if (filterUIConf != null) {
                val filterName = filterUIConf.name.ifBlank { column.name }
                column.copy(uiConf = column.uiConf.copy(filter = filterUIConf.copy(name = filterName, columnName = column.name)))
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
        val parser = parserPanel.getParser() as SerializableLogParser
        val metadata = LogMetadataModel(
            logType = logTypePanel.value,
            description = descriptionPanel.value,
            sample = samplePanel.value,
            isBuiltIn = old.isBuiltIn,
            columns = columns,
            parser = parser,
            supportedFileExtensions = old.supportedFileExtensions,
            levels = levelPanel.items,
            isDeviceLog = old.isDeviceLog
        )
        parser.setLogMetadata(metadata)
        return metadata
    }

    private fun configureEditableTablePanels() {
        columnsPanel.configure()
        levelPanel.configure()
        filterPanel.configure()
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
                        isNew = logMetadata.isNew,
                    )
                )
            }
    }
}