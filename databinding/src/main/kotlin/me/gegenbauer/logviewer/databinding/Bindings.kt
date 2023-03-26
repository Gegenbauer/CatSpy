package me.gegenbauer.logviewer.databinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.log.GLog
import javax.swing.JComponent

// 绑定属性，将 Component 的属性绑定到 ObservableProperty 类型的对象上，当 ObservableProperty 的值发生变化时，Component 的属性也会发生变化
// 而且绑定是双向的，当 Component 的属性发生变化时，ObservableProperty 的值也会发生变化
@Suppress("UNCHECKED_CAST")
object Bindings {
    private const val TAG = "Bindings"
    private val scope = AppScope

    fun <T> bind(component: JComponent, property: ComponentProperty, observable: ObservableProperty<T>) {
        scope.launch(Dispatchers.UI) {
            bindInternal(component, property, observable)
        }
    }

    private fun <T> bindInternal(
        component: JComponent,
        property: ComponentProperty,
        observable: ObservableProperty<T>
    ) {
        val bindingKey = BindingKey(component, observable)
        if (BindingCache.hasBinding(component, observable)) {
            GLog.e(TAG, "[bind] binding ${bindingKey.hashCode()} already exists")
            return
        }
        GLog.d(TAG, "[bind] ${bindingKey.hashCode()}")
        val componentPropertyChangeListener = PropertyChangeListener<T> { source, newValue, _ ->
            GLog.d(TAG, "[componentPropertyChange] ${bindingKey.hashCode()} property change to $newValue")
            if (source is BaseProperty)
            observable.setValue(newValue)
        }
        val componentPropertyDelegate = property.getObservableProperty<T>(component)
        property.getObservableProperty<T>(component).addOnPropertyChangedCallback(componentPropertyChangeListener)
        val observablePropertyChangeCallback = PropertyChangeListener<T> { source, newValue, _ ->
            GLog.d(TAG, "[observablePropertyChange] ${bindingKey.hashCode()} property change to $newValue")
            componentPropertyDelegate.setValue(newValue, true)
        }
        observable.addOnPropertyChangedCallback(observablePropertyChangeCallback)
        BindingCache.addBinding(
            component,
            observable,
            componentPropertyChangeListener,
            observablePropertyChangeCallback
        )
        ComponentPropertyDelegateCache.addDelegate(component, componentPropertyDelegate)
    }

    fun <T> unBind(component: JComponent, property: ComponentProperty, observableProperty: ObservableProperty<T>) {
        scope.launch(Dispatchers.UI) {
            unBindInternal(component, property, observableProperty)
        }
    }

    private fun <T> unBindInternal(
        component: JComponent,
        property: ComponentProperty,
        observableProperty: ObservableProperty<T>
    ) {
        BindingCache.hasBinding(component, observableProperty).let {
            if (!it) {
                GLog.e(TAG, "[unBind] binding not exists")
                return
            }
        }
        val componentPropertyDelegate =
            ComponentPropertyDelegateCache.getDelegate(component, property) as ObservableProperty<T>?
        val bindingListeners = BindingCache.getBindingListener(component, observableProperty)
        bindingListeners?.let {
            observableProperty.removeOnPropertyChangedCallback(it.second as PropertyChangeListener<T>?)
            componentPropertyDelegate?.removeOnPropertyChangedCallback(it.first as PropertyChangeListener<T>?)
        }
    }
}