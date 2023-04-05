package me.gegenbauer.logviewer.databinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapterFactory
import me.gegenbauer.logviewer.databinding.adapter.component.DisposableAdapter
import me.gegenbauer.logviewer.log.GLog
import java.awt.event.HierarchyEvent
import javax.swing.JComponent

abstract class ObservableComponentProperty<T>(
    val component: JComponent,
) : ObservableProperty<T>() {
    private var propertyChangeObserver: ((T?) -> Unit)? = null
    val componentAdapter: ComponentAdapter? = ComponentAdapterFactory.getComponentAdapter(component)

    init {
        component.addHierarchyListener { e ->
            if (((e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()) != 0L) && !component.isDisplayable) {
                GLog.d(TAG, "[HierarchyListener] component " + "${component.javaClass.simpleName}_${component.hashCode()} isDisplayable = false")
                // component is not displayable, cancel all coroutines
                Bindings.unBind(this)
                (componentAdapter as? DisposableAdapter)?.dispose()
            }
        }
    }

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
        // do nothing
    }

    companion object {
        const val TAG = "ComponentProperty"
    }
}