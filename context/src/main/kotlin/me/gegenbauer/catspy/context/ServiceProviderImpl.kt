package me.gegenbauer.catspy.context

/**
 * ServiceProvider itself is also a type of ContextService.
 * Different Contexts have different ServiceProviders, which can be obtained through the Context id.
 */
class ServiceProviderImpl : ServiceProvider, ContextService {
    private val services = hashMapOf<Class<out ContextService>, InstanceFetcher<ContextService>>()

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    override fun <T : ContextService> get(serviceClazz: Class<out T>): T {
        return services.getOrPut(serviceClazz) { serviceClazz.defaultFetcher }.get() as T
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    override fun <T : ContextService> register(
        serviceClazz: Class<out T>,
        serviceSupplier: InstanceFetcher<out ContextService>
    ) {
        services[serviceClazz] = serviceSupplier as InstanceFetcher<ContextService>
    }

    @Synchronized
    override fun onTrimMemory(level: MemoryAware.Level) {
        services.values.forEach { it.get().onTrimMemory(level) }
    }

    @Synchronized
    override fun onContextDestroyed(context: Context) {
        services.values.forEach { it.get().onContextDestroyed(context) }
        services.clear()
    }

}