package me.gegenbauer.logviewer.databinding.adapter.component

import me.gegenbauer.logviewer.databinding.adapter.property.EnabledAdapter
import me.gegenbauer.logviewer.databinding.adapter.property.VisibilityAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.beans.PropertyChangeListener
import javax.swing.JComponent

open class JComponentAdapter(private val component: JComponent) : EnabledAdapter, VisibilityAdapter ,DisposableAdapter {
    final override val enabledStatusChangeListener: PropertyChangeListener
        get() = PropertyChangeListener { evt ->
            enabledStateChangeObserver?.invoke(evt.newValue as Boolean)
        }

    init {
        component.addPropertyChangeListener(PROPERTY_ENABLED, enabledStatusChangeListener)
    }

    private var enabledStateChangeObserver: ((Boolean) -> Unit)? = null

    override fun updateEnabledStatus(value: Boolean?) {
        value ?: return
        component.isEnabled = value
    }

    override fun observeEnabledStatusChange(observer: (Boolean?) -> Unit) {
        enabledStateChangeObserver = observer
    }

    override fun removeEnabledChangeListener() {
        component.removePropertyChangeListener(PROPERTY_ENABLED, enabledStatusChangeListener)
    }

    override val visibilityChangeListener: HierarchyListener
        get() = HierarchyListener {
            if (it.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                visibilityChangeObserver?.invoke(component.isShowing)
            }
        }
    private var visibilityChangeObserver: ((Boolean) -> Unit)? = null

    override fun updateVisibility(visible: Boolean) {
        component.isVisible = visible
    }

    override fun observeVisibilityChange(observer: (Boolean) -> Unit) {
        visibilityChangeObserver = observer
    }

    override fun removeVisibilityChangeListener() {
        component.removeHierarchyListener(visibilityChangeListener)
    }

    companion object {
        private const val PROPERTY_ENABLED = "enabled"
    }
}