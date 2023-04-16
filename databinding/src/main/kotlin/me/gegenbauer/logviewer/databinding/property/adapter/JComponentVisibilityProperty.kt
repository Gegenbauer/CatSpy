package me.gegenbauer.logviewer.databinding.property.adapter

import me.gegenbauer.logviewer.databinding.property.support.BasePropertyAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.JComponent

class JComponentVisibilityProperty(component: JComponent): BasePropertyAdapter<JComponent, Boolean, HierarchyListener>(component) {

    init {
        component.addHierarchyListener(propertyChangeListener)
    }

    override val propertyChangeListener: HierarchyListener
        get() = HierarchyListener {
            if (it.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                propertyChangeObserver?.invoke(component.isShowing)
            }
        }

    override fun removePropertyChangeListener() {
        component.removeHierarchyListener(propertyChangeListener)
    }

    override fun updateValue(value: Boolean?) {
        value ?: return
        component.isVisible = value
    }
}