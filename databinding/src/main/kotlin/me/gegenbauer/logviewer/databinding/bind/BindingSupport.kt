package me.gegenbauer.logviewer.databinding.bind

import javax.swing.JComponent

private const val KEY_COMPONENT_NAME = "ComponentName"

/**
 * put component name in client property, to be able to identify the component in the binding
 * and can also be used in log
 */

var JComponent.componentName: String
    get() {
        return getClientProperty(KEY_COMPONENT_NAME) as? String ?: ""
    }
    set(value) {
        putClientProperty(KEY_COMPONENT_NAME, value)
    }

inline infix fun <reified T: JComponent> T.withName(name: String): T {
    componentName = name
    return this
}

infix fun <T> ObservableComponentProperty<T>.bindDual(viewModelProperty: ObservableViewModelProperty<T>) {
    Bindings.bind(this, viewModelProperty)
}

infix fun <T> ObservableComponentProperty<T>.bindRight(viewModelProperty: ObservableViewModelProperty<T>) {
    Bindings.bind(this, viewModelProperty, BindType.ONE_WAY_TO_TARGET)
}

infix fun <T> ObservableComponentProperty<T>.bindLeft(viewModelProperty: ObservableViewModelProperty<T>) {
    Bindings.bind(this, viewModelProperty, BindType.ONE_WAY_TO_SOURCE)
}

fun <T> List<T>.updateListByLRU(lastUsedItem: T): List<T> {
    return if (lastUsedItem in this) {
        val list = this.toMutableList()
        list.remove(lastUsedItem)
        list.add(0, lastUsedItem)
        list
    } else {
        this
    }
}