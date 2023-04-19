package me.gegenbauer.logviewer.databinding.property.adapter

import me.gegenbauer.logviewer.databinding.property.support.BasePropertyAdapter
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.AbstractButton

class AbstractButtonSelectedProperty(component: AbstractButton) :
    BasePropertyAdapter<AbstractButton, Boolean, ItemListener>(component) {

    override val propertyChangeListener: ItemListener = ItemListener { e: ItemEvent ->
        propertyChangeObserver?.invoke(e.stateChange == ItemEvent.SELECTED)
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