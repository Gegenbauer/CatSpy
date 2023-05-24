package me.gegenbauer.catspy.databinding.bind

fun interface ValueUpdater<T> {
    fun updateValue(newValue: T?)
}