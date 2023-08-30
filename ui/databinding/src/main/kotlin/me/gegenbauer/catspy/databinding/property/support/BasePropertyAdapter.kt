package me.gegenbauer.catspy.databinding.property.support

import me.gegenbauer.catspy.databinding.bind.*

abstract class BasePropertyAdapter<COMPONENT, VALUE, LISTENER>(protected val component: COMPONENT) :
    PropertyAdapter<VALUE, LISTENER> {

    protected var propertyChangeObserver: ValueUpdater<VALUE>? = null

    override fun observeValueChange(observer: ValueUpdater<VALUE>) {
        propertyChangeObserver = observer
    }

    protected fun notifyValueChange(value: VALUE?) {
        propertyChangeObserver?.updateValue(value)
    }
}