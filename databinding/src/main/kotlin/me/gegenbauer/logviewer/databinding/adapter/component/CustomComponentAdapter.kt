package me.gegenbauer.logviewer.databinding.adapter.component

import me.gegenbauer.logviewer.databinding.adapter.property.CustomPropertyAdapter
import me.gegenbauer.logviewer.databinding.setField
import javax.swing.JComponent

/**
 * 目前只支持单向绑定
 */
class CustomComponentAdapter<T>(private val component: JComponent, private val fieldName: String): CustomPropertyAdapter<T> {
    override fun updateValue(value: T?) {
        component.setField(fieldName, value)
    }

}