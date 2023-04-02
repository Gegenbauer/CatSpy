package me.gegenbauer.logviewer.databinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.log.GLog
import java.awt.Component
import javax.swing.JComponent

private const val TAG = "Bindings"

interface Bindable {
    fun <T> bind(
        componentProperty: ObservableComponentProperty<T>,
        viewModelProperty: ObservableViewModelProperty<T>,
    )
}

private const val KEY_BINDING_CACHE = "binding_cache"

internal fun JComponent.getOrCreateBindingCache(): MutableMap<ObservableComponentProperty<*>, BindingItem> {
    var bindingCache = getClientProperty(KEY_BINDING_CACHE) as? MutableMap<ObservableComponentProperty<*>, BindingItem>
    if (bindingCache == null) {
        bindingCache = mutableMapOf()
        putClientProperty(KEY_BINDING_CACHE, bindingCache)
    }
    return bindingCache
}

/**
 * 一个 Component 的一个属性与一个 ViewModel 的一个属性构建一种类型的绑定关系
 */
enum class BindType : Bindable {
    ONE_WAY_TO_SOURCE {
        override fun <T> bind(
            componentProperty: ObservableComponentProperty<T>,
            viewModelProperty: ObservableViewModelProperty<T>
        ) {
            val bindingCache = componentProperty.component.getOrCreateBindingCache()
            if (bindingCache.containsKey(componentProperty)) {
                GLog.w(TAG, "[bind ONE_WAY_TO_SOURCE] binding ${bindingCache[componentProperty]?.javaClass?.simpleName} already exists")
                return
            }
            GLog.d(TAG, "[bind ONE_WAY_TO_SOURCE] binding ${componentProperty.getDisplayName()} to ${viewModelProperty.getDisplayName()}")
            val observer = ComponentPropertyObserver<T> { newValue ->
                GLog.d(TAG, "[ComponentPropertyObserver] ${componentProperty.getDisplayName()} property change to $newValue")
                viewModelProperty.updateValue(newValue)
            }
            componentProperty.addObserver(observer)
            bindingCache[componentProperty] = BindingItem(this, componentProperty, viewModelProperty, observer)
        }
    },
    ONE_WAY_TO_TARGET {
        override fun <T> bind(
            componentProperty: ObservableComponentProperty<T>,
            viewModelProperty: ObservableViewModelProperty<T>
        ) {
            GLog.d(TAG, "[bind ONE_WAY_TO_TARGET] binding prepare to bind ${componentProperty.getDisplayName()} to ${viewModelProperty.getDisplayName()}")
            val bindingCache = componentProperty.component.getOrCreateBindingCache()
            if (bindingCache.containsKey(componentProperty)) {
                GLog.w(TAG, "[bind ONE_WAY_TO_TARGET] binding ${bindingCache[componentProperty]?.javaClass?.simpleName} already exists")
                return
            }
            val observer = ViewModelPropertyObserver<T> { newValue ->
                GLog.d(TAG, "[ViewModelPropertyObserver] ${viewModelProperty.getDisplayName()} property change to $newValue")
                componentProperty.updateValue(newValue)
            }
            viewModelProperty.addObserver(observer)
            bindingCache[componentProperty] = BindingItem(this, componentProperty, viewModelProperty, null, observer)
        }
    },

    TWO_WAY {
        override fun <T> bind(
            componentProperty: ObservableComponentProperty<T>,
            viewModelProperty: ObservableViewModelProperty<T>
        ) {
            GLog.d(TAG, "[bind TWO_WAY] binding prepare to bind ${componentProperty.getDisplayName()} to ${viewModelProperty.getDisplayName()}")
            val bindingCache = componentProperty.component.getOrCreateBindingCache()
            if (bindingCache.containsKey(componentProperty)) {
                GLog.w(TAG, "[bind TWO_WAY] binding ${bindingCache[componentProperty]?.javaClass?.simpleName} already exists")
                return
            }
            val viewModelPropertyObserver = ViewModelPropertyObserver<T> { newValue ->
                GLog.d(TAG, "[ViewModelPropertyObserver] ${viewModelProperty.getDisplayName()} property change to $newValue")
                componentProperty.updateValue(newValue)
            }
            viewModelProperty.addObserver(viewModelPropertyObserver)
            val componentPropertyObserver = ComponentPropertyObserver<T> { newValue ->
                GLog.d(TAG, "[ComponentPropertyObserver] ${componentProperty.getDisplayName()} property change to $newValue")
                viewModelProperty.updateValue(newValue)
            }
            componentProperty.addObserver(componentPropertyObserver)
            bindingCache[componentProperty] = BindingItem(this, componentProperty, viewModelProperty, componentPropertyObserver, viewModelPropertyObserver)
        }
    }
}

data class BindingItem(
    val bindType: BindType,
    val componentProperty: ObservableComponentProperty<*>,
    val viewModelProperty: ObservableViewModelProperty<*>,
    var componentPropertyChangeListener: ComponentPropertyObserver<*>? = null,
    var viewModelPropertyChangeListener: ViewModelPropertyObserver<*>? = null
) {
    override fun hashCode(): Int {
        return componentProperty.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other !is BindingItem) {
            return false
        }
        return componentProperty == other.componentProperty
    }
}

/**
 * 绑定属性，将 [Component] 的属性绑定到 [ObservableProperty] 类型的对象上，
 * 当 [ObservableProperty] 的值发生变化时，[Component] 的属性也会发生变化
 * 而且绑定是双向的，当 [Component] 的属性发生变化时，[ObservableProperty] 的值也会发生变化
 */
@Suppress("UNCHECKED_CAST")
object Bindings {
    private val scope = AppScope

    fun <T> bind(
        componentProperty: ObservableComponentProperty<T>,
        observable: ObservableViewModelProperty<T>,
        bindType: BindType = BindType.TWO_WAY
    ) {
        scope.launch(Dispatchers.UI) {
            bindType.bind(componentProperty, observable)
        }
    }

    /**
     * componentPropertyDelegate 持有 Component 的引用
     *
     * 所以在不需要绑定时，需要手动解绑，可以通过 [ComponentPropertyDelegateCache]
     * 获取到 componentPropertyDelegate，并移除对 Component 的引用
     */
    fun unBind(componentProperty: ObservableComponentProperty<*>) {
        scope.launch(Dispatchers.UI) {
            unBindInternal(componentProperty)
        }
    }

    private fun unBindInternal(componentProperty: ObservableComponentProperty<*>) {
        val component = componentProperty.component
        val bindingCache = component.getOrCreateBindingCache()
        if (!bindingCache.containsKey(componentProperty)) {
            GLog.w(TAG, "[unBind] binding ${componentProperty.getDisplayName()} not exists")
            return
        }
        GLog.d(TAG, "[unBind] unBinding ${componentProperty.getDisplayName()}")
        val bindingItem = bindingCache[componentProperty]!!
        (bindingItem.viewModelProperty as ObservableViewModelProperty<Any>)
            .removeObserver(bindingItem.viewModelPropertyChangeListener as? ViewModelPropertyObserver<Any>)
        (bindingItem.componentProperty as ObservableComponentProperty<Any>)
            .removeObserver(bindingItem.componentPropertyChangeListener as? ComponentPropertyObserver<Any>)
    }
}