package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.Disposable
import java.beans.PropertyChangeListener

interface EnabledAdapter: ComponentAdapter {
    val enabledStatusChangeListener: PropertyChangeListener

    fun updateEnabledStatus(value: Boolean?)

    fun observeEnabledStatusChange(observer: (Boolean?) -> Unit)

    @Disposable
    fun removeEnabledChangeListener()
}