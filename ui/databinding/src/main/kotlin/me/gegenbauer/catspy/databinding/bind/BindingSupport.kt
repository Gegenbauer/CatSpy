package me.gegenbauer.catspy.databinding.bind

infix fun <T> ObservableComponentProperty<T>?.bindDual(viewModelProperty: ObservableValueProperty<T>?) {
    if (this == null || viewModelProperty == null) {
        return
    }
    Bindings.bind(this, viewModelProperty)
}

/**
 * Binds the [ObservableComponentProperty] to the [ObservableValueProperty] in a one-way binding from the view to the view model.
 * The view property will be the source of the binding.
 */
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

/**
 * Binds the [ObservableComponentProperty] to the [ObservableValueProperty] in a one-way binding from the view model to the view.
 * The view model property will be the source of the binding.
 */
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