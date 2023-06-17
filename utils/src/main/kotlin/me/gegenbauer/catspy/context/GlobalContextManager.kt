package me.gegenbauer.catspy.context

import java.lang.ref.WeakReference

object GlobalContextManager: ContextManager {
    private val contexts = mutableMapOf<Int, WeakReference<Context>>()

    override fun getContext(id: Int): Context? {
        return contexts[id]?.get()
    }

    override fun register(context: Context) {
        contexts[context.getId()] = WeakReference(context)
    }

    override fun unregister(context: Context) {
        contexts.remove(context.getId())
    }
}