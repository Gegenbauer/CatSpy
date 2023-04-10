package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.Disposable
import javax.swing.event.DocumentListener

interface TextAdapter: ComponentAdapter {
    val textChangeListener: DocumentListener

    fun updateText(value: String?)

    fun observeTextChange(observer: (String?) -> Unit)

    @Disposable
    fun removeTextChangeListener()
}