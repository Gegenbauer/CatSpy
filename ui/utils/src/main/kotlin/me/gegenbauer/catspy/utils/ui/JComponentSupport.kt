package me.gegenbauer.catspy.utils.ui

import me.gegenbauer.catspy.java.ext.getFieldDeeply
import java.awt.datatransfer.DataFlavor
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.File
import java.util.*
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.TransferHandler

open class DefaultFocusListener : FocusListener {

    override fun focusGained(e: FocusEvent) {
        focusChanged(e)
    }

    override fun focusLost(e: FocusEvent) {
        focusChanged(e)
    }

    open fun focusChanged(e: FocusEvent) {
        // no-op
    }
}

fun JComponent.withPropertyChangeListenerDisabled(propertyName: String, action: () -> Unit) {
    val listenerList = propertyChangeListenerList ?: return
    val propertyChangeListenersCache = listenerList[propertyName]
    propertyChangeListenersCache ?: return
    listenerList.remove(propertyName)
    action()
    listenerList[propertyName] = propertyChangeListenersCache
}

@Suppress("UNCHECKED_CAST")
var JComponent.propertyChangeListenerList: MutableMap<String, Array<EventListener>>?
    get() {
        val changeSupportField = getFieldDeeply("vetoableChangeSupport")
        val changeListenerMapField = changeSupportField.get(this).getFieldDeeply("map")
        val changeListenerMap = changeListenerMapField.get(changeSupportField.get(this))
        val listenerMap = changeListenerMap.getFieldDeeply("map")
        return listenerMap.get(changeListenerMap) as? MutableMap<String, Array<EventListener>>
    }
    set(value) {
        val changeSupportField = getFieldDeeply("changeSupport")
        val changeListenerMapField = changeSupportField.get(this).getFieldDeeply("map")
        val changeListenerMap = changeListenerMapField.get(changeSupportField.get(this))
        val listenerMap = changeListenerMap.getFieldDeeply("map")
        listenerMap.set(changeListenerMap, value)
    }

fun getDefaultFileChooser(): JFileChooser {
    return JFileChooser().apply {
        isMultiSelectionEnabled = false
        fileSelectionMode = JFileChooser.FILES_ONLY
        isFileHidingEnabled = false
        dragEnabled = true
        isAcceptAllFileFilterUsed = true
        transferHandler = FileChooserTransferHandler(this)
    }
}

class FileChooserTransferHandler(private val chooser: JFileChooser) : TransferHandler() {
    override fun canImport(support: TransferSupport): Boolean {
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
    }

    override fun importData(support: TransferSupport): Boolean {
        if (!canImport(support)) {
            return false
        }
        val files = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
        if (files.isNotEmpty()) {
            val file = files[0] as? File ?: return false
            chooser.currentDirectory = file.parentFile
            chooser.selectedFile = file
            return true
        }
        return false
    }
}