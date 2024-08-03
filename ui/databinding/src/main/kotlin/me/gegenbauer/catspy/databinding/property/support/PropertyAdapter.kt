package me.gegenbauer.catspy.databinding.property.support

import me.gegenbauer.catspy.databinding.bind.ValueUpdater

interface PropertyAdapter<VALUE, LISTENER> : DisposableAdapter {
    val propertyChangeListener: LISTENER

    fun updateValue(value: VALUE?)

    fun observeValueChange(observer: ValueUpdater<VALUE>)

    @Disposable
    fun removePropertyChangeListener()
}