package me.gegenbauer.catspy.databinding.bind

class ObservableValueProperty<T>(value: T? = null): ObservableProperty<T>(value) {

    /**
     * notify component to update with default value of [ObservableValueProperty] when binding first created
     */
    override fun addObserver(observer: Observer<T>) {
        super.addObserver(observer)
        observer.onChange(value)
    }
}