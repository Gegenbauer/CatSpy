package me.gegenbauer.logviewer.databinding.property.adapter

import me.gegenbauer.logviewer.databinding.property.support.BasePropertyAdapter
import me.gegenbauer.logviewer.databinding.property.support.PROPERTY_TEXT
import java.beans.PropertyChangeListener
import javax.swing.JLabel

class JLabelTextProperty(component: JLabel): BasePropertyAdapter<JLabel, String, PropertyChangeListener>(component) {

    init {
        component.addPropertyChangeListener(PROPERTY_TEXT, propertyChangeListener)
    }

    override val propertyChangeListener: PropertyChangeListener
        get() = PropertyChangeListener {
            propertyChangeObserver?.invoke(it.newValue as? String)
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