package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.Disposable
import java.beans.PropertyChangeListener

interface EditableAdapter: ComponentAdapter {
    val editableChangeListener: PropertyChangeListener

    fun updateEditableStatus(value: Boolean?)

    fun observeEditableStatusChange(observer: (Boolean?) -> Unit)

    @Disposable
    fun removeEditableChangeListener()
}