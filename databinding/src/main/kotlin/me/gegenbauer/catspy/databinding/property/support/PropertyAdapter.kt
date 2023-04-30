package me.gegenbauer.catspy.databinding.property.support

interface PropertyAdapter<VALUE, LISTENER> {
    val propertyChangeListener: LISTENER

    fun updateValue(value: VALUE?)

    fun observeValueChange(observer: (VALUE?) -> Unit)

    @Disposable
    fun removePropertyChangeListener()
}