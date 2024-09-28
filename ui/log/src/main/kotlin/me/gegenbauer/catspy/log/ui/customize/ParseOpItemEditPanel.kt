package me.gegenbauer.catspy.log.ui.customize

import info.clearthought.layout.TableLayout
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.serialize.*
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.OnScrollToEndListener
import me.gegenbauer.catspy.utils.ui.ScrollToEndListenerSupport
import me.gegenbauer.catspy.utils.ui.addOnScrollToEndListener
import me.gegenbauer.catspy.utils.ui.adjustScrollPaneHeight
import me.gegenbauer.catspy.view.panel.ScrollConstrainedScrollablePanel
import me.gegenbauer.catspy.view.panel.VerticalFlexibleWidthLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*
import javax.swing.border.LineBorder

interface ParseOpEditor : Editor {
    fun setPreviousOpParsedResult(parts: List<String>)

    fun getParsedResult(): List<String>

    fun getParseOp(): ParseOp

    fun setPreviousParseOp(previousParseOp: ParseOp?)
}

data class ParseOpContext(
    val parseOp: ParseOp?,
    val previousOp: ParseOp?,
    val parentOp: ParseOp?,
) {
    fun isSplitToPartsOp(): Boolean {
        return parentOp == null && previousOp == null
    }

    fun isSplitPostProcessOp(): Boolean {
        return parentOp is SplitByWordSeparatorOp
    }

    fun isSplitByWordSeparatorOp(): Boolean {
        return parseOp is SplitByWordSeparatorOp
    }
}

/**
 * Based on the previous parser action and parsing result
 * Users can select the currently supported parser actions, and then fill in the parser parameters according
 * to the selected parser actions.
 * If a parser step is passed in, the parser step is displayed
 */
