package me.gegenbauer.catspy.databinding.bind

interface ValueUpdater<T> {
    fun updateValue(newValue: T?)

    fun forceUpdateValue(newValue: T?)
}