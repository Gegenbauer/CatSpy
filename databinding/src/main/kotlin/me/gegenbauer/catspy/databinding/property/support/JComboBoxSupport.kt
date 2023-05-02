package me.gegenbauer.catspy.databinding.property.support

import javax.swing.JComboBox
import javax.swing.event.EventListenerList
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

var JComboBox<*>.itemListenerList: EventListenerList?
    get() {
        val listenerListField = getFieldDeeply("listenerList")
        return listenerListField.get(this) as EventListenerList
    }
    set(value) {
        val listenerListField = getFieldDeeply("listenerList")
        listenerListField.set(this, value)
    }

var JComboBox<*>.listDataListenerList: EventListenerList?
    get() {
        val model = this.model
        val listenerListField = model.getFieldDeeply("listenerList")
        return listenerListField.get(model) as EventListenerList
    }
    set(value) {
        val model = this.model
        val listenerListField = model.getFieldDeeply("listenerList")
        listenerListField.set(model, value)
    }


fun <E> JComboBox<E>.withAllListenerDisabled(action: JComboBox<E>.() -> Unit) {
    val listDataListenerListCopy = listDataListenerList
    val itemListenerListCopy = itemListenerList
    val propertyChangeListenerListCopy = propertyChangeListenerList
    val isEditableCopy = isEditable
    listDataListenerList = EventListenerList()
    itemListenerList = EventListenerList()
    propertyChangeListenerList = null
    action.invoke(this)
    listDataListenerList = listDataListenerListCopy
    itemListenerList = itemListenerListCopy
    propertyChangeListenerList = propertyChangeListenerListCopy
    isEditable = isEditableCopy
}

fun <T> List<T>.contentEquals(target: List<T>): Boolean {
    if (this.size != target.size) {
        return false
    }
    for (i in this.indices) {
        if (this[i] != target[i]) {
            return false
        }
    }
    return true
}

open class DefaultListDataListener: ListDataListener {
    override fun intervalAdded(e: ListDataEvent) {
        // do nothing
    }

    override fun intervalRemoved(e: ListDataEvent) {
        // do nothing
    }

    override fun contentsChanged(e: ListDataEvent) {
        // do nothing
    }

}