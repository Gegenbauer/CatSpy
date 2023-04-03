package me.gegenbauer.logviewer.databinding.adapter.component

import me.gegenbauer.logviewer.databinding.adapter.property.EnabledAdapter
import java.beans.PropertyChangeListener
import javax.swing.JComponent

open class JComponentAdapter(private val component: JComponent) : EnabledAdapter, DisposableAdapter {
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

    companion object {
        private const val PROPERTY_ENABLED = "enabled"
    }
}