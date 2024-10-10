package me.gegenbauer.catspy.context

/**
 * Marks a class as a singleton that is scoped to a context. And it has the following requirements:
 * 1. ContextService must have a no-arg constructor.
 * 2. ContextService instances are created lazily.
 * 3. ContextService is associated with a context. Get same instance for the same context.
 */
interface ContextService: MemoryAware {

    fun onContextDestroyed(context: Context) {}
}