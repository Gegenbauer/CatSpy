package me.gegenbauer.logviewer.databinding.property.adapter

import me.gegenbauer.logviewer.databinding.property.support.BasePropertyAdapter
import me.gegenbauer.logviewer.databinding.property.support.DefaultDocumentListener
import me.gegenbauer.logviewer.databinding.property.support.withDocumentListenerDisabled
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

class JTextComponentTextProperty(component: JTextComponent) :
    BasePropertyAdapter<JTextComponent, String, DocumentListener>(component) {

    init {
        component.document.addDocumentListener(propertyChangeListener)
    }

    override val propertyChangeListener: DocumentListener
        get() = object : DefaultDocumentListener() {

            override fun contentUpdate(content: String) {
                propertyChangeObserver?.invoke(content)
            }
        }

    override fun removePropertyChangeListener() {
        component.document.removeDocumentListener(propertyChangeListener)
    }

    override fun updateValue(value: String?) {
        if (component.text == value) {
            return
        }
        component.withDocumentListenerDisabled {
            text = value
        }
        component.repaint()
    }
}