package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.showWarningDialog
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

interface LogMetadataListController {
    var data: List<LogMetadataModel>

    var detailController: ILogMetadataDetail

    fun bind(detailController: ILogMetadataDetail) {
        this.detailController = detailController
        addSelectedItemChangeListener {
            detailController.stopEditing()
            detailController.logMetadataEditModel = it ?: LogMetadataModel.default.toEditModel()
        }
    }

    fun addSelectedItemChangeListener(listener: (LogMetadataEditModel?) -> Unit)
}

class LogMetadataListPanel : JPanel(), LogMetadataListController, ListSelectionListener {

    override var data: List<LogMetadataModel> = emptyList()
        set(value) {
            field = value
            setLogMetadataList(value.map { it.toEditModel() })
        }

    override lateinit var detailController: ILogMetadataDetail

    private val addButton = JButton(STRINGS.ui.createNewMetadata)
    private val deleteButton = JButton(STRINGS.ui.deleteMetadata).apply {
        toolTipText = STRINGS.toolTip.deleteMetadata
        isEnabled = false
    }
    private val logMetadataListModel =
        DefaultListModel<LogMetadataEditModel>() // 这里只是一个示例，你需要根据实际需求来创建 DefaultListModel
    private val logMetadataList = JList(logMetadataListModel)
    private val logMetadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    private val selectedItemChangeListeners = mutableListOf<(LogMetadataEditModel?) -> Unit>()
    private var lastSelectedLogMetadata: LogMetadataEditModel? = null

    init {
        layout = BorderLayout()

        // 创建按钮面板
        val buttonPanel = JPanel()
        buttonPanel.add(addButton)
        buttonPanel.add(deleteButton)

        add(buttonPanel, BorderLayout.NORTH)

        add(JScrollPane(logMetadataList), BorderLayout.CENTER)

        logMetadataList.selectionModel = SelectionInterceptedModel()
        logMetadataList.cellRenderer = LogMetadataListCellRenderer()
        logMetadataList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        logMetadataList.selectionModel.addListSelectionListener(this)

        addButton.addActionListener { addNewLogMetadata() }
        deleteButton.addActionListener { deleteLogMetadata(logMetadataList.selectedValue) }
    }

    private fun addNewLogMetadata() {
        val rawLogMetadata = logMetadataManager.getMetadata(LogMetadataManager.LOG_TYPE_RAW)!!
        val nonDuplicateMetadata = rawLogMetadata.copy(
            logType = logMetadataManager.getUniqueLogType(rawLogMetadata.logType),
            isBuiltIn = false
        )
        val editModel = nonDuplicateMetadata.toEditModel(isNew = true)
        addLogMetadata(editModel)
        detailController.logMetadataEditModel = editModel
        selectLogMetadata(editModel)
        detailController.startEditing()
    }

    private fun addLogMetadata(logMetadata: LogMetadataEditModel) {
        logMetadataListModel.addElement(logMetadata)
    }

    private fun selectLogMetadata(logMetadata: LogMetadataEditModel) {
        val index = logMetadataListModel.indexOf(logMetadata)
        if (index != -1) {
            logMetadataList.selectedIndex = index
        }
    }

    override fun bind(detailController: ILogMetadataDetail) {
        super.bind(detailController)
        detailController.addOnLogMetadataChangedListener {
            val selectedLogMetadata = logMetadataList.selectedValue
            if (selectedLogMetadata.id == it.id) {
                updateLogMetadata(it, logMetadataList.selectedIndex)
            }
            if (it.isDeleted) {
                deleteLogMetadata(it)
            }
        }
        detailController.addOnEditModeChangedListener {
            if (it) {
                disableEditing()
            } else {
                enableEditing()
            }
        }
    }

    private fun updateLogMetadata(logMetadata: LogMetadataEditModel, index: Int) {
        logMetadataListModel.setElementAt(logMetadata, index)
        logMetadataList.repaint()
    }

