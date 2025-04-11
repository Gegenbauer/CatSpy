package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.log.serialize.ParseOp
import me.gegenbauer.catspy.utils.ui.OnScrollToEndListener
import me.gegenbauer.catspy.utils.ui.ScrollToEndListenerSupport
import me.gegenbauer.catspy.view.panel.VerticalFlexibleWidthLayout
import java.awt.Graphics
import java.awt.event.MouseWheelEvent
import javax.swing.JPanel

interface ParseOpEventListener {
    fun onOpChanged(parseOpPanel: ParseOpItemEditPanel)

    fun onOpAdded(parseOpPanel: ParseOpItemEditPanel) {}

    fun onOpRemoved(parseOpPanel: ParseOpItemEditPanel) {}
}

interface ParseOpRequestListener {
    fun onRequestAddOp(parseOpPanel: ParseOpItemEditPanel)

    fun onRequestRemoveOp(parseOpPanel: ParseOpItemEditPanel)
}

fun interface ParseOpRequestSupport {
    fun addParseOpRequestListener(listener: ParseOpRequestListener)

    fun removeParseOpRequestListener(listener: ParseOpRequestListener) {
        // default no-op
    }
}

fun interface ParserOpEventSupport {
    fun addParseOpEventListener(listener: ParseOpEventListener)

    fun removeParseOpEventListener(listener: ParseOpEventListener) {
        // default no-op
    }
}

