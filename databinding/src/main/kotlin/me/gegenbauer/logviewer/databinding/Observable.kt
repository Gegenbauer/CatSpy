package me.gegenbauer.logviewer.databinding

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

interface Observable<T> {

    fun addOnPropertyChangedCallback(callback: PropertyChangeListener<T>)

    fun removeOnPropertyChangedCallback(callback: PropertyChangeListener<T>?)

}

fun interface PropertyChangeListener<T> {
    fun onPropertyChange(source: ObservableProperty<*>, newValue: T?, oldValue: T?)
}

open class ObservableProperty<T>(private var value: T? = null) : Observable<T>, PropertyChangeListener<T> {
    private val lock = ReentrantReadWriteLock()

    private val callbacks = mutableListOf<PropertyChangeListener<T>>()

    fun setValue(value: T?, notifySelf: Boolean = false) {
        val oldValue = this.value
        if (oldValue == value) return
        this.value = value
        lock.read {
            callbacks.forEach { it.onPropertyChange(this, value, oldValue) }
        }
        if (notifySelf) {
            onPropertyChange(this, value, oldValue)
        }
    }

    fun getValue(): T? = value

    override fun addOnPropertyChangedCallback(callback: PropertyChangeListener<T>) {
        lock.write { callbacks.add(callback) }
    }

    override fun removeOnPropertyChangedCallback(callback: PropertyChangeListener<T>?) {
        lock.write {
            if (callback == null) {
                callbacks.clear()
                return@write
            }
            callbacks.remove(callback)
        }
    }

    override fun onPropertyChange(source: ObservableProperty<*>, newValue: T?, oldValue: T?) {
        // do nothing
    }

}