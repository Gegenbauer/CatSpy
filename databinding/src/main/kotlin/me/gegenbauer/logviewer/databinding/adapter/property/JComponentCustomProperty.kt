package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.setField
import javax.swing.JComponent

class JComponentCustomProperty<VALUE>(component: JComponent, private val propertyName: String): BasePropertyAdapter<JComponent, VALUE, Any>(component) {

    override val propertyChangeListener: Any
        get() = Any()

    override fun removePropertyChangeListener() {
        // do nothing
    }

    override fun updateValue(value: VALUE?) {
        component.setField(propertyName, value)
    }
}