package me.gegenbauer.catspy.context

import java.util.function.Supplier

class LazyInitInstance<T>(supplier: Supplier<T>) {
    private val instance by lazy { supplier.get() }

    fun get(): T {
        return instance
    }
}