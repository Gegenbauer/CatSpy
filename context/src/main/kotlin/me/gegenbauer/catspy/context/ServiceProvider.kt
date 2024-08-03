package me.gegenbauer.catspy.context

interface ServiceProvider : ContextLifecycleAware, MemoryAware {
    fun <T : ContextService> get(serviceClazz: Class<out T>): T

    fun <T : ContextService> register(serviceClazz: Class<out T>, serviceSupplier: InstanceFetcher<out ContextService>)
}