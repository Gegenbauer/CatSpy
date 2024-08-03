package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel

abstract class StringValueEditPanel(
    label: String,
    tooltip: String,
    private val maxCharCount: Int,
    verifier: ParamVerifier,
    var alwaysUnEditable: Boolean = false,
) : JPanel(), LogMetadataEditor, EditEventSource, EditEventListener {

    var value: String
        get() = valueField.text
        set(value) {
            valueField.text = value
        }

    // TODO maxCharCount 目前不生效
    private val valueField = ParamEditor(tooltip, maxCharCount)
    private val editEventListeners = mutableListOf<EditEventListener>()

    init {
        border = BorderFactory.createTitledBorder(label)
        layout = BorderLayout()
        add(valueField, BorderLayout.CENTER)
        valueField.setVerifier(verifier)
        valueField.addEditEventListener(this)
    }

    override fun getPreferredSize(): Dimension {
        if (valueField.text.length > maxCharCount) {
            return Dimension(valueField.preferredSize.width + 10, valueField.preferredSize.height * 2)
        }
        return super.getPreferredSize()
    }

    override fun getMaximumSize(): Dimension {
        if (valueField.text.length > maxCharCount) {
            return Dimension(valueField.maximumSize.width + 10, valueField.maximumSize.height * 2)
        }
        return super.getMaximumSize()
    }

    override fun addEditEventListener(listener: EditEventListener) {
        editEventListeners.add(listener)
    }

    override fun onEditDone(component: JComponent) {
        editEventListeners.forEach { it.onEditDone(this) }
    }

    override fun startEditing() {
        if (!alwaysUnEditable) {
            valueField.startEditing()
        }
    }

    override fun stopEditing() {
        valueField.stopEditing()
    }

    override fun isEditValid(): Boolean {
        return valueField.isEditValid()
    }
}

class StringValueEditPanelBuilder {
    private var label: String = ""
    private var maxCharCount: Int = Int.MAX_VALUE
    private var tooltip: String = ""
    private var editorVerifier: ParamVerifier = ParamVerifier.default
    private var alwaysUnEditablePredicate: (LogMetadataModel) -> Boolean = { false }
    private var logMetadataHandler: StringValueEditPanel.(LogMetadataModel) -> Unit = {}

    fun label(buildAction: () -> String) {
        label = buildAction()
    }

    fun maxCharCount(buildAction: () -> Int) {
        maxCharCount = buildAction()
    }

    fun tooltip(buildAction: () -> String) {
        tooltip = buildAction()
    }

    fun verifier(paramVerifierFunction: (JComponent) -> ParamVerifier.Result) {
        editorVerifier = ParamVerifier { paramVerifierFunction(it) }
    }

    fun alwaysUnEditable(alwaysUnEditablePredicate: (LogMetadataModel) -> Boolean) {
        this.alwaysUnEditablePredicate = alwaysUnEditablePredicate
    }

    fun logMetadataHandler(logMetadataHandler: StringValueEditPanel.(LogMetadataModel) -> Unit) {
        this.logMetadataHandler = logMetadataHandler
    }

    fun build(): StringValueEditPanel {
        return object : StringValueEditPanel(label, tooltip, maxCharCount, editorVerifier) {
            override fun setLogMetadata(metadata: LogMetadataEditModel) {
                logMetadataHandler(metadata.model)
                alwaysUnEditable = alwaysUnEditablePredicate(metadata.model)
            }
        }
    }
}

fun stringValueEditPanel(buildAction: StringValueEditPanelBuilder.() -> Unit): StringValueEditPanel {
    val builder = StringValueEditPanelBuilder()
    builder.buildAction()
    return builder.build()
}
