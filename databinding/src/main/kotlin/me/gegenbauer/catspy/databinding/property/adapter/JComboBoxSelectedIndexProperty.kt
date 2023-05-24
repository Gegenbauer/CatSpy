package me.gegenbauer.catspy.databinding.property.adapter

import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import java.awt.event.ItemListener
import javax.swing.JComboBox

class JComboBoxSelectedIndexProperty<ITEM>(component: JComboBox<ITEM>) :
    BasePropertyAdapter<JComboBox<ITEM>, Int, ItemListener>(component) {

    override val propertyChangeListener: ItemListener = ItemListener {
        notifyValueChange(component.selectedIndex)
    }

    init {
        component.addItemListener(propertyChangeListener)
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