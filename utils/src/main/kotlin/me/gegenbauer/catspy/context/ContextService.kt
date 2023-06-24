package me.gegenbauer.catspy.context

/**
 * Marks a class as a singleton that is scoped to a context.
 */
interface ContextService {

    fun onContextDestroyed(context: Context) {}
}