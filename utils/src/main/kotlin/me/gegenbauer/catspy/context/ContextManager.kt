package me.gegenbauer.catspy.context

interface ContextManager {

    fun getContext(id: Int): Context?

    fun register(context: Context)

    fun unregister(context: Context)
}