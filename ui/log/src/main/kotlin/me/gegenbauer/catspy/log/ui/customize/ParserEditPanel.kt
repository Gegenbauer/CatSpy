package me.gegenbauer.catspy.log.ui.customize

import info.clearthought.layout.TableLayout
import me.gegenbauer.catspy.log.serialize.*
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.OnScrollToEndListener
import me.gegenbauer.catspy.utils.ui.ScrollToEndListenerSupport
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

/**
 * The parser editor consists of a series of parser operation panels and parser result panels.
 * The parser operation panels correspond one-to-one with the parser demonstration panels.
 * After adding a parser operation, a corresponding parser result panel will be generated.
 * Each parser operation can be re-edited, and the edits will affect subsequent parser operations and results.
 */
class ParserEditPanel : JPanel(), LogMetadataEditor, ParseOpEventListener, ScrollToEndListenerSupport, EditEventSource {

    private val actionPanel = ActionPanel()
    private val hintLabel = MultilineLabel(STRINGS.ui.parserHint)
    private val rawLogPanel: StringValueEditPanel = stringValueEditPanel {
        label { STRINGS.ui.parserRowLog }
        tooltip { STRINGS.toolTip.parserRowLog }
        maxCharCount { 60 }
        logMetadataHandler { value = it.sample }
        alwaysUnEditable { it.isBuiltIn }
    }
    private var logMetadata: LogMetadataEditModel = LogMetadataModel.default.toEditModel()
    private val parseOpGroupEditor = ParseOpGroupEditor(this)
    private var lastValidParser: SerializableLogParser? = null

    private val editEventListeners = mutableListOf<EditEventListener>()

    private val SerializableLogParser.opList: List<ParseOp>
        get() = mutableListOf<ParseOp>().apply {
            add(splitToPartsOp)
            addAll(trimOps)
        }

    init {
        border = BorderFactory.createTitledBorder(STRINGS.ui.parserEditorTitle)
        layout = TableLayout(
            doubleArrayOf(TableLayout.FILL),
            doubleArrayOf(TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED)
        )
        add(actionPanel, "0,0")
        add(hintLabel, "0,1")
        add(rawLogPanel, "0,2")
        add(parseOpGroupEditor, "0,3")

        actionPanel.addOnCheckPerformancesClickListener(::onCheckPerformancesClick)
        actionPanel.addOnAddOpClickListener { parseOpGroupEditor.addFirstParseOpPanel() }
        parseOpGroupEditor.addParseOpEventListener(this)
    }

    fun getParser(): SerializableLogParser? {
        val parser = getSerializableLogParser()
        if (parser != null) {
            return parser
        }
        return lastValidParser
    }

    private fun getSerializableLogParser(): SerializableLogParser? {
        if (parseOpGroupEditor.isEditValid()) {
            val parseOps = parseOpGroupEditor.getParseOps()
            val splitToPartsOp = (parseOps.firstOrNull { it is SplitToPartsOp } as? SplitToPartsOp) ?: EmptySplitToPartsOp()
            val trimOps = parseOps.filterIsInstance<TrimOp>()
            return SerializableLogParser(splitToPartsOp, trimOps)
        }
        return null
    }

    override fun setLogMetadata(metadata: LogMetadataEditModel) {
        logMetadata = metadata
        rawLogPanel.setLogMetadata(metadata)
        actionPanel.setLogMetadata(metadata)
        if (metadata.model.parser !is SerializableLogParser) return

        lastValidParser = metadata.model.parser

        val ops = metadata.model.parser.opList
        parseOpGroupEditor.setParseOps(ops)
        val sample = rawLogPanel.value
        parseOpGroupEditor.parsedLogParts = listOf(sample)
    }

    override fun isModified(): Boolean {
        return getParser() != lastValidParser
    }

    override fun onOpChanged(parseOpPanel: ParseOpItemEditPanel) {
        if (parseOpGroupEditor.getParseOps().isEmpty()) {
            actionPanel.setAddOpButtonEnabled(true)
            actionPanel.setCheckPerformancesButtonEnabled(false)
        } else {
            parseOpGroupEditor.parsedLogParts = listOf(rawLogPanel.value)
        }
        parseOpGroupEditor.revalidate()
        parseOpGroupEditor.repaint()
        revalidate()
        repaint()

        notifyEditStateChanged()
    }