    private fun enableEditing() {
        addButton.isEnabled = true
        deleteButton.isEnabled = true

        checkDeleteButtonState()
    }

    private fun disableEditing() {
        addButton.isEnabled = false
        deleteButton.isEnabled = false
    }

    private fun setLogMetadataList(logMetadata: List<LogMetadataEditModel>) {
        logMetadataListModel.clear()
        logMetadataListModel.addAll(logMetadata)
    }

    override fun addSelectedItemChangeListener(listener: (LogMetadataEditModel?) -> Unit) {
        selectedItemChangeListeners.add(listener)
    }

    private fun notifySelectedItemChangeListeners(selectedItem: LogMetadataEditModel?) {
        selectedItemChangeListeners.toList().forEach {
            it.invoke(selectedItem)
        }
    }

    override fun valueChanged(e: ListSelectionEvent) {
        val selectedLogMetadata = logMetadataList.selectedValue
        if (lastSelectedLogMetadata != selectedLogMetadata) {
            notifySelectedItemChangeListeners(selectedLogMetadata)
            checkDeleteButtonState()
            lastSelectedLogMetadata = selectedLogMetadata
        }
    }

    private fun checkDeleteButtonState() {
        if (logMetadataList.isSelectionEmpty || logMetadataList.selectedValue!!.model.isBuiltIn) {
            deleteButton.isEnabled = false
        }
    }

    private class LogMetadataListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            if (value is LogMetadataEditModel) {
                label.text = value.model.logType
            }
            return label
        }
    }

    private fun deleteLogMetadata(logMetadata: LogMetadataEditModel?) {
        logMetadata ?: return
        if (logMetadata.isNew.not() && !showDeletingWarning()) {
            return
        }
        logMetadataListModel.removeElement(logMetadata)
        logMetadataList.clearSelection()
        if (!logMetadata.isNew) {
            logMetadataManager.delete(logMetadata.model.logType)
        }
        repaint()
    }

    private fun showDeletingWarning(): Boolean {
        val actions = listOf(
            STRINGS.ui.deleteMetadata to { true },
            STRINGS.ui.cancel to { false }
        )
        return showWarningDialog(
            this.parent,
            "",
            STRINGS.ui.deleteMetadataWarning,
            actions
        )
    }

    private inner class SelectionInterceptedModel : DefaultListSelectionModel() {
        override fun setSelectionInterval(newIndex: Int, index1: Int) {
            if (newIndex == logMetadataList.selectedIndex) {
                return
            }
            val selectedIndex = logMetadataList.selectedIndex
            if (selectedIndex != -1 && detailController.isEditing() && detailController.isModified()) {
                if (!showEditingWarning()) {
                    return
                }
            }
            val selectedLogMetadata = logMetadataList.selectedValue
            if (selectedLogMetadata != null && selectedLogMetadata.isNew && detailController.isEditing()) {
                if (!showCreatingWarning()) {
                    return
                }
            }
            super.setSelectionInterval(newIndex, index1)
        }

        private fun showEditingWarning(): Boolean {
            val actions = listOf(
                STRINGS.ui.discardEditedAndSwitchMetadata to {
                    detailController.cancelEdit()
                    true
                },
                STRINGS.ui.saveEditedAndSwitchMetadata to {
                    detailController.save()
                    true
                },
                STRINGS.ui.cancel to { false }
            )
            return showWarningDialog(
                this@LogMetadataListPanel.parent,
                "",
                STRINGS.ui.discardEditedMetadataWarning,
                actions
            )
        }

        private fun showCreatingWarning(): Boolean {
            val actions = listOf(
                STRINGS.ui.createNewMetadata to {
                    detailController.cancelEdit()
                    deleteLogMetadata(logMetadataList.selectedValue)
                    true
                },
                STRINGS.ui.cancel to { false }
            )
            return showWarningDialog(
                this@LogMetadataListPanel.parent,
                "",
                STRINGS.ui.discardCreatedWarning,
                actions
            )
        }
    }
}