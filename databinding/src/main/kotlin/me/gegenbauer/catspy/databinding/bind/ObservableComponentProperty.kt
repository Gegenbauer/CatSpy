package me.gegenbauer.catspy.databinding.bind

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.databinding.property.support.DisposableAdapter
import me.gegenbauer.catspy.databinding.property.support.PropertyAdapter
import me.gegenbauer.catspy.log.GLog
import java.awt.event.HierarchyEvent
import javax.swing.JComponent

abstract class ObservableComponentProperty<T>(
    val component: JComponent,
) : ObservableProperty<T>() {
    private var propertyChangeObserver: ((T?) -> Unit)? = null
    private val propertyAdapter: PropertyAdapter<T, *> = getPropertyAdapterImpl().apply {
        observeValueChange(this@ObservableComponentProperty::updateValue)
    }

    init {
        component.addHierarchyListener { e ->
            if (((e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()) != 0L) && !component.isDisplayable) {
                GLog.d(TAG, "[HierarchyListener] component " + "${component.javaClass.simpleName}_${component.hashCode()} isDisplayable = false")
                // component is not displayable, cancel all coroutines
                Bindings.unBind(this)
                (propertyAdapter as? DisposableAdapter)?.dispose()
            }
        }
    }

    abstract fun getPropertyAdapterImpl(): PropertyAdapter<T, *>

    abstract fun createProperty(component: JComponent): ObservableComponentProperty<T>

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_${getValueType()}"
    }

    final override fun updateValue(newValue: T?) {
        if (value != newValue && newValue != null) {
            AppScope.launch(Dispatchers.UI) {
                setProperty(newValue)
            }
        }
        super.updateValue(newValue)
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