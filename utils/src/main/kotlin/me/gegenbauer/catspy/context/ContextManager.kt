package me.gegenbauer.catspy.context

interface ContextManager {

    fun getContext(id: Long): Context?

    fun register(context: Context)

    fun unregister(context: Context)
}