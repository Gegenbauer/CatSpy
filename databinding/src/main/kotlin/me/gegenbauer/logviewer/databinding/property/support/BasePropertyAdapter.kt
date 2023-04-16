package me.gegenbauer.logviewer.databinding.property.support

abstract class BasePropertyAdapter<COMPONENT, VALUE, LISTENER>(protected val component: COMPONENT) : PropertyAdapter<VALUE, LISTENER> {

    protected var propertyChangeObserver: ((VALUE?) -> Unit)? = null

    override fun observeValueChange(observer: (VALUE?) -> Unit) {
        propertyChangeObserver = observer
    }
}