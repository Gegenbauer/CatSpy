package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.ColorToggleButton
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

class EditActionPanel : JPanel(), EditableContainer {

    override val isEditing: Boolean
        get() = saveButton.isEnabled

    val isNightMode: Boolean
        get() = nightModeButton.isSelected

    private val editButton = JButton(STRINGS.ui.edit)
    private val saveButton = JButton(STRINGS.ui.save).apply { isEnabled = false }
    private val cancelButton = JButton(STRINGS.ui.cancel).apply { isEnabled = false }
    private val resetButton = JButton(STRINGS.ui.resetMetadata).apply {
        isEnabled = false
        toolTipText = STRINGS.toolTip.resetMetadata
    }
    private val nightModeButton = ColorToggleButton(STRINGS.ui.darkTheme)

    private lateinit var controller: ILogMetadataDetail

    init {
        layout = FlowLayout(FlowLayout.RIGHT)
        add(editButton)
        add(saveButton)
        add(cancelButton)
        add(resetButton)
        add(nightModeButton)

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
        nightModeButton.addActionListener {
            controller.changeNightMode(nightModeButton.isSelected)
        }
        setResetButtonVisible(false)
        nightModeButton.isSelected = ThemeManager.isDarkTheme
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