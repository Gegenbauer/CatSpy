package me.gegenbauer.catspy.context

interface ContextServiceProvider {

    fun <T : ContextService> getContextService(contextId: Long, serviceClazz: Class<out T>): T?

    /**
     * Returns the service for the target context.
     */
    fun <T : ContextService> getContextService(context: Context, serviceClazz: Class<out T>): T

    /**
     * Returns the service for the current process context.
     */
    fun <T : ContextService> getContextService(serviceClazz: Class<out T>): T

    fun <T : ContextService> registerContextService(serviceClazz: Class<out T>)

    fun createServiceProvider(context: Context): ServiceProvider

    fun dispose(context: Context)
}