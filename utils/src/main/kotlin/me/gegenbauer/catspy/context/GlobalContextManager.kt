package me.gegenbauer.catspy.context

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

object GlobalContextManager: ContextManager {
    private val contexts = ConcurrentHashMap<Long, WeakReference<Context>>()

    override fun getContext(id: Long): Context? {
        return contexts[id]?.get()
    }

    override fun register(context: Context) {
        contexts[context.getId()] = WeakReference(context)
    }

    override fun unregister(context: Context) {
        contexts.remove(context.getId())
    }
}