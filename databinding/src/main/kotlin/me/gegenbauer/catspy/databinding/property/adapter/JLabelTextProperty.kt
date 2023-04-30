package me.gegenbauer.catspy.databinding.property.adapter

import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import me.gegenbauer.catspy.databinding.property.support.PROPERTY_TEXT
import java.beans.PropertyChangeListener
import javax.swing.JLabel

class JLabelTextProperty(component: JLabel): BasePropertyAdapter<JLabel, String, PropertyChangeListener>(component) {

    override val propertyChangeListener: PropertyChangeListener = PropertyChangeListener {
        propertyChangeObserver?.invoke(it.newValue as? String)
    }

    init {
        component.addPropertyChangeListener(PROPERTY_TEXT, propertyChangeListener)
    }

    override fun removePropertyChangeListener() {
        component.removePropertyChangeListener(propertyChangeListener)
    }

    override fun updateValue(value: String?) {
        if (component.text == value) {
            return
        }
        component.text = value
    }

}