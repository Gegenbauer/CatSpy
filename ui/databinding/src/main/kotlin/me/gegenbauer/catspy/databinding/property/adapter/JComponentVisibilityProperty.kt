package me.gegenbauer.catspy.databinding.property.adapter

import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.JComponent

class JComponentVisibilityProperty(component: JComponent): BasePropertyAdapter<JComponent, Boolean, HierarchyListener>(component) {

    override val propertyChangeListener: HierarchyListener = HierarchyListener {
        if (it.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
            notifyValueChange(component.isShowing)
        }
    }

    init {
        component.addHierarchyListener(propertyChangeListener)
    }

    override fun removePropertyChangeListener() {
        component.removeHierarchyListener(propertyChangeListener)
    }

    override fun updateValue(value: Boolean?) {
        value ?: return
        component.isVisible = value
    }
}