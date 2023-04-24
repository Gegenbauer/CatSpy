package me.gegenbauer.logviewer.databinding.property.adapter

import me.gegenbauer.logviewer.databinding.property.support.BasePropertyAdapter
import me.gegenbauer.logviewer.databinding.property.support.setField
import javax.swing.JComponent

class JComponentCustomControllerProperty<VALUE>(component: JComponent, val propertyName: String): BasePropertyAdapter<JComponent, VALUE, Any>(component) {

    override val propertyChangeListener: Any = Any()

    override fun removePropertyChangeListener() {
        // do nothing
    }

    override fun updateValue(value: VALUE?) {
        component.setField(propertyName, value)
    }
}