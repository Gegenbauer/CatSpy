package me.gegenbauer.catspy.context

/**
 * Marks a class as a singleton that is scoped to a context.
 */
class ContextServiceProviderImpl : ContextServiceProvider {
    private val providers = mutableMapOf<Int, ServiceProvider>()

    override fun <T : ContextService> getContextService(contextId: Int, serviceType: Class<out T>): T {
        return getContextService(GlobalContextManager.getContext(contextId)!!, serviceType)
    }

    override fun <T : ContextService> getContextService(context: Context, serviceType: Class<out T>): T {
        val serviceProvider = providers.getOrPut(context.getId()) { createServiceProvider(context) }
        return serviceProvider.get(serviceType)
    }

    override fun <T : ContextService> getContextService(serviceType: Class<out T>): T {
        return getContextService(Context.globalScope, serviceType)
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