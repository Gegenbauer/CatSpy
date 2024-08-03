package me.gegenbauer.catspy.log.filter

import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.utils.persistence.Preferences
import me.gegenbauer.catspy.utils.persistence.UserPreferences

class StorableValueProperty<T>(
    override val key: String,
    private val defaultValue: T,
) : ObservableValueProperty<T>(defaultValue), UserPreferences.PreferencesChangeListener, Observer<T> {

    init {
        updateValue(getStoredValue())
        addObserver(this)
        Preferences.addChangeListener(this)
    }

    private fun getStoredValue(): T {
        if (defaultValue is List<*>) {
            return Preferences.getStringList(key) as T
        }
        val storedValue = Preferences.getString(key)
        return stringToValue(storedValue)
    }

    private fun stringToValue(value: String): T {
        return when (defaultValue) {
            is String -> value as T
            is Int -> value.toInt() as T
            is Long -> value.toLong() as T
            is Float -> value.toFloat() as T
            is Boolean -> if (value.isEmpty()) defaultValue else value.toBoolean() as T
            else -> defaultValue
        }
    }

    override fun onPreferencesChanged() {
        val storedValue = getStoredValue()
        if (value != storedValue) {
            updateValue(storedValue)
        }
    }

    override fun onChange(newValue: T?) {
        val storedValue = getStoredValue()
        if (storedValue == newValue) return
        if (defaultValue is List<*>) {
            Preferences.putStringList(key, newValue as? List<String> ?: emptyList())
        } else {
            Preferences.putString(key, newValue?.toString() ?: "")
        }
    }

}