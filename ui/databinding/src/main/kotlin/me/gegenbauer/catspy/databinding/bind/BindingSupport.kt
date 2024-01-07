package me.gegenbauer.catspy.databinding.bind

infix fun <T> ObservableComponentProperty<T>?.bindDual(viewModelProperty: ObservableValueProperty<T>?) {
    if (this == null || viewModelProperty == null) {
        return
    }
    Bindings.bind(this, viewModelProperty)
}

infix fun <T> ObservableComponentProperty<T>?.bindRight(viewModelProperty: ObservableValueProperty<T>?) {
    if (this == null || viewModelProperty == null) {
        return
    }
    Bindings.bind(
        this,
        viewModelProperty,
        BindType.ONE_WAY_TO_TARGET
    )
}

infix fun <T> ObservableComponentProperty<T>?.bindLeft(viewModelProperty: ObservableValueProperty<T>?) {
    if (this == null || viewModelProperty == null) {
        return
    }
    Bindings.bind(
        this,
        viewModelProperty,
        BindType.ONE_WAY_TO_SOURCE
    )
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