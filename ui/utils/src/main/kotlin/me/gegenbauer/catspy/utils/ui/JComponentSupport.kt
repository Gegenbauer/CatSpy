package me.gegenbauer.catspy.utils.ui

import me.gegenbauer.catspy.java.ext.getFieldDeeply
import java.awt.Dimension
import java.awt.Insets
import java.awt.datatransfer.DataFlavor
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.File
import java.util.*
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.TransferHandler
import javax.swing.border.Border

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

val EMPTY_INSETS = Insets(0, 0, 0, 0)

fun JComponent.getSizeWithPadding(size: Dimension): Dimension {
    return if (isVisible) {
        Dimension(size.width + horizontalPadding(), size.height + verticalPadding())
    } else {
        Dimension(0, 0)
    }
}

fun JComponent.horizontalPadding(): Int {
    val insets = insets
    return if (isVisible) {
        insets.left + insets.right
    } else {
        0
    }
}

fun JComponent.verticalPadding(): Int {
    val insets = insets
    return if (isVisible) {
        insets.top + insets.bottom
    } else {
        0
    }
}

fun createSpace(width: Int = 0, height: Int = 0): JComponent {
    return object : JComponent() {
        override fun getPreferredSize(): Dimension {
            return Dimension(width, height)
        }
    }
}

fun createBorder(
    top: Int = 0,
    left: Int = 0,
    bottom: Int = 0,
    right: Int = 0
): Border {
    return BorderFactory.createEmptyBorder(top, left, bottom, right)
}