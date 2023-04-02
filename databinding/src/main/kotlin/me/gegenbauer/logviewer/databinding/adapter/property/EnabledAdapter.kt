package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter

interface EnabledAdapter: ComponentAdapter {
    fun updateEnabledStatus(value: Boolean?)

    fun observeEnabledStatusChange(observer: (Boolean?) -> Unit)
}