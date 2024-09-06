package me.gegenbauer.catspy.log.ui.customize

import info.clearthought.layout.TableLayout
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.ui.ScrollEventDelegator
import me.gegenbauer.catspy.utils.ui.showWarningDialog
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane

interface ILogMetadataDetail : Editor {
    var logMetadataEditModel: LogMetadataEditModel

    fun save(): Boolean

    fun cancelEdit()

    fun resetToBuiltIn()

    fun changeNightMode(isDark: Boolean)

    fun isEditing(): Boolean

    fun isModified(): Boolean

    fun addOnLogMetadataChangedListener(listener: (LogMetadataEditModel) -> Unit)

    fun addOnEditModeChangedListener(listener: (Boolean) -> Unit)
}

class LogMetadataDetailPanel : JPanel(), ILogMetadataDetail, EditEventListener {

    override var logMetadataEditModel: LogMetadataEditModel = LogMetadataModel.default.toEditModel()
        set(value) {
            field = value.copy(isDarkMode = editActionPanel.isNightMode)
            setLogMetadata(field)
        }

    private val logMetadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    private val contentPanel = LogMetadataDetailContentPanel()
    private val previewPanel = LogMetadataPreviewPanel()
    private val editActionPanel = EditActionPanel()
    private val onLogMetadataChangedListeners = mutableListOf<(LogMetadataEditModel) -> Unit>()
    private val onEditModeChangedListeners = mutableListOf<(Boolean) -> Unit>()
    private val contentContainer = JScrollPane(contentPanel)
    private val previewContainer = JScrollPane(previewPanel)
    private val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, contentContainer, previewContainer)
    private val scrollEventDelegator = ScrollEventDelegator(contentContainer)

    private val logMetadata: LogMetadataModel
        get() = logMetadataEditModel.model

    init {
        layout = TableLayout(
            doubleArrayOf(
                TableLayout.FILL
            ),
            doubleArrayOf(
                TableLayout.PREFERRED,
                TableLayout.FILL,
            )
        )
        add(editActionPanel, "0, 0")
        add(splitPane, "0, 1")

        editActionPanel.setEditEnabled(false)
        editActionPanel.bind(this)

        contentPanel.addOnScrollToEndListener(scrollEventDelegator)
        contentPanel.addEditEventListener(this)
        setContentVisible(false)
        showPreview(false)
    }

    private fun setLogMetadata(metadata: LogMetadataEditModel) {
        setContentVisible(metadata.model != LogMetadataModel.default)
        showPreview(metadata.model != LogMetadataModel.default)
        contentPanel.setLogMetadata(metadata)
        editActionPanel.setResetButtonVisible(metadata.model.isBuiltIn && logMetadataManager.isCustomized(metadata.model.logType))
        updatePreview(metadata)
    }

    private fun updatePreview(metadata: LogMetadataEditModel) {
        previewPanel.setMetadata(metadata)
    }

    private fun showPreview(show: Boolean) {
        splitPane.bottomComponent = if (show) previewContainer else null
        splitPane.setDividerLocation(1.0 - PREVIEW_HEIGHT_PERCENT)
    }

    private fun setContentVisible(visible: Boolean) {
        contentPanel.isVisible = visible
        editActionPanel.isVisible = visible
    }

    override fun save(): Boolean {
        val isEditValid = isEditValid()
        if (isEditValid) {
            stopEditing()
            val logMetadataModel = contentPanel.getUpdatedLogMetadata(logMetadata)
            if (logMetadataEditModel.isNew) {
                logMetadataManager.addNewLogMetadata(logMetadataModel)
            } else {
                if (logMetadataModel != logMetadata) {
                    logMetadataManager.modifyLogMetadata(logMetadata, logMetadataModel)
                    logMetadataEditModel = logMetadataModel.toEditModel(
                        id = logMetadataEditModel.id,
                        isNightMode = logMetadataEditModel.isDarkMode
                    )
                    if (logMetadataModel.isBuiltIn) {
                        editActionPanel.setResetButtonVisible(true)
                    }
                    notifyLogMetadataChanged(logMetadataEditModel)
                }
            }
        }
        return isEditValid
    }

    override fun cancelEdit() {
        if (logMetadataEditModel.isNew) {
            logMetadataEditModel = logMetadataEditModel.copy(isDeleted = true)
            notifyLogMetadataChanged(logMetadataEditModel)
        } else {
            contentPanel.setLogMetadata(logMetadataEditModel)
        }
        updatePreview(logMetadataEditModel)
        stopEditing()
    }

    override fun resetToBuiltIn() {
        if (showResetWarning(logMetadata.logType)) {
            val builtInMetadata = logMetadataManager.resetToBuiltIn(logMetadata.logType)
            val editModel = builtInMetadata.toEditModel(
                id = logMetadataEditModel.id,
                isNightMode = editActionPanel.isNightMode
            )
            if (logMetadata != builtInMetadata) {
                notifyLogMetadataChanged(editModel)
            }
            logMetadataEditModel = editModel
        }
    }

    override fun changeNightMode(isDark: Boolean) {
        contentPanel.onNightModeChanged(isDark)
        previewPanel.onNightModeChanged(logMetadataEditModel.copy(isDarkMode = isDark))
    }

    private fun showResetWarning(logType: String): Boolean {
        val actions = listOf(
            STRINGS.ui.reset to { true },
            STRINGS.ui.cancel to { false }
        )
        return showWarningDialog(
            null,
            EMPTY_STRING,
            STRINGS.ui.resetToBuiltInMetadataWarning.get(logType),
            actions,
            defaultChoice = 1
        )
    }

    override fun stopEditing() {
        contentPanel.stopEditing()
        editActionPanel.setEditingState(false)
        notifyEditModeChanged(false)
    }

    override fun startEditing() {
        editActionPanel.startEditing()
        contentPanel.startEditing()
        notifyEditModeChanged(true)
    }

    override fun isEditValid(): Boolean {
        return contentPanel.isEditValid()
    }

    override fun isEditing(): Boolean {
        return editActionPanel.isEditing
    }

    override fun isModified(): Boolean {
        return contentPanel.isModified()
    }

    override fun addOnLogMetadataChangedListener(listener: (LogMetadataEditModel) -> Unit) {
        onLogMetadataChangedListeners.add(listener)
    }

    private fun notifyLogMetadataChanged(logMetadataModel: LogMetadataEditModel) {
        onLogMetadataChangedListeners.toList().forEach { it(logMetadataModel) }
    }

    override fun addOnEditModeChangedListener(listener: (Boolean) -> Unit) {
        onEditModeChangedListeners.add(listener)
    }

    private fun notifyEditModeChanged(editing: Boolean) {
        onEditModeChangedListeners.toList().forEach { it(editing) }
    }

    override fun onEditDone(component: JComponent) {
        val editModel = contentPanel.getUpdatedLogMetadata(logMetadata)
            .toEditModel(id = logMetadataEditModel.id, isNightMode = editActionPanel.isNightMode)
        updatePreview(editModel)
        notifyLogMetadataChanged(editModel)
    }

    companion object {
        private const val PREVIEW_HEIGHT_PERCENT = 0.3
    }
}