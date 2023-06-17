package me.gegenbauer.catspy.context

interface ServiceProvider : ContextLifecycleAware {
    fun <T : ContextService> get(serviceType: Class<T>): T

    fun <T : ContextService> register(serviceType: Class<T>, serviceSupplier: InstanceFetcher<out ContextService>)
}