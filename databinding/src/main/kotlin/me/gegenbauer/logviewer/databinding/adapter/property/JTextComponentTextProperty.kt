package me.gegenbauer.logviewer.databinding.adapter.property

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.concurrency.ViewModelScope
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

class JTextComponentTextProperty(component: JTextComponent) :
    BasePropertyAdapter<JTextComponent, String, DocumentListener>(component) {

    init {
        component.document.addDocumentListener(propertyChangeListener)
    }

    override val propertyChangeListener: DocumentListener
        get() = object : DocumentListener {
            private val scope = ViewModelScope()
            private var removeJob: Job? = null

            override fun insertUpdate(e: DocumentEvent?) {
                removeJob?.cancel()
                val text = component.text
                scope.launch(Dispatchers.UI) {
                    delay(10)
                    propertyChangeObserver?.invoke(text)
                }
            }

            override fun removeUpdate(e: DocumentEvent?) {
                val text = component.text
                removeJob = scope.launch(Dispatchers.UI) {
                    delay(10)
                    propertyChangeObserver?.invoke(text)
                }
            }

            override fun changedUpdate(e: DocumentEvent?) {
                //
            }
        }

    override fun removePropertyChangeListener() {
        component.document.removeDocumentListener(propertyChangeListener)
    }

    override fun updateValue(value: String?) {
        component.updateText(value)
    }

    private fun JTextComponent.updateText(text: String?) {
        text ?: return
        val oldText = this.text
        if (oldText == text) {
            return
        }
        val oldLength = oldText.length
        val newLength = text.length
        val minLength = oldLength.coerceAtMost(newLength)
        var i = 0
        while (i < minLength) {
            if (oldText[i] != text[i]) {
                break
            }
            i++
        }
        when (i) {
            oldLength -> {
                // oldText is prefix of text
                document.insertString(i, text.substring(i), null)
            }

            newLength -> {
                // text is prefix of oldText
                document.remove(i, oldLength - i)
            }

            else -> {
                // text and oldText have common prefix
                document.remove(i, oldLength - i)
                document.insertString(i, text.substring(i), null)
            }
        }
    }
}