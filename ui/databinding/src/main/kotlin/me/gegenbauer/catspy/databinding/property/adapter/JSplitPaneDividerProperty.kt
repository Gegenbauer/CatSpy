package me.gegenbauer.catspy.databinding.property.adapter

import kotlinx.coroutines.Dispatchers
import me.gegenbauer.catspy.concurrency.IgnoreFastCallbackScheduler
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import java.beans.PropertyChangeListener
import javax.swing.JSplitPane

class JSplitPaneDividerProperty(component: JSplitPane) :
    BasePropertyAdapter<JSplitPane, Int, PropertyChangeListener>(component) {
    private val ignoreFastCallbackScheduler = IgnoreFastCallbackScheduler(Dispatchers.UI.immediate, 100)
    override val propertyChangeListener: PropertyChangeListener = PropertyChangeListener {
        ignoreFastCallbackScheduler.schedule {
            notifyValueChange(it.newValue as? Int)
        }
    }

    init {
        component.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, propertyChangeListener)
    }

    override fun removePropertyChangeListener() {
        component.removePropertyChangeListener(propertyChangeListener)
    }

    override fun updateValue(value: Int?) {
        value ?: return
        component.removePropertyChangeListener(propertyChangeListener)
        component.dividerLocation = value
        component.lastDividerLocation = value
        component.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, propertyChangeListener)
    }
}