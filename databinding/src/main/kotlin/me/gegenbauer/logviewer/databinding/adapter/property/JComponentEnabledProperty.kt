package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.PROPERTY_ENABLED
import me.gegenbauer.logviewer.databinding.adapter.property.BasePropertyAdapter
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class JComponentEnabledProperty(component: JComponent) : BasePropertyAdapter<JComponent, Boolean, PropertyChangeListener>(component) {

    init {
        component.addPropertyChangeListener(PROPERTY_ENABLED, propertyChangeListener)
    }

    override val propertyChangeListener: PropertyChangeListener
        get() = PropertyChangeListener { evt ->
            propertyChangeObserver?.invoke(evt.newValue as Boolean)
        }

    override fun removePropertyChangeListener() {
        component.removePropertyChangeListener(PROPERTY_ENABLED, propertyChangeListener)
    }

    override fun updateValue(value: Boolean?) {
        value ?: return
        component.isEnabled = value
    }

}