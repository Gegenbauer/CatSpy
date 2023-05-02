package me.gegenbauer.catspy.databinding.property.adapter

import kotlinx.coroutines.Dispatchers
import me.gegenbauer.catspy.concurrency.RateLimiterCallbackSchedule
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.databinding.property.support.BasePropertyAdapter
import me.gegenbauer.catspy.databinding.property.support.withPropertyChangeListenerDisabled
import java.beans.PropertyChangeListener
import javax.swing.JSplitPane

class JSplitPaneDividerProperty(component: JSplitPane) :
    BasePropertyAdapter<JSplitPane, Int, PropertyChangeListener>(component) {
    private val rateLimiterCallbackSchedule = RateLimiterCallbackSchedule(Dispatchers.UI, 100)
    override val propertyChangeListener: PropertyChangeListener = PropertyChangeListener {
        rateLimiterCallbackSchedule.schedule {
            propertyChangeObserver?.invoke(it.newValue as? Int)
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
        component.withPropertyChangeListenerDisabled(JSplitPane.DIVIDER_LOCATION_PROPERTY) {
            component.dividerLocation = value
            component.lastDividerLocation = value
        }
    }
}