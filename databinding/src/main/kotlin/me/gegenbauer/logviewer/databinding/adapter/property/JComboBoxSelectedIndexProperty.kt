package me.gegenbauer.logviewer.databinding.adapter.property

import java.awt.event.ItemListener
import javax.swing.JComboBox

class JComboBoxSelectedIndexProperty<ITEM>(component: JComboBox<ITEM>) :
    BasePropertyAdapter<JComboBox<ITEM>, Int, ItemListener>(component) {

    init {
        component.addItemListener(propertyChangeListener)
    }

    override val propertyChangeListener: ItemListener
        get() = ItemListener {
            propertyChangeObserver?.invoke(component.selectedIndex)
        }

    override fun removePropertyChangeListener() {
        component.removeItemListener(propertyChangeListener)
    }

    override fun updateValue(value: Int?) {
        value ?: return
        if (value >= 0) {
            component.selectedIndex = value
        }
    }
}