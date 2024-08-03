package me.gegenbauer.catspy.databinding.property.adapter

import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import me.gegenbauer.catspy.utils.ui.DefaultListDataListener
import java.lang.reflect.Method
import javax.swing.JComboBox
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class JComboBoxListProperty<ITEM>(component: JComboBox<ITEM>) :
    BasePropertyAdapter<JComboBox<ITEM>, List<ITEM>, ListDataListener>(component) {

    override val propertyChangeListener: ListDataListener = object : DefaultListDataListener() {
        override fun contentsChanged(e: ListDataEvent) {
            if (e.index0 < 0 && e.index1 < 0) { // selected item change
                return
            }
            notifyValueChange(component.getAllItems())
        }
    }

    private val setItemsMethod: Method? by lazy {
        kotlin.runCatching {
            component.model.javaClass.getDeclaredMethod("setItems", List::class.java)
        }.getOrNull()
    }

    init {
        component.model.addListDataListener(propertyChangeListener)
    }

    private fun JComboBox<ITEM>.getAllItems(): List<ITEM> {
        return mutableListOf<ITEM>().apply {
            for (i in 0 until itemCount) {
                add(getItemAt(i))
            }
        }
    }

    override fun removePropertyChangeListener() {
        component.model.removeListDataListener(propertyChangeListener)
    }

    override fun updateValue(value: List<ITEM>?) {
        value ?: return
        val setItemsMethod = setItemsMethod
        if (setItemsMethod != null) {
            setItemsMethod.invoke(component.model, value)
        } else {
            component.removeAllItems()
            value.forEach(component::addItem)
            component.editor.item = value.firstOrNull()
        }

        propertyChangeObserver?.updateValue(value)
    }

}