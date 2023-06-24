package me.gegenbauer.catspy.context

import java.util.concurrent.ConcurrentHashMap

/**
 * Marks a class as a singleton that is scoped to a context.
 */
class ContextServiceProviderImpl : ContextServiceProvider {
    private val providers = ConcurrentHashMap<Long, ServiceProvider>()

    override fun <T : ContextService> getContextService(contextId: Long, serviceClazz: Class<out T>): T? {
        val context = GlobalContextManager.getContext(contextId)
        context ?: return null
        return getContextService(context, serviceClazz)
    }

    override fun <T : ContextService> getContextService(context: Context, serviceClazz: Class<out T>): T {
        val serviceProvider = providers.getOrPut(context.getId()) { createServiceProvider(context) }
        return serviceProvider.get(serviceClazz)
    }

    override fun <T : ContextService> getContextService(serviceClazz: Class<out T>): T {
        return getContextService(Context.process, serviceClazz)
    }

    override fun <T : ContextService> registerContextService(serviceClazz: Class<out T>) {
        val serviceProvider = providers.getOrPut(Context.process.getId()) { createServiceProvider(Context.process) }
        serviceProvider.register(serviceClazz, serviceClazz.defaultFetcher)
    }

    override fun createServiceProvider(context: Context): ServiceProvider {
        return ServiceProviderImpl()
    }

    override fun dispose(context: Context) {
        providers.remove(context.getId())?.apply {
            onContextDestroyed(context)
        }
    }
}