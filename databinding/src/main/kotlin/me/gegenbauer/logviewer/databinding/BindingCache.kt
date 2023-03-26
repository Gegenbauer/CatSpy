package me.gegenbauer.logviewer.databinding

import me.gegenbauer.logviewer.concurrency.assertInMainThread
import java.awt.Component

internal object BindingCache {
    private val bindings = mutableMapOf<BindingKey, Pair<PropertyChangeListener<*>?, PropertyChangeListener<*>?>>()
    private val componentBindings = mutableMapOf<Component, MutableList<BindingKey>>()
    private val observableBindings = mutableMapOf<ObservableProperty<*>, MutableList<BindingKey>>()

    fun hasBinding(component: Component, observableProperty: ObservableProperty<*>): Boolean {
        assertInMainThread()
        return bindings.containsKey(BindingKey(component, observableProperty))
    }

    fun addBinding(
        component: Component,
        observableProperty: ObservableProperty<*>,
        componentPropertyChangeListener: PropertyChangeListener<*>,
        observablePropertyChangeListener: PropertyChangeListener<*>,
    ) {
        assertInMainThread()
        val bindingKey = BindingKey(component, observableProperty)
        bindings[bindingKey] = Pair(componentPropertyChangeListener, observablePropertyChangeListener)
        componentBindings.getOrPut(component) { mutableListOf() }.add(bindingKey)
        observableBindings.getOrPut(observableProperty) { mutableListOf() }.add(bindingKey)
    }

    fun getBindingListener(component: Component, observableProperty: ObservableProperty<*>): Pair<PropertyChangeListener<*>?, PropertyChangeListener<*>?>? {
        assertInMainThread()
        val bindingKey = BindingKey(component, observableProperty)
        return bindings[bindingKey]
    }

    fun removeBinding(component: Component, observableProperty: ObservableProperty<*>) {
        assertInMainThread()
        val bindingKey = BindingKey(component, observableProperty)
        bindings.remove(bindingKey)
        componentBindings[component]?.remove(bindingKey)
        observableBindings[observableProperty]?.remove(bindingKey)
    }

    fun removeAllBindings(component: Component) {
        assertInMainThread()
        componentBindings[component]?.forEach { bindingKey ->
            bindings.remove(bindingKey)
            observableBindings[bindingKey.observable]?.remove(bindingKey)
        }
        componentBindings.remove(component)
    }

    fun removeAllBindings(observable: ObservableProperty<*>) {
        assertInMainThread()
        observableBindings[observable]?.forEach { bindingKey ->
            bindings.remove(bindingKey)
            componentBindings[bindingKey.component]?.remove(bindingKey)
        }
        observableBindings.remove(observable)
    }
}

data class BindingKey(
    val component: Component,
    val observable: ObservableProperty<*>,
) {
    override fun hashCode(): Int {
        return component.hashCode() + observable.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is BindingKey) {
            return component == other.component && observable == other.observable
        }
        return false
    }
}