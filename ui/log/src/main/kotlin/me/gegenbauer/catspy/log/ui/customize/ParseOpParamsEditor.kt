package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.serialize.*
import me.gegenbauer.catspy.strings.STRINGS
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

fun interface EditEventListener {
    fun onEditDone(component: JComponent)
}

interface EditEventSource {

    fun addEditEventListener(listener: EditEventListener)

    fun removeEditEventListener(listener: EditEventListener) {}
}

abstract class ParseOpParamsEditor<T : ParseOp> : JPanel(), EditEventSource, Editor {

    abstract val parseOp: T

    private val editListeners = mutableListOf<EditEventListener>()

    var partCount: Int = DEFAULT_MAX_PARTS
        set(value) {
            field = value
            onPartCountChanged(value)
        }

    fun registerEditorEvents() {
        getEditEventObservables().forEach { it.addEditEventListener { notifyEditListeners() } }
    }

    /**
     * Set the maximum number of parts into which the input log line is split
     */
    open fun onPartCountChanged(partCount: Int) {
        // do nothing by default
    }

    private fun notifyEditListeners() {
        editListeners.forEach { it.onEditDone(this) }
    }

    override fun addEditEventListener(listener: EditEventListener) {
        editListeners.add(listener)
    }

    override fun startEditing() {
        getEditEventObservables().filterIsInstance<Editor>().forEach { it.startEditing() }
    }

    override fun stopEditing() {
        getEditEventObservables().filterIsInstance<Editor>().forEach { it.stopEditing() }
    }

    override fun isEditValid(): Boolean {
        return getEditEventObservables().filterIsInstance<Editor>().all { it.isEditValid() }
    }

    abstract fun getEditEventObservables(): List<EditEventSource>

    companion object {
        private const val DEFAULT_MAX_PARTS = 20
    }
}

private class NumberParamEditor(tooltip: String? = null, min: Int, max: Int = Int.MAX_VALUE) : ParamEditor(tooltip) {

    private var range: Range = Range(min, max)

    init {
        setRange(min, max)
    }

    fun setRange(min: Int, max: Int) {
        range = Range(min, max)
        setVerifier(IntVerifier(min, max))
        reVerify()
    }

    data class Range(val min: Int, val max: Int)
}

private class CharParamEditor(tooltip: String? = null) : ParamEditor(tooltip) {

    var value: Char?
        get() = text.firstOrNull()
        set(value) {
            text = value?.toString() ?: EMPTY_STRING
        }

    init {
        setVerifier(CharVerifier())
    }
}

class EmptyOpParamsEditor(override val parseOp: ParseOp) : ParseOpParamsEditor<ParseOp>() {

    override fun getEditEventObservables(): List<EditEventSource> {
        return emptyList()
    }
}

class SplitByWordSeparatorOpParamsEditor(maxParts: Int) : ParseOpParamsEditor<SplitByWordSeparatorOp>() {

    override val parseOp: SplitByWordSeparatorOp
        get() = SplitByWordSeparatorOp(maxParts)

    private var maxParts: Int = maxParts
        get() = editor.text.toIntOrNull() ?: SplitByWordSeparatorOp.DEFAULT_MAX_PARTS
        private set(value) {
            editor.text = value.toString()
            field = value
        }

    private val label = EditorLabel(STRINGS.ui.wordSeparatorMaxPartsLabel)
    private val editor = NumberParamEditor(STRINGS.toolTip.wordSeparatorMaxParts, 0, maxParts)

    init {
        editor.maximumSize = Dimension(EDITOR_WIDTH, editor.preferredSize.height)
        editor.minimumSize = Dimension(EDITOR_WIDTH, editor.preferredSize.height)
        add(label)
        add(editor)

        this.maxParts = maxParts
        editor.setRange(0, SplitByWordSeparatorOp.DEFAULT_MAX_PARTS + 1)
    }

    override fun getEditEventObservables(): List<EditEventSource> {
        return listOf(editor)
    }

    companion object {
        private const val EDITOR_WIDTH = 50
    }
}

