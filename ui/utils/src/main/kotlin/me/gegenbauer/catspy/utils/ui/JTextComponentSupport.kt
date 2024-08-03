package me.gegenbauer.catspy.utils.ui

import me.gegenbauer.catspy.java.ext.getFieldDeeply
import me.gegenbauer.catspy.java.ext.getMethodDeeply
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.EventListenerList
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter
import javax.swing.text.JTextComponent
import javax.swing.text.PlainDocument

private const val TAG = "JTextComponentSupport"

var JTextComponent.documentListeners: EventListenerList?
    get() {
        val listenerListField = document.getFieldDeeply("listenerList")
        return listenerListField[document] as EventListenerList
    }
    set(value) {
        val listenerListField = document.getFieldDeeply("listenerList")
        listenerListField[document] = value
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

class ValidCharFilter(private val validCharRegex: Regex): DocumentFilter() {
    override fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: AttributeSet?) {
        if (string == null) return
        val sb = StringBuilder()
        for (i in string.indices) {
            val c = string[i]
            if (c.isValidChar()) {
                sb.append(c)
            }
        }
        super.insertString(fb, offset, sb.toString(), attr)
    }

    override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
        if (text == null) return
        val sb = StringBuilder()
        for (i in text.indices) {
            val c = text[i]
            if (c.isValidChar()) {
                sb.append(c)
            }
        }
        super.replace(fb, offset, length, sb.toString(), attrs)
    }

    private fun Char.isValidChar(): Boolean {
        return toString().matches(validCharRegex)
    }
}