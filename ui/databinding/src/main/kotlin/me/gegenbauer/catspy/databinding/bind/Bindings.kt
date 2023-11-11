package me.gegenbauer.catspy.databinding.bind

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.databinding.BindingLog
import java.awt.Component
import javax.swing.JComponent

private const val TAG = "Bindings"

interface Bindable {
    fun <T> bind(
        componentProperty: ObservableComponentProperty<T>,
        viewModelProperty: ObservableValueProperty<T>,
    )
}

private const val KEY_BINDING_CACHE = "binding_cache"

@Suppress("UNCHECKED_CAST")
internal fun JComponent.getOrCreateBindingCache(): MutableMap<ObservableComponentProperty<*>, BindingItem> {
    var bindingCache = getClientProperty(KEY_BINDING_CACHE) as? MutableMap<ObservableComponentProperty<*>, BindingItem>
    if (bindingCache == null) {
        bindingCache = mutableMapOf()
        putClientProperty(KEY_BINDING_CACHE, bindingCache)
    }
    return bindingCache
}

@Suppress("UNCHECKED_CAST")
var JComponent.bindingCache: MutableMap<ObservableComponentProperty<*>, BindingItem>?
    get() = getClientProperty(KEY_BINDING_CACHE) as? MutableMap<ObservableComponentProperty<*>, BindingItem>
    set(value) {
        putClientProperty(KEY_BINDING_CACHE, value)
    }

/**
 * 一个 Component 的一个属性与一个 ViewModel 的一个属性构建一种类型的绑定关系
 * Source means ComponentProperty, target means ViewModelProperty
 */
enum class BindType : Bindable {
    ONE_WAY_TO_SOURCE {
        override fun <T> bind(
            componentProperty: ObservableComponentProperty<T>,
            viewModelProperty: ObservableValueProperty<T>
        ) {
            val bindingCache = componentProperty.component.getOrCreateBindingCache()
            if (bindingCache.containsKey(componentProperty)) {
                BindingLog.w(TAG, "[bind ONE_WAY_TO_SOURCE] binding ${bindingCache[componentProperty]?.javaClass?.simpleName} already exists")
                return
            }
            BindingLog.d(TAG, "[bind ONE_WAY_TO_SOURCE] binding ${componentProperty.getDisplayName()} to ${viewModelProperty.getDisplayName()}")
            val observer = ComponentPropertyObserver(viewModelProperty, componentProperty)
            componentProperty.addObserver(observer)
            bindingCache[componentProperty] = BindingItem(this, componentProperty, viewModelProperty, observer)
        }
    },
    ONE_WAY_TO_TARGET {
        override fun <T> bind(
            componentProperty: ObservableComponentProperty<T>,
            viewModelProperty: ObservableValueProperty<T>
        ) {
            BindingLog.d(TAG, "[bind ONE_WAY_TO_TARGET] binding prepare to bind ${componentProperty.getDisplayName()} to ${viewModelProperty.getDisplayName()}")
            val bindingCache = componentProperty.component.getOrCreateBindingCache()
            if (bindingCache.containsKey(componentProperty)) {
                BindingLog.w(TAG, "[bind ONE_WAY_TO_TARGET] binding ${bindingCache[componentProperty]?.javaClass?.simpleName} already exists")
                return
            }
            val observer = ValuePropertyObserver(componentProperty, viewModelProperty)
            viewModelProperty.addObserver(observer)
            bindingCache[componentProperty] = BindingItem(this, componentProperty, viewModelProperty, null, observer)
        }
    },

    TWO_WAY {
        override fun <T> bind(
            componentProperty: ObservableComponentProperty<T>,
            viewModelProperty: ObservableValueProperty<T>
        ) {
            BindingLog.d(TAG, "[bind TWO_WAY] binding prepare to bind ${componentProperty.getDisplayName()} to ${viewModelProperty.getDisplayName()}")
            val bindingCache = componentProperty.component.getOrCreateBindingCache()
            if (bindingCache.containsKey(componentProperty)) {
                BindingLog.w(TAG, "[bind TWO_WAY] binding ${bindingCache[componentProperty]?.javaClass?.simpleName} already exists")
                return
            }
            val valuePropertyObserver = ValuePropertyObserver(componentProperty, viewModelProperty)
            viewModelProperty.addObserver(valuePropertyObserver)
            val componentPropertyObserver = ComponentPropertyObserver(viewModelProperty, componentProperty)
            componentProperty.addObserver(componentPropertyObserver)
            bindingCache[componentProperty] = BindingItem(this, componentProperty, viewModelProperty, componentPropertyObserver, valuePropertyObserver)
        }
    }
}

data class BindingItem(
    val bindType: BindType,
    val componentProperty: ObservableComponentProperty<*>,
    val viewModelProperty: ObservableValueProperty<*>,
    var componentPropertyChangeListener: ComponentPropertyObserver<*>? = null,
    var viewModelPropertyChangeListener: ValuePropertyObserver<*>? = null,
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
        observable: ObservableValueProperty<T>,
        bindType: BindType = BindType.TWO_WAY
    ) {
        scope.launch(Dispatchers.UI.immediate) {
            bindType.bind(componentProperty, observable)
        }
    }

    fun unBind(componentProperty: ObservableComponentProperty<*>) {
        scope.launch(Dispatchers.UI.immediate) {
            unBindInternal(componentProperty)
        }
    }

    fun rebind(source: JComponent, target: JComponent) {
        scope.launch(Dispatchers.UI.immediate) {
            val bindingCache = source.getOrCreateBindingCache()
            bindingCache.forEach { (componentProperty, bindingItem) ->
                unBindInternal(componentProperty)
                bind(
                    (componentProperty as ObservableComponentProperty<Any>).createProperty(target),
                    bindingItem.viewModelProperty as ObservableValueProperty<Any>, bindingItem.bindType
                )
            }
        }
    }

    private fun unBindInternal(componentProperty: ObservableComponentProperty<*>) {
        val component = componentProperty.component
        val bindingCache = component.getOrCreateBindingCache()
        if (!bindingCache.containsKey(componentProperty)) {
            BindingLog.w(TAG, "[unBind] binding ${componentProperty.getDisplayName()} not exists")
            return
        }
        BindingLog.d(TAG, "[unBind] unBinding ${componentProperty.getDisplayName()}")
        val bindingItem = bindingCache[componentProperty]!!
        (bindingItem.viewModelProperty as ObservableValueProperty<Any>)
            .removeObserver(bindingItem.viewModelPropertyChangeListener as? ValuePropertyObserver<Any>)
        (bindingItem.componentProperty as ObservableComponentProperty<Any>)
            .removeObserver(bindingItem.componentPropertyChangeListener as? ComponentPropertyObserver<Any>)
        bindingCache.remove(componentProperty)
    }
}