package me.gegenbauer.catspy.context

import me.gegenbauer.catspy.cache.PatternProvider
import java.util.concurrent.ConcurrentHashMap

class ServiceProviderImpl(override val scope: ContextScope = ContextScope.PROCESS) : ServiceProvider, ContextService {
    private val services = ConcurrentHashMap<Class<out ContextService>, InstanceFetcher<ContextService>>()

    init {
        register(PatternProvider::class.java, LazyInitInstanceFetcher { PatternProvider() })
    }

    override fun <T : ContextService> get(serviceType: Class<T>): T {
        return services.getOrPut(serviceType) {
            InstanceFetcher { serviceType.getDeclaredConstructor().newInstance() }
        }.get() as T
    }

    override fun <T : ContextService> register(
        serviceType: Class<T>,
        serviceSupplier: InstanceFetcher<out ContextService>
    ) {
        services[serviceType] = serviceSupplier as InstanceFetcher<ContextService>
    }

    override fun onContextDestroyed(context: Context) {
        services.values.forEach { it.get().onContextDestroyed(context) }
        services.clear()
    }

}