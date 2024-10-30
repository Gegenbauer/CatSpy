package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.metadata.LogMetadataChangeListener
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.strings.STRINGS
import java.awt.Component
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.lang.ref.WeakReference
import javax.swing.*

interface OnMetadataChangedListener {
    fun onLogMetadataSelected(metadata: LogMetadataModel) {}

    fun onSelectedMetadataModified(modifiedMetadata: LogMetadataModel) {}
}

interface MetadataSelector {

    fun setExcludedMetadata(excludedLogType: String)

    /**
     * Do not pass in an instance of an anonymous inner class created in a method,
     * it will be recycled after the method is completed.
     */
    fun addOnSelectedMetadataChangedListener(listener: OnMetadataChangedListener)
}

fun interface OnMetadataConfChangedListener {
    fun onMetadataConfChanged(metadataGroup: List<LogMetadataModel>)
}

fun interface MetadataConfChangeEventSource {

    /**
     * Do not pass in an instance of an anonymous inner class created in a method,
     * it will be recycled after the method is completed.
     */
    fun addOnMetadataConfChangedListener(listener: OnMetadataConfChangedListener)
}

class LogMetadataConfMonitor : LogMetadataChangeListener, MetadataConfChangeEventSource {

    private val onMetadataConfChangedListeners = mutableListOf<WeakReference<OnMetadataConfChangedListener>>()

    private val logMetadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    init {
        logMetadataManager.addOnMetadataChangeListener(this)
    }

    /**
     * invoked when metadata is modified
     */
    override fun onMetadataChanged(old: LogMetadataModel, new: LogMetadataModel) {
        notifyMetadataConfChanged()
    }

    override fun onMetadataAdded(metadata: LogMetadataModel) {
        notifyMetadataConfChanged()
    }

    override fun onMetadataDeleted(logType: String) {
        notifyMetadataConfChanged()
    }

    override fun addOnMetadataConfChangedListener(listener: OnMetadataConfChangedListener) {
        onMetadataConfChangedListeners.add(WeakReference(listener))
        listener.onMetadataConfChanged(logMetadataManager.getAllFileLogMetadata())
    }

    private fun notifyMetadataConfChanged() {
        val metadataGroup = logMetadataManager.getAllFileLogMetadata()
        onMetadataConfChangedListeners.forEach { it.get()?.onMetadataConfChanged(metadataGroup) }
    }
}

class LogMetadataTemplateComboBox : JComboBox<LogMetadataModel>(),
    Editor, MetadataSelector, LogMetadataChangeListener, ActionListener, OnMetadataConfChangedListener {

    private val logMetadataConfMonitor = LogMetadataConfMonitor()
    private val onMetadataChangedListeners = mutableListOf<WeakReference<OnMetadataChangedListener>>()

    private var excludedLogType: String? = null

    private val model: DefaultComboBoxModel<LogMetadataModel>
        get() = getModel() as DefaultComboBoxModel<LogMetadataModel>

    private val logMetadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    private val selectionChangeListener = ItemListener {
        if (!isEnabled) {
            return@ItemListener
        }
        if (it.stateChange == ItemEvent.SELECTED) {
            val selected = selectedItem as LogMetadataModel
            notifySelectedMetadataChanged(selected)
        }
    }

    init {
        setRenderer(Renderer())
        addItemListener(selectionChangeListener)
        logMetadataConfMonitor.addOnMetadataConfChangedListener(this)
    }

    fun selectMetadata(logType: String) {
        val metadata = logMetadataManager.getMetadata(logType)
        if (metadata != null) {
            selectedItem = metadata
        }
    }

    override fun setExcludedMetadata(excludedLogType: String) {
        this.excludedLogType = excludedLogType
    }

    override fun addOnSelectedMetadataChangedListener(listener: OnMetadataChangedListener) {
        onMetadataChangedListeners.add(WeakReference(listener))
    }

    private fun notifySelectedMetadataChanged(metadata: LogMetadataModel) {
        onMetadataChangedListeners.forEach { it.get()?.onLogMetadataSelected(metadata) }
    }

    private fun notifySelectedMetadataModified(metadata: LogMetadataModel) {
        onMetadataChangedListeners.forEach { it.get()?.onSelectedMetadataModified(metadata) }
    }

    override fun onMetadataConfChanged(metadataGroup: List<LogMetadataModel>) {
        removeItemListener(selectionChangeListener)
        val selected = selectedItem as? LogMetadataModel
        removeAllItems()
        model.addAll(metadataGroup.filter {
            it.logType != excludedLogType && it.logType != LogMetadataManager.LOG_TYPE_FILE_GROUP_SEARCH_RESULT
        })
        ensureCurrentSelection(selected, metadataGroup)
        addItemListener(selectionChangeListener)
    }

    private fun ensureCurrentSelection(currentSelection: LogMetadataModel?, newOptions: List<LogMetadataModel>) {
        if (currentSelection != null) {
            val updatedSelectedItem = newOptions.firstOrNull { it.logType == currentSelection.logType }
            if (updatedSelectedItem != null) {
                // modified
                selectedIndex = newOptions.indexOf(updatedSelectedItem)
                if (selectedItem != currentSelection) {
                    notifySelectedMetadataModified(selectedItem as LogMetadataModel)
                }
            } else {
                // deleted
                model.insertElementAt(currentSelection, 0)
                selectedIndex = 0
            }
        }
    }

    override fun startEditing() {
        isEnabled = true
    }

    override fun stopEditing() {
        isEnabled = false
    }

    override fun isEditValid(): Boolean {
        return true
    }

    private class Renderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val logType = (value as? LogMetadataModel)?.logType
            return super.getListCellRendererComponent(list, logType, index, isSelected, cellHasFocus)
        }
    }
}

class LogMetadataTemplateLoaderButton : JButton(), MetadataSelector {

    private val onMetadataChangedListeners = mutableListOf<WeakReference<OnMetadataChangedListener>>()

    private val logMetadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    private var excludedLogType: String? = null

    init {
        text = STRINGS.ui.loadTemplate
        addActionListener { showTemplateSelectPopup() }
    }

    override fun setExcludedMetadata(excludedLogType: String) {
        this.excludedLogType = excludedLogType
    }

    private fun showTemplateSelectPopup() {
        val metadataGroup = logMetadataManager.getAllFileLogMetadata().filter { it.logType != excludedLogType }
        val selectorPopup = TemplateSelectorPopup(metadataGroup)
        selectorPopup.show(this, 0, height)
    }

    override fun addOnSelectedMetadataChangedListener(listener: OnMetadataChangedListener) {
        onMetadataChangedListeners.add(WeakReference(listener))
    }

    private fun notifySelectedMetadataChanged(metadata: LogMetadataModel) {
        onMetadataChangedListeners.forEach { it.get()?.onLogMetadataSelected(metadata) }
    }

    private inner class TemplateSelectorPopup(metadataGroup: List<LogMetadataModel>) : JPopupMenu() {
        init {
            metadataGroup.forEach { metadata ->
                add(JMenuItem(metadata.logType).apply {
                    addActionListener {
                        notifySelectedMetadataChanged(metadata)
                    }
                })
            }
            pack()
        }
    }
}