    override fun onOpRemoved(parseOpPanel: ParseOpItemEditPanel) {
        if (parseOpGroupEditor.getParseOps().isEmpty()) {
            actionPanel.setAddOpButtonEnabled(true)
            actionPanel.setCheckPerformancesButtonEnabled(false)
        }
        parseOpGroupEditor.parsedLogParts = listOf(rawLogPanel.value)

        notifyEditStateChanged()
    }

    override fun onOpAdded(parseOpPanel: ParseOpItemEditPanel) {
        actionPanel.setAddOpButtonEnabled(false)
        actionPanel.setCheckPerformancesButtonEnabled(true)
        parseOpGroupEditor.parsedLogParts = listOf(rawLogPanel.value)

        notifyEditStateChanged()
    }

    private fun onCheckPerformancesClick() {
        // TODO
    }

    override fun addEditEventListener(listener: EditEventListener) {
        editEventListeners.add(listener)
    }

    private fun notifyEditStateChanged() {
        editEventListeners.forEach { it.onEditDone(this) }
    }

    override fun startEditing() {
        if (!logMetadata.model.isBuiltIn) {
            parseOpGroupEditor.startEditing()
            actionPanel.startEditing()
        }
    }

    override fun stopEditing() {
        parseOpGroupEditor.stopEditing()
        actionPanel.stopEditing()
    }

    override fun isEditValid(): Boolean {
        return logMetadata.model.isBuiltIn || parseOpGroupEditor.isEditValid()
    }

    override fun addOnScrollToEndListener(listener: OnScrollToEndListener) {
        parseOpGroupEditor.addOnScrollToEndListener(listener)
    }

    private inner class ActionPanel : CenteredDualDirectionPanel(), Editor, LogMetadataEditor {
        private val addOpButton = JButton(STRINGS.ui.addNewParseOp)
        private val templateLoaderButton = LogMetadataTemplateLoaderButton()
        private val checkPerformancesButton = JButton(STRINGS.ui.checkPerformance).apply {
            toolTipText = STRINGS.toolTip.checkPerformance
        }
        private val logMetadataSelectListener = object : OnMetadataChangedListener {
            override fun onLogMetadataSelected(metadata: LogMetadataModel) {
                onTemplateSelected(metadata)
            }
        }

        init {
            addRight(addOpButton)
            addRight(templateLoaderButton)
            addRight(checkPerformancesButton)

            checkPerformancesButton.isVisible = false
            checkPerformancesButton.isEnabled = false
            addOpButton.isEnabled = false

            templateLoaderButton.addOnSelectedMetadataChangedListener(logMetadataSelectListener)
        }

        private fun onTemplateSelected(metadata: LogMetadataModel) {
            this@ParserEditPanel.setLogMetadata(
                metadata.copy(
                    isBuiltIn = false,
                    sample = logMetadata.model.sample,
                    logType = logMetadata.model.logType
                ).toEditModel(isNightMode = logMetadata.isNightMode)
            )
        }

        fun addOnAddOpClickListener(listener: () -> Unit) {
            addOpButton.addActionListener { listener() }
        }

        fun addOnCheckPerformancesClickListener(listener: () -> Unit) {
            checkPerformancesButton.addActionListener { listener() }
        }

        fun setAddOpButtonEnabled(enabled: Boolean) {
            addOpButton.isEnabled = enabled
        }

        fun setAddOpButtonVisible(visible: Boolean) {
            addOpButton.isVisible = visible
        }

        fun setCheckPerformancesButtonEnabled(enabled: Boolean) {
            checkPerformancesButton.isEnabled = enabled
        }

        fun setCheckPerformancesButtonVisible(visible: Boolean) {
            checkPerformancesButton.isVisible = visible
        }

        override fun setLogMetadata(metadata: LogMetadataEditModel) {
            templateLoaderButton.isVisible = !metadata.model.isBuiltIn
            templateLoaderButton.setExcludedMetadata(metadata.model.logType)
        }

        override fun isModified(): Boolean {
            return false
        }

        override fun startEditing() {
            addOpButton.isEnabled = true
            templateLoaderButton.isEnabled = true
            checkPerformancesButton.isEnabled = true
        }

        override fun stopEditing() {
            addOpButton.isEnabled = false
            templateLoaderButton.isEnabled = false
            checkPerformancesButton.isEnabled = false
        }

        override fun isEditValid(): Boolean {
            return true
        }
    }
}
