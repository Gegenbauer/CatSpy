package me.gegenbauer.catspy.databinding.property.adapter

import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import me.gegenbauer.catspy.utils.ui.DefaultDocumentListener
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

class JTextComponentTextProperty(component: JTextComponent) :
    BasePropertyAdapter<JTextComponent, String, DocumentListener>(component) {

    override val propertyChangeListener: DocumentListener = object : DefaultDocumentListener() {

        override fun contentUpdate(content: String) {
            notifyValueChange(content)
        }
    }

    init {
        component.document.addDocumentListener(propertyChangeListener)
    }

    override fun removePropertyChangeListener() {
        component.document.removeDocumentListener(propertyChangeListener)
    }

    override fun updateValue(value: String?) {
        if (component.text == value) {
            return
        }
        component.text = value
    }

    companion object {
        private const val TAG = "JTextComponentTextProperty"
    }
}