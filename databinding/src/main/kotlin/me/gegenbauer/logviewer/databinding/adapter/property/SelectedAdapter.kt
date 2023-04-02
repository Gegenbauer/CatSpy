package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter

interface SelectedAdapter: ComponentAdapter {
    fun updateSelectedStatus(value: Boolean?)

    fun observeSelectedStatus(observer: (Boolean?) -> Unit)
}