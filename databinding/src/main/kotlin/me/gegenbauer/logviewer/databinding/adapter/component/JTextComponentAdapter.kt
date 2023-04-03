package me.gegenbauer.logviewer.databinding.adapter.component

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.concurrency.ViewModelScope
import me.gegenbauer.logviewer.databinding.adapter.property.EnabledAdapter
import me.gegenbauer.logviewer.databinding.adapter.property.TextAdapter
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent


class JTextComponentAdapter(component: JComponent) :
    TextAdapter, EnabledAdapter by JComponentAdapter(component), DisposableAdapter {
    private val textComponent = component as JTextComponent

    // 给 text field 增加一个延迟，避免频繁地更新，保证最后一次更新能成功即可
    private val documentListener = object : DocumentListener {
        private val scope = ViewModelScope()
        private var removeJob: Job? = null

        override fun insertUpdate(e: DocumentEvent?) {
            removeJob?.cancel()
            scope.launch(Dispatchers.UI) {
                delay(10)
                textChangeObserver?.invoke(textComponent.text)
            }
        }

        override fun removeUpdate(e: DocumentEvent?) {
            removeJob = scope.launch(Dispatchers.UI) {
                delay(10)
                textChangeObserver?.invoke(textComponent.text)
            }
        }

        override fun changedUpdate(e: DocumentEvent?) {
            //
        }
    }

    private var textChangeObserver: ((String) -> Unit)? = null

    init {
        textComponent.document.addDocumentListener(documentListener)
    }

    override fun updateText(value: String?) {
        textComponent.updateText(value)
    }

    override fun observeTextChange(observer: (String?) -> Unit) {
        textChangeObserver = observer
    }

    override fun removeTextChangeListener() {
        textComponent.document.removeDocumentListener(documentListener)
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