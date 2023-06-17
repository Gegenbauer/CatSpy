package me.gegenbauer.catspy.context

fun interface ContextLifecycleAware {

    fun onContextDestroyed(context: Context)
}