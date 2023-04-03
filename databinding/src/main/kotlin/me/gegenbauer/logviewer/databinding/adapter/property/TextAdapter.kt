package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.Disposable

interface TextAdapter: ComponentAdapter {
    fun updateText(value: String?)

    fun observeTextChange(observer: (String?) -> Unit)

    @Disposable
    fun removeTextChangeListener()
}