class SplitPartWithCharOpParamsEditor(
    char: Char?,
    targetPartIndex: Int,
    partCount: Int,
    maxParts: Int,
) : ParseOpParamsEditor<SplitPartWithCharOp>() {

    private var maxParts: Int = 0
        get() = maxPartsEditor.text.toIntOrNull() ?: Int.MAX_VALUE
        private set(value) {
            maxPartsEditor.text = value.toString()
            field = value
        }

    override val parseOp: SplitPartWithCharOp
        get() = SplitPartWithCharOp(char, targetPartIndex, maxParts)

    private var char: Char? = char
        get() = charEditor.value
        private set(value) {
            charEditor.value = value
            field = value
        }

    private var targetPartIndex: Int = targetPartIndex
        get() = partIndexEditor.text.toIntOrNull() ?: Int.MAX_VALUE
        private set(value) {
            partIndexEditor.text = value.toString()
            field = value
        }

    private val charLabel = EditorLabel(STRINGS.ui.splitCharLabel)
    private val charEditor = CharParamEditor(STRINGS.toolTip.splitChar)
    private val targetPartIndexLabel = EditorLabel(STRINGS.ui.targetPartIndexLabel)
    private val partIndexEditor = NumberParamEditor(STRINGS.toolTip.targetPartIndex, 0, partCount)

    private val maxPartsLabel = EditorLabel(STRINGS.ui.maxPartsLabel)
    private val maxPartsEditor = NumberParamEditor(STRINGS.toolTip.maxParts, 0, 20)

    init {
        val fieldWidth = 50

        partIndexEditor.maximumSize = Dimension(fieldWidth, partIndexEditor.preferredSize.height)
        partIndexEditor.minimumSize = Dimension(fieldWidth, partIndexEditor.preferredSize.height)
        add(targetPartIndexLabel)
        add(partIndexEditor)

        charEditor.maximumSize = Dimension(fieldWidth, charEditor.preferredSize.height)
        charEditor.minimumSize = Dimension(fieldWidth, charEditor.preferredSize.height)
        add(charLabel)
        add(charEditor)

        maxPartsEditor.maximumSize = Dimension(fieldWidth, maxPartsEditor.preferredSize.height)
        maxPartsEditor.minimumSize = Dimension(fieldWidth, maxPartsEditor.preferredSize.height)
        add(maxPartsLabel)
        add(maxPartsEditor)

        this.char = char
        this.targetPartIndex = targetPartIndex
        this.maxParts = maxParts
    }

    override fun onPartCountChanged(partCount: Int) {
        partIndexEditor.setRange(0, partCount)
    }

    override fun getEditEventObservables(): List<EditEventSource> {
        return listOf(charEditor, partIndexEditor, maxPartsEditor)
    }
}

class MergeNearbyPartsOpParamsEditor(
    from: Int,
    to: Int,
    partCount: Int
) : ParseOpParamsEditor<MergeNearbyPartsOp>() {

    override val parseOp: MergeNearbyPartsOp
        get() = MergeNearbyPartsOp(from, to)

    private var from: Int = from
        get() = fromEditor.text.toIntOrNull() ?: Int.MAX_VALUE
        private set(value) {
            fromEditor.text = value.toString()
            field = value
        }

    private var to: Int = to
        get() = toEditor.text.toIntOrNull() ?: Int.MAX_VALUE
        private set(value) {
            toEditor.text = value.toString()
            field = value
        }

    private val fromLabel = EditorLabel(STRINGS.ui.mergeNearByPartsFromIndexLabel)
    private val fromEditor = NumberParamEditor(STRINGS.toolTip.mergeNearByPartsFromIndex, 0, partCount)
    private val toLabel = EditorLabel(STRINGS.ui.mergeNearByPartsToIndexLabel)
    private val toEditor = NumberParamEditor(STRINGS.toolTip.mergeNearByPartsToIndex, 0, partCount)

    init {
        val fieldWidth = 50
        fromEditor.maximumSize = Dimension(fieldWidth, fromEditor.preferredSize.height)
        fromEditor.minimumSize = Dimension(fieldWidth, fromEditor.preferredSize.height)
        add(fromLabel)
        add(fromEditor)

        toEditor.maximumSize = Dimension(fieldWidth, toEditor.preferredSize.height)
        toEditor.minimumSize = Dimension(fieldWidth, toEditor.preferredSize.height)
        add(toLabel)
        add(toEditor)

        this.from = from
        this.to = to.takeIf { it > from } ?: (from + 1)

        fromEditor.addEditEventListener {
            toEditor.setRange(this.from, this.partCount)
        }

        toEditor.addEditEventListener {
            fromEditor.setRange(0, this.to)
        }
    }

    override fun onPartCountChanged(partCount: Int) {
        fromEditor.setRange(0, this.to)
        toEditor.setRange(this.from, this.partCount)
    }

    override fun getEditEventObservables(): List<EditEventSource> {
        return listOf(fromEditor, toEditor)
    }
}

