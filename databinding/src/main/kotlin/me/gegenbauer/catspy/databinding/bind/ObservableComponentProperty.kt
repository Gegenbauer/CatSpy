package me.gegenbauer.catspy.databinding.bind

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.isInMainThread
import me.gegenbauer.catspy.databinding.property.support.PropertyAdapter
import javax.swing.JComponent

abstract class ObservableComponentProperty<T>(
    val component: JComponent,
) : ObservableProperty<T>() {
    private val propertyAdapter: PropertyAdapter<T, *> = getPropertyAdapterImpl().apply {
        observeValueChange(this@ObservableComponentProperty)
    }

    abstract fun getPropertyAdapterImpl(): PropertyAdapter<T, *>

    abstract fun createProperty(component: JComponent): ObservableComponentProperty<T>

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_${getValueType()}"
    }

    override fun updateValueInternal(newValue: T?) {
        value = newValue
        if (isInMainThread()) {
            setProperty(value)
            notifyValueChange(value)
        } else {
            AppScope.launch(Dispatchers.Main) {
                setProperty(value)
                notifyValueChange(value)
            }
        }
    }

    /**
     * update property of component when value changed
     */
    protected open fun setProperty(newValue: T?) {
        propertyAdapter.updateValue(newValue)
    }

    companion object {
        const val TAG = "ComponentProperty"
    }
}