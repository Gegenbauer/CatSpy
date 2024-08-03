package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.strings.STRINGS
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

class EditActionPanel : JPanel(), EditableContainer {

    override val isEditing: Boolean
        get() = saveButton.isEnabled

    private val editButton = JButton(STRINGS.ui.edit)
    private val saveButton = JButton(STRINGS.ui.save).apply { isEnabled = false }
    private val cancelButton = JButton(STRINGS.ui.cancel).apply { isEnabled = false }
    private val resetButton = JButton(STRINGS.ui.resetMetadata).apply {
        isEnabled = false
        toolTipText = STRINGS.toolTip.resetMetadata
    }

    private lateinit var controller: ILogMetadataDetail

    init {
        layout = FlowLayout(FlowLayout.RIGHT)
        add(editButton)
        add(saveButton)
        add(cancelButton)
        add(resetButton)

        editButton.addActionListener {
            controller.startEditing()
            startEditing()
        }
        saveButton.addActionListener {
            val result = controller.save()
            if (result) {
                stopEditing()
            }
        }
        cancelButton.addActionListener {
            controller.cancelEdit()
        }
        resetButton.addActionListener {
            controller.resetToBuiltIn()
        }
        setResetButtonVisible(false)
    }

    fun setEditEnabled(enabled: Boolean) {
        editButton.isEnabled = enabled
    }

    fun setEditingState(editing: Boolean) {
        editButton.isEnabled = !editing
        resetButton.isEnabled = !editing
        saveButton.isEnabled = editing
        cancelButton.isEnabled = editing
    }

    fun setResetButtonVisible(visible: Boolean) {
        resetButton.isVisible = visible
    }

    fun bind(controller: ILogMetadataDetail) {
        this.controller = controller
    }

    override fun startEditing() {
        changeEditMode(true)
    }

    override fun stopEditing() {
        changeEditMode(false)
    }

    private fun changeEditMode(editMode: Boolean) {
        editButton.isEnabled = !editMode
        saveButton.isEnabled = editMode
        cancelButton.isEnabled = editMode
        resetButton.isEnabled = !editMode
    }
}