class MergeUntilCharOpParamsEditor(
    start: Int = 0,
    partCount: Int,
    targetChar: Char? = null,
) : ParseOpParamsEditor<MergeUntilCharOp>() {

    override val parseOp: MergeUntilCharOp
        get() = MergeUntilCharOp(start, targetChar)

    private var start: Int = start
        get() = startEditor.text.toIntOrNull() ?: Int.MAX_VALUE
        private set(value) {
            startEditor.text = value.toString()
            field = value
        }

    private var targetChar: Char? = targetChar
        get() = targetCharEditor.value
        private set(value) {
            targetCharEditor.value = value
            field = value
        }

    private val startLabel = EditorLabel(STRINGS.ui.mergeUntilCharStartIndexLabel)
    private val startEditor = NumberParamEditor(STRINGS.toolTip.mergeUntilCharStartIndex, 0, partCount)
    private val targetCharLabel = EditorLabel(STRINGS.ui.mergeUntilCharTargetCharLabel)
    private val targetCharEditor = CharParamEditor(STRINGS.toolTip.mergeUntilCharTargetChar)

    init {
        val fieldWidth = 50
        startEditor.maximumSize = Dimension(fieldWidth, startEditor.preferredSize.height)
        startEditor.minimumSize = Dimension(fieldWidth, startEditor.preferredSize.height)
        add(startLabel)
        add(startEditor)

        targetCharEditor.maximumSize = Dimension(fieldWidth, targetCharEditor.preferredSize.height)
        targetCharEditor.minimumSize = Dimension(fieldWidth, targetCharEditor.preferredSize.height)
        add(targetCharLabel)
        add(targetCharEditor)

        this.start = start
        this.targetChar = targetChar
    }

    override fun onPartCountChanged(partCount: Int) {
        startEditor.setRange(0, partCount)
    }

    override fun getEditEventObservables(): List<EditEventSource> {
        return listOf(startEditor, targetCharEditor)
    }
}

class TrimWithCharOpParamsEditor(
    targetPartIndex: Int,
    partCount: Int,
    leadingChar: Char? = null,
    trailingChar: Char? = null,
) : ParseOpParamsEditor<TrimWithCharOp>() {

    override val parseOp: TrimWithCharOp
        get() = TrimWithCharOp(targetPartIndex, leadingChar, trailingChar)

    private var leadingChar: Char? = leadingChar
        get() = leadingCharEditor.value
        private set(value) {
            leadingCharEditor.value = value
            field = value
        }

    private var trailingChar: Char? = trailingChar
        get() = trailingCharEditor.value
        private set(value) {
            trailingCharEditor.value = value
            field = value
        }

    private var targetPartIndex: Int = targetPartIndex
        get() = targetPartIndexEditor.text.toIntOrNull() ?: Int.MAX_VALUE
        private set(value) {
            targetPartIndexEditor.text = value.toString()
            field = value
        }

    private val leadingCharLabel = EditorLabel(STRINGS.ui.trimLeadingCharLabel)
    private val leadingCharEditor = CharParamEditor(STRINGS.toolTip.trimLeadingChar)
    private val tailingCharLabel = EditorLabel(STRINGS.ui.trimTrailingCharLabel)
    private val trailingCharEditor = CharParamEditor(STRINGS.toolTip.trimTrailingChar)
    private val targetPartIndexLabel = EditorLabel(STRINGS.ui.targetPartIndexLabel)
    private val targetPartIndexEditor = NumberParamEditor(STRINGS.toolTip.targetPartIndex, 0, partCount)


    init {
        val fieldWidth = 50

        targetPartIndexEditor.maximumSize = Dimension(fieldWidth, targetPartIndexEditor.preferredSize.height)
        targetPartIndexEditor.minimumSize = Dimension(fieldWidth, targetPartIndexEditor.preferredSize.height)
        add(targetPartIndexLabel)
        add(targetPartIndexEditor)

        leadingCharEditor.maximumSize = Dimension(fieldWidth, leadingCharEditor.preferredSize.height)
        leadingCharEditor.minimumSize = Dimension(fieldWidth, leadingCharEditor.preferredSize.height)
        add(leadingCharLabel)
        add(leadingCharEditor)

        trailingCharEditor.maximumSize = Dimension(fieldWidth, trailingCharEditor.preferredSize.height)
        trailingCharEditor.minimumSize = Dimension(fieldWidth, trailingCharEditor.preferredSize.height)
        add(tailingCharLabel)
        add(trailingCharEditor)

        this.targetPartIndex = targetPartIndex
        this.leadingChar = leadingChar
        this.trailingChar = trailingChar
    }

    override fun onPartCountChanged(partCount: Int) {
        targetPartIndexEditor.setRange(0, partCount)
    }

    override fun getEditEventObservables(): List<EditEventSource> {
        return listOf(leadingCharEditor, trailingCharEditor, targetPartIndexEditor)
    }
}

