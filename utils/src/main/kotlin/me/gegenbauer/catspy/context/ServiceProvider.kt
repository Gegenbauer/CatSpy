package me.gegenbauer.catspy.context

interface ServiceProvider {
    fun <T: ContextService> get(serviceType: Class<T>): T

    fun <T: ContextService> register(serviceType: Class<T>, serviceSupplier: LazyInitInstance<out ContextService>)
}