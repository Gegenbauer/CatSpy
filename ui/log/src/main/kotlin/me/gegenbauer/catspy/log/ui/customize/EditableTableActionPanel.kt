package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.strings.STRINGS
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class EditableTableActionPanel(
    private val templateLoader: LogMetadataTemplateLoaderButton = LogMetadataTemplateLoaderButton()
) : CenteredDualDirectionPanel(), EditableContainer, MetadataSelector by templateLoader {

    override var isEditing: Boolean = false

    private val addButton = JButton(STRINGS.ui.addRow)
    private val removeButton = JButton(STRINGS.ui.removeRow)

    private val upButton = JButton(STRINGS.ui.moveRowUp)
    private val downButton = JButton(STRINGS.ui.moveRowDown)

    private val customActionButtons = mutableListOf<JButton>()
    private var actionVisibilityParam = ActionVisibilityParam()
    private var actionEnableStateParam = ActionEnableStateParam()

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        addLeft(addButton)
        addLeft(removeButton)
        addLeft(upButton)
        addLeft(downButton)

        addRight(templateLoader)
    }

    fun setCustomActions(actions: List<CustomAction>) {
        customActionButtons.forEach { remove(it) }
        customActionButtons.clear()

        var targetIndex = components.indexOf(downButton) + 1
        actions.forEach { (action, listener) ->
            val button = JButton(action)
            customActionButtons.add(button)
            button.addActionListener { listener() }
            addLeft(button, targetIndex++)
            button.isEnabled = isEditing
        }
    }

    fun setActionVisibilityParam(param: ActionVisibilityParam) {
        actionVisibilityParam = param
        upButton.isVisible = param.moveRowAction.isTrue()
        downButton.isVisible = param.moveRowAction.isTrue()
        addButton.isVisible = param.addOrRemoveRowAction.isTrue()
        removeButton.isVisible = param.addOrRemoveRowAction.isTrue()
        templateLoader.isVisible = param.loadTemplateAction.isTrue()
    }

    fun setActionEnableStateParam(param: ActionEnableStateParam) {
        actionEnableStateParam = param
        changeEditMode(isEditing)
    }

    override fun startEditing() {
        changeEditMode(true)

        isEditing = true
    }

    override fun stopEditing() {
        changeEditMode(false)

        isEditing = false
    }

    private fun changeEditMode(editMode: Boolean) {
        if (actionEnableStateParam.moveRowAction.isTrue()) {
            upButton.isEnabled = editMode
            downButton.isEnabled = editMode
        }
        if (actionEnableStateParam.addOrRemoveRowAction.isTrue()) {
            addButton.isEnabled = editMode
            removeButton.isEnabled = editMode
        }
        if (actionEnableStateParam.loadTemplateAction.isTrue()) {
            templateLoader.isEnabled = editMode
        }
        customActionButtons.forEach { it.isEnabled = editMode }
    }

    private fun setAddActionListener(listener: () -> Unit) {
        addButton.addActionListener { listener() }
    }

    private fun setRemoveActionListener(listener: () -> Unit) {
        removeButton.addActionListener { listener() }
    }

    private fun setUpActionListener(listener: () -> Unit) {
        upButton.addActionListener { listener() }
    }

    private fun setDownActionListener(listener: () -> Unit) {
        downButton.addActionListener { listener() }
    }

    fun bindTable(
        table: JTable,
        defaultRowItemProvider: () -> List<Any?>,
    ) {
        setAddActionListener {
            (table.model as DefaultTableModel).addRow(defaultRowItemProvider().toTypedArray())
        }
        setRemoveActionListener {
            val selectedRow = table.selectedRow
            if (selectedRow != -1) {
                (table.model as DefaultTableModel).removeRow(selectedRow)
                if (selectedRow < table.rowCount) {
                    table.setRowSelectionInterval(selectedRow, selectedRow)
                } else if (selectedRow > 0) {
                    table.setRowSelectionInterval(selectedRow - 1, selectedRow - 1)
                }
            }
        }
        setUpActionListener { moveSelectedRow(table, toLeftOrRight = true)}
        setDownActionListener { moveSelectedRow(table, toLeftOrRight = false)}
    }

    private fun moveSelectedRow(table: JTable, toLeftOrRight: Boolean) {
        val selectedRow = table.selectedRow
        if (selectedRow !in 0 until table.rowCount) {
            return
        }
        val newRowIndex = (selectedRow + if (toLeftOrRight) -1 else 1)
            .coerceIn(0, table.rowCount - 1)
        (table.model as DefaultTableModel).moveRow(selectedRow, selectedRow, newRowIndex)
        table.setRowSelectionInterval(newRowIndex, newRowIndex)
    }

    data class CustomAction(
        val action: String,
        val listener: () -> Unit
    )

    data class ActionVisibilityParam(
        val moveRowAction: StateControlParam = StateControlParam(),
        val addOrRemoveRowAction: StateControlParam = StateControlParam(),
        val loadTemplateAction: StateControlParam = StateControlParam(),
    ) {
        fun setBuiltInState(isBuiltIn: Boolean) {
            moveRowAction.isBuiltIn = isBuiltIn
            addOrRemoveRowAction.isBuiltIn = isBuiltIn
            loadTemplateAction.isBuiltIn = isBuiltIn
        }
    }

    data class ActionEnableStateParam(
        val moveRowAction: StateControlParam = StateControlParam(),
        val addOrRemoveRowAction: StateControlParam = StateControlParam(),
        val loadTemplateAction: StateControlParam = StateControlParam(),
    )

    data class StateControlParam(
        val trueWhenNotBuiltIn: Boolean = true,
        val alwaysTrue: Boolean = false,
    ) {
        var isBuiltIn: Boolean = false

        fun isTrue(): Boolean {
            return alwaysTrue || (trueWhenNotBuiltIn && !isBuiltIn)
        }
    }
}