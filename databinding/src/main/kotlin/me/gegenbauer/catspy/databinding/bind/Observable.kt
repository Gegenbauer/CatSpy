package me.gegenbauer.catspy.databinding.bind

import me.gegenbauer.catspy.databinding.BindingLog
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private const val TAG = "Observable"

interface Observable<T> {

    fun addObserver(observer: Observer<T>)

    fun removeObserver(observer: Observer<T>?)

    fun getAllObservers(): List<Observer<T>>

}

fun interface Observer<T> {
    fun onChange(newValue: T?)
}

class ComponentPropertyObserver<T>(
    val viewModelProperty: ObservableViewModelProperty<T>,
    val componentProperty: ObservableComponentProperty<T>
) : Observer<T> {
    override fun onChange(newValue: T?) {
        BindingLog.d(TAG, "[ComponentPropertyObserver] ${componentProperty.getDisplayName()} property change to $newValue")
        viewModelProperty.updateValue(newValue)
    }
}

class ViewModelPropertyObserver<T>(
    val componentProperty: ObservableComponentProperty<T>,
    val viewModelProperty: ObservableViewModelProperty<T>
) : Observer<T> {
    override fun onChange(newValue: T?) {
        BindingLog.d(TAG, "[ViewModelPropertyObserver] ${viewModelProperty.getDisplayName()} property change to $newValue")
        componentProperty.updateValue(newValue)
    }
}

open class ObservableProperty<T>(value: T? = null) : Observable<T>, ValueUpdater<T> {
    var value: T? = value
        protected set

    private val lock = ReentrantReadWriteLock()
    private val obs = mutableListOf<Observer<T>>()

    override fun updateValue(newValue: T?) {
        if (this.value == newValue) return
        forceUpdateValue(newValue)
    }

    override fun forceUpdateValue(newValue: T?) {
        if (!filterStrategy(newValue)) {
            BindingLog.d(ObservableComponentProperty.TAG, "[forceUpdateValue] ignored by filterStrategy")
            return
        }
        updateValueInternal(newValue)
    }

    fun getValueNonNull(): T {
        return value ?: throw IllegalStateException("value assert nonnull is null")
    }

    protected open fun updateValueInternal(newValue: T?) {
        this.value = newValue
        notifyValueChange(newValue)
    }

    protected open fun notifyValueChange(newValue: T?) {
        lock.read { obs.toList().forEach { it.onChange(newValue) } }
    }

    protected open fun filterStrategy(newValue: T?): Boolean {
        return true
    }

    // 获取泛型类型的类名称
    open fun getValueType(): String {
        value ?: return "TYPE_NULL"
        return value!!::class.java.simpleName
    }

    open fun getDisplayName(): String {
        return "${hashCode()}_${getValueType()}"
    }

    override fun addObserver(observer: Observer<T>) {
        lock.write { obs.add(observer) }
    }

    override fun removeObserver(observer: Observer<T>?) {
        if (observer == null) {
            return
        }
        lock.write {
            obs.remove(observer)
        }
    }

    override fun getAllObservers(): List<Observer<T>> {
        return lock.read { obs.toList() }
    }

}