class ParseOpGroupEditor(
    private val container: JPanel,
    private val parentOp: ParseOp? = null
) : JPanel(), ParseOpEventListener, EditableContainer, ParserOpEventSupport, ParseOpRequestListener,
    ScrollToEndListenerSupport, OnScrollToEndListener {

    override var isEditing: Boolean = false

    var parsedLogParts: List<String> = emptyList()
        set(value) {
            field = value
            updateParseResults()
        }

    private var ops: List<ParseOp> = emptyList()
    private var parseMetadata: LogParser.ParseMetadata = LogParser.defaultParseMetadata

    private val parseOpPanels = mutableListOf<ParseOpItemEditPanel>()
    private val parseOpEventListeners = mutableListOf<ParseOpEventListener>()
    private val onScrollToEndListeners = mutableSetOf<OnScrollToEndListener>()

    init {
        layout = VerticalFlexibleWidthLayout()
    }

    fun getParseOps(): List<ParseOp> {
        return parseOpPanels.map { it.getParseOp() }
    }

    fun setParseOps(ops: List<ParseOp>, parseMetadata: LogParser.ParseMetadata) {
        this.ops = ops
        this.parseMetadata = parseMetadata
        buildParseOpPanels()
    }

    private fun buildParseOpPanels() {
        removeAll()
        parseOpPanels.clear()
        for (i in ops.indices) {
            val op = ops[i]
            val previousOp = if (i > 0) ops[i - 1] else null
            val panel = createParseOpPanel(previousOp, parentOp, op)
            addParseOpPanel(panel)
        }
    }

    private fun createParseOpPanel(
        previousOp: ParseOp?,
        parentOp: ParseOp?,
        op: ParseOp? = null,
    ): ParseOpItemEditPanel {
        return ParseOpItemEditPanel(ParseOpContext(op, previousOp, parentOp, parseMetadata))
    }

    private fun addParseOpPanel(panel: ParseOpItemEditPanel, index: Int = parseOpPanels.size) {
        parseOpPanels.add(panel)
        add(panel, index)
        registerListeners(panel)
        if (this.isEditing) {
            panel.startEditing()
        } else {
            panel.stopEditing()
        }
        updateOpDependencies()
        revalidate()
        repaint()
    }

    private fun registerListeners(panel: ParseOpItemEditPanel) {
        panel.addParseOpEventListener(this)
        panel.addParseOpRequestListener(this)
        panel.addOnScrollToEndListener(this)
    }

    private fun unregisterListeners(panel: ParseOpItemEditPanel) {
        panel.removeParseOpEventListener(this)
        panel.removeParseOpRequestListener(this)
        panel.removeOnScrollToEndListener(this)
    }

    private fun removeParseOpPanel(panel: ParseOpItemEditPanel) {
        parseOpPanels.remove(panel)
        remove(panel)
        unregisterListeners(panel)
        updateOpDependencies()
        revalidate()
        repaint()
    }

    private fun updateParseResults() {
        var parsedLogParts = parsedLogParts
        for (i in parseOpPanels.indices) {
            parseOpPanels[i].setPreviousOpParsedResult(parsedLogParts)
            parsedLogParts = parseOpPanels[i].getParsedResult()
            parseOpPanels[i].revalidate()
            parseOpPanels[i].repaint()
        }
    }

    override fun startEditing() {
        for (panel in parseOpPanels) {
            panel.startEditing()
        }

        isEditing = true
    }

    override fun stopEditing() {
        parseOpPanels.forEach { it.stopEditing() }

        isEditing = false
    }

    override fun isEditValid(): Boolean {
        return parseOpPanels.all { it.isEditValid() }
    }

    override fun addParseOpEventListener(listener: ParseOpEventListener) {
        parseOpEventListeners.add(listener)
    }

    override fun removeParseOpEventListener(listener: ParseOpEventListener) {
        parseOpEventListeners.remove(listener)
    }

    private fun notifyOpChanged(parseOpPanel: ParseOpItemEditPanel) {
        for (listener in parseOpEventListeners) {
            listener.onOpChanged(parseOpPanel)
        }
    }

    private fun notifyOpAdded(parseOpPanel: ParseOpItemEditPanel) {
        for (listener in parseOpEventListeners) {
            listener.onOpAdded(parseOpPanel)
        }
    }

    private fun notifyOpRemoved(parseOpPanel: ParseOpItemEditPanel) {
        for (listener in parseOpEventListeners) {
            listener.onOpRemoved(parseOpPanel)
        }
    }

    private fun updateOps() {
        ops = parseOpPanels.map { it.getParseOp() }
    }

    override fun onRequestAddOp(parseOpPanel: ParseOpItemEditPanel) {
        val index = parseOpPanels.indexOf(parseOpPanel)
        val newOpPanel = createParseOpPanel(parseOpPanel.getParseOp(), parentOp)
        addParseOpPanel(newOpPanel, index + 1)
        updateOps()
        notifyOpAdded(newOpPanel)
    }

    override fun onRequestRemoveOp(parseOpPanel: ParseOpItemEditPanel) {
        removeParseOpPanel(parseOpPanel)
        updateOps()
        notifyOpRemoved(parseOpPanel)
    }

    override fun onOpChanged(parseOpPanel: ParseOpItemEditPanel) {
        updateOps()
        if (container is ParseOpItemEditPanel) {
            notifyOpChanged(container)
        } else {
            notifyOpChanged(parseOpPanel)
        }
    }

    fun addFirstParseOpPanel() {
        val newOpPanel = createParseOpPanel(null, null, null)
        addParseOpPanel(newOpPanel)
        updateOps()
        notifyOpAdded(newOpPanel)
    }

    fun addOpPanel(parentOp: ParseOp) {
        val newOpPanel = createParseOpPanel(null, parentOp, null)
        addParseOpPanel(newOpPanel)
        updateOps()
        notifyOpAdded(newOpPanel)
    }

    private fun updateOpDependencies() {
        for (i in 0 until parseOpPanels.size) {
            val panel = parseOpPanels[i]
            val previousOp = if (i > 0) parseOpPanels[i - 1].getParseOp() else null
            panel.setPreviousParseOp(previousOp)
        }
    }

    override fun addOnScrollToEndListener(listener: OnScrollToEndListener) {
        onScrollToEndListeners.add(listener)
    }

    override fun removeOnScrollToEndListener(listener: OnScrollToEndListener) {
        onScrollToEndListeners.remove(listener)
    }

    private fun notifyOnScrollToEndListeners(event: MouseWheelEvent) {
        onScrollToEndListeners.forEach { it.onScrollToEnd(event) }
    }

    override fun onScrollToEnd(event: MouseWheelEvent) {
        notifyOnScrollToEndListeners(event)
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
    }
}