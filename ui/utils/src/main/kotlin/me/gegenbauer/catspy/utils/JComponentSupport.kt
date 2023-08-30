package me.gegenbauer.catspy.utils

import me.gegenbauer.catspy.java.ext.getFieldDeeply
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.*
import javax.swing.JComponent

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