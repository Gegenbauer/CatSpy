package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.property.BasePropertyAdapter
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.AbstractButton

class AbstractButtonSelectedProperty(component: AbstractButton) :
    BasePropertyAdapter<AbstractButton, Boolean, ItemListener>(component) {

    init {
        component.addItemListener(propertyChangeListener)
    }

    override val propertyChangeListener: ItemListener
        get() = ItemListener { e: ItemEvent ->
            propertyChangeObserver?.invoke(e.stateChange == ItemEvent.SELECTED)
        }

    override fun updateValue(value: Boolean?) {
        value ?: return
        component.isSelected = value
    }

    override fun removePropertyChangeListener() {
        component.removeItemListener(propertyChangeListener)
    }
}