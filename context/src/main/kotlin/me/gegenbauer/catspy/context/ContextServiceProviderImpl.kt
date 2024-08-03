package me.gegenbauer.catspy.context

/**
 * Marks a class as a singleton that is scoped to a context.
 */
class ContextServiceProviderImpl : ContextServiceProvider {
    private val providers = hashMapOf<Long, ServiceProvider>()

    @Synchronized
    override fun <T : ContextService> getContextService(contextId: Long, serviceClazz: Class<out T>): T? {
        val context = GlobalContextManager.getContext(contextId)
        context ?: return null
        return getContextService(context, serviceClazz)
    }

    @Synchronized
    override fun <T : ContextService> getContextService(context: Context, serviceClazz: Class<out T>): T {
        val serviceProvider = providers.getOrPut(context.getId()) { createServiceProvider(context) }
        return serviceProvider.get(serviceClazz)
    }

    @Synchronized
    override fun <T : ContextService> getContextService(serviceClazz: Class<out T>): T {
        return getContextService(Context.process, serviceClazz)
    }

    @Synchronized
    override fun <T : ContextService> registerGlobalService(serviceClazz: Class<out T>) {
        val serviceProvider = providers.getOrPut(Context.process.getId()) { createServiceProvider(Context.process) }
        serviceProvider.register(serviceClazz, serviceClazz.defaultFetcher)
    }

    @Synchronized
    override fun createServiceProvider(context: Context): ServiceProvider {
        return ServiceProviderImpl()
    }

    @Synchronized
    override fun onTrimMemory(level: MemoryAware.Level) {
        providers.values.forEach { it.onTrimMemory(level) }
    }

    @Synchronized
    override fun dispose(context: Context) {
        providers.remove(context.getId())?.apply {
            onContextDestroyed(context)
        }
    }
}