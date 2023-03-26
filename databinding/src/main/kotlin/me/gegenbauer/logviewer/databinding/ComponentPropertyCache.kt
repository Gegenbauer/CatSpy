package me.gegenbauer.logviewer.databinding

import java.awt.Component

internal object ComponentPropertyDelegateCache {
    private val componentPropertyDelegates = mutableMapOf<Int, ObservableProperty<*>>()
    private val componentDelegates = mutableMapOf<Int, Int>()

    private fun hasDelegate(component: Component, property: ComponentProperty): Boolean {
        return componentPropertyDelegates.containsKey(component.hashCode() + property.hashCode())
    }

    fun addDelegate(component: Component, delegate: ObservableProperty<*>) {
        val key = component.hashCode() + delegate.hashCode()
        componentPropertyDelegates[component.hashCode() + delegate.hashCode()] = delegate
        componentDelegates[component.hashCode()] = key
    }

    fun getDelegate(component: Component, property: ComponentProperty): ObservableProperty<*>? {
        return componentPropertyDelegates[component.hashCode() + property.hashCode()]
    }

    fun removeDelegate(component: Component, delegate: ObservableProperty<*>) {
        componentPropertyDelegates.remove(component.hashCode() + delegate.hashCode())
    }

    fun removeAllDelegates(component: Component) {
        componentDelegates[component.hashCode()]?.let { key ->
            componentPropertyDelegates.remove(key)
        }
        componentDelegates.remove(component.hashCode())
    }
}