class ParseOpItemEditPanel(
    private val parseOpContext: ParseOpContext
) : JPanel(), ParseOpEditor, ParseOpEventListener, ParserOpEventSupport, ParseOpRequestSupport,
    ScrollToEndListenerSupport {

    private val editor = OpEditor()
    private val actionPanel = ActionPanel()
    private val parseResultPanel = ParseResultPanel()
    private val parseOpEventListeners = mutableListOf<ParseOpEventListener>()
    private val parseOpRequestListeners = mutableListOf<ParseOpRequestListener>()

    private val postProcessOpsPanel: ParseOpGroupEditor = ParseOpGroupEditor(this, SplitByWordSeparatorOp())
    private var previousParseOpParsedResult = emptyList<String>()

    init {
        layout = TableLayout(
            doubleArrayOf(
                TableLayout.PREFERRED,
                TableLayout.FILL,
                TableLayout.PREFERRED
            ),
            doubleArrayOf(
                TableLayout.PREFERRED,
                TableLayout.PREFERRED,
                TableLayout.PREFERRED
            )
        )
        add(editor.opSelectorContainer, "0,0")
        add(editor.paramsEditorContainer, "1,0")
        add(actionPanel, "2,0")
        add(editor.subParseOpContainer, "0,1,1,1")
        add(parseResultPanel, "0,2,2,1")

        actionPanel.addOnAddOpClickListener {
            notifyOnRequestAddOp()
        }
        actionPanel.addOnRemoveOpClickListener {
            notifyOnRemoveOp()
        }
        actionPanel.addOnAddSubOpClickListener {
            postProcessOpsPanel.addOpPanel(getParseOp())
        }

        actionPanel.setAddSubOpButtonVisible(parseOpContext.isSplitByWordSeparatorOp())
        updateEditorOp(parseOpContext.parseOp)
        updateSplitPostProcessOps(parseOpContext.parseOp)
        updateOpSelectOptions()

        postProcessOpsPanel.addParseOpEventListener(this)
        editor.setSubOpsGroupPanel(postProcessOpsPanel)
    }

    override fun updateUI() {
        super.updateUI()
        border = LineBorder(UIManager.getColor("Separator.foreground"), 1)
    }

    override fun setPreviousOpParsedResult(parts: List<String>) {
        previousParseOpParsedResult = parts
        editor.setPartCount(parts.size)
        updateParseResult(parts)

        val maxParts = (getParseOp() as? SplitByWordSeparatorOp)?.maxParts ?: SplitByWordSeparatorOp.DEFAULT_MAX_PARTS
        postProcessOpsPanel.parsedLogParts = parts.flatMap { it.split(WORD_SEPARATOR_REGEX, maxParts) }
    }

    override fun getParseOp(): ParseOp {
        return editor.getParseOp()
    }

    override fun setPreviousParseOp(previousParseOp: ParseOp?) {
        updateOpSelectOptions()
    }

    override fun addParseOpRequestListener(listener: ParseOpRequestListener) {
        parseOpRequestListeners.add(listener)
    }

    override fun removeParseOpRequestListener(listener: ParseOpRequestListener) {
        parseOpRequestListeners.remove(listener)
    }

    private fun notifyOnRequestAddOp() {
        parseOpRequestListeners.forEach { it.onRequestAddOp(this) }
    }

    private fun notifyOnRemoveOp() {
        parseOpRequestListeners.toList().forEach { it.onRequestRemoveOp(this) }
    }

    private fun updateParseResult(previousOpParsedResult: List<String>) {
        val op = getParseOp()
        val parsedLogParts = op.process(previousOpParsedResult.asSequence()).toList()
        parseResultPanel.setResults(parsedLogParts)
    }

    override fun getParsedResult(): List<String> {
        return parseResultPanel.getParsedResult()
    }

    override fun addParseOpEventListener(listener: ParseOpEventListener) {
        parseOpEventListeners.add(listener)
    }

    override fun removeParseOpEventListener(listener: ParseOpEventListener) {
        parseOpEventListeners.remove(listener)
    }

    override fun onOpChanged(parseOpPanel: ParseOpItemEditPanel) {
        notifyOnParseOpChanged()
    }

    private fun onOpChangedManually() {
        val op = getParseOp()
        actionPanel.setAddSubOpButtonVisible(op is SplitByWordSeparatorOp)
        notifyOnParseOpChanged()
    }

    private fun notifyOnParseOpChanged() {
        parseOpEventListeners.forEach { it.onOpChanged(this) }
    }

    private fun updateEditorOp(op: ParseOp?) {
        editor.selectOp(op)
    }

    override fun onOpAdded(parseOpPanel: ParseOpItemEditPanel) {
        editor.subParseOpContainer.isVisible = postProcessOpsPanel.getParseOps().isNotEmpty()
        notifyOnParseOpChanged()
    }

    override fun onOpRemoved(parseOpPanel: ParseOpItemEditPanel) {
        editor.subParseOpContainer.isVisible = postProcessOpsPanel.getParseOps().isNotEmpty()
        notifyOnParseOpChanged()
    }

    private fun updateSplitPostProcessOps(parseOp: ParseOp?) {
        if (parseOp is SplitByWordSeparatorOp) {
            setSplitPostProcessOps(parseOp.splitPostProcessOps)
        }
    }

    private fun updateOpSelectOptions() {
        val supportedOps = if (parseOpContext.isSplitToPartsOp()) {
            listOf(SplitByWordSeparatorOp(), EmptySplitToPartsOp())
        } else if (parseOpContext.isSplitPostProcessOp()) {
            listOf(
                MergeNearbyPartsOp(0, 0),
                MergeUntilCharOp(0, null),
                RemoveBlankPartOp(),
                SplitPartWithCharOp(null, 0, 0)
            )
        } else {
            listOf(
                TrimWithCharOp(0, null, null),
                TrimWithIndexOp(0, 0, 0)
            )
        }
        editor.setOpSelectOptions(supportedOps)
    }

    private fun getSplitPostProcessOps(): List<SplitPostProcessOp> {
        return postProcessOpsPanel.getParseOps().mapNotNull { it as? SplitPostProcessOp }
    }

    private fun setSplitPostProcessOps(ops: List<SplitPostProcessOp>) {
        postProcessOpsPanel.setParseOps(ops)
        editor.subParseOpContainer.isVisible = ops.isNotEmpty()
    }

    override fun startEditing() {
        editor.startEditing()
        actionPanel.startEditing()

        postProcessOpsPanel.startEditing()
    }

    override fun stopEditing() {
        editor.stopEditing()
        actionPanel.stopEditing()
        postProcessOpsPanel.stopEditing()
    }

    override fun isEditValid(): Boolean {
        return editor.isEditValid() && postProcessOpsPanel.isEditValid()
    }

    override fun addOnScrollToEndListener(listener: OnScrollToEndListener) {
        parseResultPanel.addOnScrollToEndListener(listener)
        postProcessOpsPanel.addOnScrollToEndListener(listener)
    }

    private class ActionPanel(isOpGroupContainer: Boolean = false) : JPanel(), EditableContainer {
        private val addOpButton = JButton(STRINGS.ui.addParseOpBlow)
        private val addSubOpButton = JButton(STRINGS.ui.addSubOp).apply {
            toolTipText = STRINGS.toolTip.addSubOp
        }
        private val removeOpButton = JButton(STRINGS.ui.removeCurrentOp).apply {
            toolTipText = STRINGS.toolTip.removeCurrentOp
        }

        override val isEditing: Boolean
            get() = removeOpButton.isEnabled

        init {
            add(addOpButton)
            add(addSubOpButton)
            add(removeOpButton)

            addSubOpButton.isVisible = isOpGroupContainer
        }

        fun addOnAddOpClickListener(listener: () -> Unit) {
            addOpButton.addActionListener { listener() }
        }

        fun addOnAddSubOpClickListener(listener: () -> Unit) {
            addSubOpButton.addActionListener { listener() }
        }

        fun setAddSubOpButtonVisible(visible: Boolean) {
            addSubOpButton.isVisible = visible
        }

        fun addOnRemoveOpClickListener(listener: () -> Unit) {
            removeOpButton.addActionListener { listener() }
        }

        override fun startEditing() {
            addOpButton.isEnabled = true
            removeOpButton.isEnabled = true
            addSubOpButton.isEnabled = true
        }

        override fun stopEditing() {
            addOpButton.isEnabled = false
            addSubOpButton.isEnabled = false
            removeOpButton.isEnabled = false
        }
    }

    inner class OpEditor : EditableContainer {

        override val isEditing: Boolean
            get() = opSelector.isEnabled

        val opSelectorContainer = JPanel()
        val paramsEditorContainer = JPanel()
        val subParseOpContainer = JPanel()

        private var opDetailEditor: ParseOpParamsEditor<*> = EmptyOpParamsEditor(EmptyParseOp())
            private set(value) {
                field = value
                addOpDetailEditor()
            }
        private val opSelector = JComboBox<ParseOp>().apply { isEnabled = false }
        private var currentOp: ParseOp? = null

        private val opSelectionChangeListener = ItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                currentOp = it.item as? ParseOp
                onOpSelectChanged(currentOp)
                onOpChangedManually()
            }
        }

        init {
            subParseOpContainer.border = BorderFactory.createEmptyBorder(0, 30, 0, 0)
            subParseOpContainer.layout = VerticalFlexibleWidthLayout()
            subParseOpContainer.isVisible = false
            opSelector.renderer = ComboBoxRenderer()
            opSelector.addItemListener(opSelectionChangeListener)
            opSelectorContainer.add(opSelector)
        }

        fun setPartCount(partCount: Int) {
            opDetailEditor.partCount = partCount
        }

        fun setOpSelectOptions(options: List<ParseOp>) {
            opSelector.removeAllItems()
            opSelector.model = DefaultComboBoxModel(options.toTypedArray())
            selectOp(currentOp)
        }

        fun setSubOpsGroupPanel(panel: JPanel) {
            subParseOpContainer.removeAll()
            subParseOpContainer.add(panel)
        }

        fun selectOp(op: ParseOp?) {
            currentOp = op
            for (i in 0 until opSelector.itemCount) {
                if (opSelector.getItemAt(i).javaClass == op?.javaClass) {
                    changeSelectionWithoutNotify(opSelector.getItemAt(i))
                    return
                }
            }
            changeSelectionWithoutNotify(null)
        }

        private fun changeSelectionWithoutNotify(selection: ParseOp?) {
            opSelector.removeItemListener(opSelectionChangeListener)
            opSelector.selectedItem = selection
            onOpSelectChanged(currentOp)
            opSelector.addItemListener(opSelectionChangeListener)
        }

        override fun startEditing() {
            opDetailEditor.startEditing()
            opSelector.isEnabled = true
        }

        override fun stopEditing() {
            opDetailEditor.stopEditing()
            opSelector.isEnabled = false
        }

        override fun isEditValid(): Boolean {
            return opDetailEditor.isEditValid()
        }

        private fun addOpDetailEditor() {
            paramsEditorContainer.removeAll()
            opDetailEditor.addEditEventListener {
                currentOp = getParseOp()
            }
            val editor = opDetailEditor as JPanel
            paramsEditorContainer.add(editor)
            paramsEditorContainer.revalidate()
            paramsEditorContainer.repaint()
        }

        private fun onOpSelectChanged(newOp: ParseOp?) {
            opDetailEditor = createEditorPanel(newOp)
            opSelector.toolTipText = newOp?.description ?: EMPTY_STRING
            if (isEditing) {
                opDetailEditor.startEditing()
            } else {
                opDetailEditor.stopEditing()
            }
            opDetailEditor.registerEditorEvents()
            opDetailEditor.addEditEventListener { onOpChangedManually() }
        }

        private fun createEditorPanel(parseOp: ParseOp?): ParseOpParamsEditor<*> {
            val maxPartCountAllowed = previousParseOpParsedResult.size
            return when (parseOp) {
                is EmptySplitToPartsOp -> EmptyOpParamsEditor(parseOp)
                is SplitByWordSeparatorOp -> SplitByWordSeparatorOpParamsEditor(parseOp.maxParts)
                is SplitPartWithCharOp -> SplitPartWithCharOpParamsEditor(
                    parseOp.splitChar,
                    parseOp.partIndex,
                    maxPartCountAllowed,
                    parseOp.maxPart,
                )

                is MergeNearbyPartsOp -> MergeNearbyPartsOpParamsEditor(parseOp.from, parseOp.to, maxPartCountAllowed)
                is MergeUntilCharOp -> MergeUntilCharOpParamsEditor(
                    parseOp.start,
                    maxPartCountAllowed,
                    parseOp.targetChar
                )
                is TrimWithCharOp -> TrimWithCharOpParamsEditor(
                    parseOp.partIndex,
                    maxPartCountAllowed,
                    parseOp.leading,
                    parseOp.trailing
                )

                is TrimWithIndexOp -> TrimWithIndexOpParamsEditor(
                    parseOp.partIndex,
                    maxPartCountAllowed,
                    parseOp.removedLeadingCharCount,
                    parseOp.removedTrailingCharCount
                )

                else -> EmptyOpParamsEditor(parseOp ?: EmptyParseOp())
            }
        }

        fun getParseOp(): ParseOp {
            val op = opDetailEditor.parseOp
            if (op is SplitByWordSeparatorOp) {
                return SplitByWordSeparatorOp(op.maxParts, getSplitPostProcessOps())
            }
            return opDetailEditor.parseOp
        }

        private inner class ComboBoxRenderer : ListCellRenderer<ParseOp> {
            private val renderer = DefaultListCellRenderer()

            override fun getListCellRendererComponent(
                list: JList<out ParseOp>?,
                value: ParseOp?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                return renderer.getListCellRendererComponent(list, value?.name, index, isSelected, cellHasFocus)
            }
        }
    }

    private class ParseResultPanel : JPanel(), ScrollToEndListenerSupport {

        private val container = ScrollConstrainedScrollablePanel(verticalScrollable = false)
        private val scrollPane = JScrollPane(container)

        init {
            layout = VerticalFlexibleWidthLayout()
            add(scrollPane)

            scrollPane.viewport.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    adjustScrollPaneHeight(scrollPane)
                }
            })
        }

        fun setResults(parts: List<String>) {
            container.removeAll()
            container.layout = createTableLayout(parts.size)
            parts.forEachIndexed { index, s ->
                container.add(PartItem(s), getTableLayoutConstraint(index))
            }
            container.revalidate()
            container.repaint()
            scrollPane.horizontalScrollBar.value = 0
            adjustScrollPaneHeight(scrollPane)
        }

        override fun addOnScrollToEndListener(listener: OnScrollToEndListener) {
            scrollPane.addOnScrollToEndListener(listener)
        }

        private fun getTableLayoutConstraint(index: Int): String {
            return "$index, 0"
        }

        private fun createTableLayout(count: Int): TableLayout {
            return TableLayout(
                DoubleArray(count) { TableLayout.PREFERRED },
                doubleArrayOf(TableLayout.PREFERRED),
            )
        }

        fun getParsedResult(): List<String> {
            return container.components.map { it as PartItem }.map { it.text }
        }

        private class PartItem(text: String) : WidthConstrainedTextField(text = text, maxCharCount = MAX_CHAR_LEN) {

            init {
                isEditable = false
            }

            companion object {
                private const val MAX_CHAR_LEN = 70
            }
        }
    }

    companion object {
        private val WORD_SEPARATOR_REGEX = "\\s+".toRegex()
    }
}