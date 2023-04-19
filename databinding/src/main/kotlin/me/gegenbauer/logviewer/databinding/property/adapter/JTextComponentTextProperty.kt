package me.gegenbauer.logviewer.databinding.property.adapter

import me.gegenbauer.logviewer.databinding.property.support.BasePropertyAdapter
import me.gegenbauer.logviewer.databinding.property.support.DefaultDocumentListener
import me.gegenbauer.logviewer.databinding.property.support.withDocumentListenerDisabled
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

class JTextComponentTextProperty(component: JTextComponent) :
    BasePropertyAdapter<JTextComponent, String, DocumentListener>(component) {

    override val propertyChangeListener: DocumentListener = object : DefaultDocumentListener() {

        override fun contentUpdate(content: String) {
            propertyChangeObserver?.invoke(content)
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
        component.withDocumentListenerDisabled(propertyChangeListener) {
            text = value
        }
        component.repaint()
    }

    companion object {
        private const val TAG = "JTextComponentTextProperty"
    }
}