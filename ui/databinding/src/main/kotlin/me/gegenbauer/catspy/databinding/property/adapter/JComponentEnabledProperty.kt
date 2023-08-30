package me.gegenbauer.catspy.databinding.property.adapter

import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import me.gegenbauer.catspy.databinding.property.support.PROPERTY_ENABLED
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class JComponentEnabledProperty(component: JComponent) : BasePropertyAdapter<JComponent, Boolean, PropertyChangeListener>(component) {

    override val propertyChangeListener: PropertyChangeListener = PropertyChangeListener { evt ->
        notifyValueChange(evt.newValue as? Boolean)
    }

    init {
        component.addPropertyChangeListener(PROPERTY_ENABLED, propertyChangeListener)
    }

    override fun removePropertyChangeListener() {
        component.removePropertyChangeListener(PROPERTY_ENABLED, propertyChangeListener)
    }

    override fun updateValue(value: Boolean?) {
        value ?: return
        component.isEnabled = value
    }

}