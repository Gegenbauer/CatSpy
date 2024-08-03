package me.gegenbauer.catspy.context

import java.lang.ref.WeakReference

object GlobalContextManager: ContextManager {
    private val contexts = hashMapOf<Long, WeakReference<Context>>()

    @Synchronized
    override fun getContext(id: Long): Context? {
        return contexts[id]?.get()
    }

    @Synchronized
    override fun register(context: Context) {
        contexts[context.getId()] = WeakReference(context)
    }

    @Synchronized
    override fun unregister(context: Context) {
        contexts.remove(context.getId())
    }
}