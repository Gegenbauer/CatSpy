package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter

interface TextAdapter: ComponentAdapter {
    fun updateText(value: String?)

    fun observeTextChange(observer: (String?) -> Unit)
}