class TrimWithIndexOpParamsEditor(
    targetPartIndex: Int,
    partCount: Int,
    removedLeadingCharCount: Int,
    removedTrailingCharCount: Int,
) : ParseOpParamsEditor<TrimWithIndexOp>() {

    override val parseOp: TrimWithIndexOp
        get() = TrimWithIndexOp(targetPartIndex, removedLeadingCharCount, removedTrailingCharCount)

    private var targetPartIndex: Int = targetPartIndex
        get() = targetPartIndexEditor.text.toIntOrNull() ?: Int.MAX_VALUE
        private set(value) {
            targetPartIndexEditor.text = value.toString()
            field = value
        }

    private var removedLeadingCharCount: Int = removedLeadingCharCount
        get() = removedLeadingCharCountEditor.text.toIntOrNull() ?: Int.MAX_VALUE
        private set(value) {
            removedLeadingCharCountEditor.text = value.toString()
            field = value
        }

    private var removedTrailingCharCount: Int = removedTrailingCharCount
        get() = removedTrailingCharCountEditor.text.toIntOrNull() ?: Int.MAX_VALUE
        private set(value) {
            removedTrailingCharCountEditor.text = value.toString()
            field = value
        }

    private val targetPartIndexLabel = EditorLabel(STRINGS.ui.targetPartIndexLabel)
    private val removedLeadingCharCountLabel = EditorLabel(STRINGS.ui.trimLeadingCharCountLabel)
    private val removedTrailingCharCountLabel = EditorLabel(STRINGS.ui.trimTrailingCharCountLabel)

    private val targetPartIndexEditor = NumberParamEditor(STRINGS.toolTip.targetPartIndex, 0, partCount)
    private val removedLeadingCharCountEditor = NumberParamEditor(STRINGS.toolTip.trimLeadingCharCount, 0, partCount)
    private val removedTrailingCharCountEditor = NumberParamEditor(STRINGS.toolTip.trimTrailingCharCount, 0, partCount)

    init {
        val fieldWidth = 50

        targetPartIndexEditor.maximumSize = Dimension(fieldWidth, targetPartIndexEditor.preferredSize.height)
        targetPartIndexEditor.minimumSize = Dimension(fieldWidth, targetPartIndexEditor.preferredSize.height)
        add(targetPartIndexLabel)
        add(targetPartIndexEditor)

        removedLeadingCharCountEditor.maximumSize =
            Dimension(fieldWidth, removedLeadingCharCountEditor.preferredSize.height)
        removedLeadingCharCountEditor.minimumSize =
            Dimension(fieldWidth, removedLeadingCharCountEditor.preferredSize.height)
        add(removedLeadingCharCountLabel)
        add(removedLeadingCharCountEditor)

        removedTrailingCharCountEditor.maximumSize =
            Dimension(fieldWidth, removedTrailingCharCountEditor.preferredSize.height)
        removedTrailingCharCountEditor.minimumSize =
            Dimension(fieldWidth, removedTrailingCharCountEditor.preferredSize.height)
        add(removedTrailingCharCountLabel)
        add(removedTrailingCharCountEditor)

        this.targetPartIndex = targetPartIndex
        this.removedLeadingCharCount = removedLeadingCharCount
        this.removedTrailingCharCount = removedTrailingCharCount

        removedLeadingCharCountEditor.addEditEventListener {
            removedTrailingCharCountEditor.setRange(0, this.partCount - this.removedLeadingCharCount)
        }
        removedTrailingCharCountEditor.addEditEventListener {
            removedLeadingCharCountEditor.setRange(0, this.partCount - this.removedTrailingCharCount)
        }
    }

    override fun onPartCountChanged(partCount: Int) {
        targetPartIndexEditor.setRange(0, partCount)
        removedLeadingCharCountEditor.setRange(0, this.partCount - this.removedLeadingCharCount)
        removedTrailingCharCountEditor.setRange(0, this.partCount - this.removedTrailingCharCount)
    }

    override fun getEditEventObservables(): List<EditEventSource> {
        return listOf(targetPartIndexEditor, removedLeadingCharCountEditor, removedTrailingCharCountEditor)
    }
}
