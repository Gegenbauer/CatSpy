package me.gegenbauer.catspy.databinding.property.adapter

import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import java.awt.event.ItemListener
import javax.swing.JComboBox

class JComboBoxSelectedItemProperty<ITEM>(component: JComboBox<ITEM>) :
    BasePropertyAdapter<JComboBox<ITEM>, ITEM, ItemListener>(component) {

    override val propertyChangeListener: ItemListener = ItemListener {
        notifyValueChange(component.selectedItem as? ITEM)
    }

    init {
        component.addItemListener(propertyChangeListener)
    }

    override fun removePropertyChangeListener() {
        component.removeItemListener(propertyChangeListener)
    }

    override fun updateValue(value: ITEM?) {
        component.model.selectedItem = value
    }
}