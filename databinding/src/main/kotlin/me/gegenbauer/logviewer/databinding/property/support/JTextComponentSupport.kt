package me.gegenbauer.logviewer.databinding.property.support

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.EventListenerList
import javax.swing.text.JTextComponent
import javax.swing.text.PlainDocument

private const val TAG = "JTextComponentSupport"

var JTextComponent.documentListeners: EventListenerList?
    get() {
        val listenerListField = document.getFieldDeeply("listenerList")
        return listenerListField.get(document) as EventListenerList
    }
    set(value) {
        val listenerListField = document.getFieldDeeply("listenerList")
        return listenerListField.set(document, value)
    }

fun JTextComponent.withDocumentListenerDisabled(action: JTextComponent.() -> Unit) {
    val documentListenersCopy = documentListeners
    documentListeners = EventListenerList()
    action.invoke(this)
    documentListeners = documentListenersCopy
}

fun JTextComponent.withDocumentListenerDisabled(listener: DocumentListener, action: JTextComponent.() -> Unit) {
    document.removeDocumentListener(listener)
    action.invoke(this)
    document.addDocumentListener(listener)
}

fun JTextComponent.reportTextChange() {
    val fireInsertUpdateMethod = document.getMethodDeeply("fireInsertUpdate", DocumentEvent::class.java)
    fireInsertUpdateMethod?.invoke(document, DefaultDocument().getDocumentEvent(text.length))
}

open class DefaultDocumentListener : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
        contentUpdate(e.document.getText(0, e.document.length))
    }

    override fun removeUpdate(e: DocumentEvent) {
        contentUpdate(e.document.getText(0, e.document.length))
    }

    override fun changedUpdate(e: DocumentEvent) {
        // empty implementation
    }

    protected open fun contentUpdate(content: String) {
        // empty implementation
    }
}

class DefaultDocument : PlainDocument() {
    fun getDocumentEvent(len: Int): DocumentEvent {
        return DefaultDocumentEvent(0, len, DocumentEvent.EventType.INSERT)
    }
}