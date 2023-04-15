package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.Disposable

interface PropertyAdapter<VALUE, LISTENER> {
    val propertyChangeListener: LISTENER

    fun updateValue(value: VALUE?)

    fun observeValueChange(observer: (VALUE?) -> Unit)

    @Disposable
    fun removePropertyChangeListener()
}