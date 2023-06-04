package me.gegenbauer.catspy.context

import me.gegenbauer.catspy.cache.PatternProvider
import java.util.concurrent.ConcurrentHashMap

class ServiceProviderImpl(override val scope: ContextScope = ContextScope.PROCESS) : ServiceProvider, ContextService {
    private val services = ConcurrentHashMap<Class<out ContextService>, LazyInitInstance<ContextService>>()

    init {
        register(PatternProvider::class.java, LazyInitInstance { PatternProvider() })
    }

    override fun <T : ContextService> get(serviceType: Class<T>): T {
        return services[serviceType]?.get() as? T
            ?: throw IllegalStateException("No service of type $serviceType registered")
    }

    override fun <T : ContextService> register(
        serviceType: Class<T>,
        serviceSupplier: LazyInitInstance<out ContextService>
    ) {
        services[serviceType] = serviceSupplier as LazyInitInstance<ContextService>
    }

    override fun onContextCreated(context: Context) {

    }

    override fun onContextDestroyed(context: Context) {
        services.clear()
    }

}