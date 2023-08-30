package me.gegenbauer.catspy.context

import java.util.function.Supplier

class LazyInitInstanceFetcher<T>(supplier: Supplier<T>) : InstanceFetcher<T> {
    private val instance by lazy { supplier.get() }

    override fun get(): T {
        return instance
    }
}

inline val <T: ContextService> Class<out T>.defaultFetcher: InstanceFetcher<T>
    get() = LazyInitInstanceFetcher { this.getDeclaredConstructor().newInstance() }

class InstantInitInstanceFetcher<T>(private val instance: T) : InstanceFetcher<T> {
    override fun get(): T {
        return instance
    }
}

fun interface InstanceFetcher<T> {
    fun get(): T
}