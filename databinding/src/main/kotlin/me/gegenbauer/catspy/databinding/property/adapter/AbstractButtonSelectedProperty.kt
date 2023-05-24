package me.gegenbauer.catspy.databinding.property.adapter

import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.AbstractButton

class AbstractButtonSelectedProperty(component: AbstractButton) :
    BasePropertyAdapter<AbstractButton, Boolean, ItemListener>(component) {

    override val propertyChangeListener: ItemListener = ItemListener { e: ItemEvent ->
        notifyValueChange(e.stateChange == ItemEvent.SELECTED)
    }

    init {
        component.addItemListener(propertyChangeListener)
    }

    override fun updateValue(value: Boolean?) {
        value ?: return
        component.isSelected = value
    }

    override fun removePropertyChangeListener() {
        component.removeItemListener(propertyChangeListener)
    }
}