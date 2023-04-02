package me.gegenbauer.logviewer.databinding.adapter.component

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.concurrency.ViewModelScope
import me.gegenbauer.logviewer.databinding.adapter.Disposable
import me.gegenbauer.logviewer.databinding.adapter.property.EnabledAdapter
import me.gegenbauer.logviewer.databinding.adapter.property.TextAdapter
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent


class JTextFieldAdapter(component: JComponent) : TextAdapter, EnabledAdapter, Disposable {
    private val textField = component as JTextField

    // 给 text field 增加一个延迟，避免频繁地更新，保证最后一次更新能成功即可
    private val documentListener = object : DocumentListener {
        private val scope = ViewModelScope()
        private var removeJob: Job? = null

        override fun insertUpdate(e: DocumentEvent?) {
            removeJob?.cancel()
            scope.launch(Dispatchers.UI) {
                delay(10)
                textChangeObserver?.invoke(textField.text)
            }
        }

        override fun removeUpdate(e: DocumentEvent?) {
            removeJob = scope.launch(Dispatchers.UI) {
                delay(10)
                textChangeObserver?.invoke(textField.text)
            }
        }

        override fun changedUpdate(e: DocumentEvent?) {
            //
        }
    }

    private val enableStateChangeListener = PropertyChangeListener{ evt ->
        enabledStateChangeObserver?.invoke(evt.newValue as Boolean)
    }

    private var enabledStateChangeObserver: ((Boolean) -> Unit)? = null
    private var textChangeObserver: ((String) -> Unit)? = null

    init {
        textField.document.addDocumentListener(documentListener)
        textField.addPropertyChangeListener(PROPERTY_ENABLED, enableStateChangeListener)
    }

    override fun dispose() {
        textField.document.removeDocumentListener(documentListener)
    }

    override fun updateText(value: String?) {
        textField.updateText(value)
    }

    override fun observeTextChange(observer: (String?) -> Unit) {
        textChangeObserver = observer
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

    override fun updateEnabledStatus(value: Boolean?) {
        value ?: return
        textField.isEnabled = value
    }

    override fun observeEnabledStatusChange(observer: (Boolean?) -> Unit) {
        enabledStateChangeObserver = observer
    }

    companion object {
        private const val PROPERTY_ENABLED = "enabled"
    }
}