package me.gegenbauer.catspy.configuration

import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import kotlin.reflect.KMutableProperty

class WriteSyncValueProperty<T>(syncFields: List<KMutableProperty<T>>, initialValue: T) :
    ObservableValueProperty<T>(initialValue) {
    init {
        addObserver { value ->
            SettingsManager.updateSettings {
                syncFields.forEach { field -> field.setter.call(value) }
            }
        }
    }
}