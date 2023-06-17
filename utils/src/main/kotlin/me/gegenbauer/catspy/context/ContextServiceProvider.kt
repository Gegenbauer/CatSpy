package me.gegenbauer.catspy.context

interface ContextServiceProvider {

    fun <T : ContextService> getContextService(contextId: Int, serviceType: Class<out T>): T

    fun <T : ContextService> getContextService(context: Context, serviceType: Class<out T>): T

    fun <T : ContextService> getContextService(serviceType: Class<out T>): T

    fun createServiceProvider(context: Context): ServiceProvider

    fun dispose(context: Context)
}