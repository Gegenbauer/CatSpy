package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.Disposable
import java.awt.event.HierarchyListener

interface VisibilityAdapter : ComponentAdapter {
    val visibilityChangeListener: HierarchyListener

    fun updateVisibility(visible: Boolean)

    fun observeVisibilityChange(observer: (Boolean) -> Unit)

    @Disposable
    fun removeVisibilityChangeListener()
}