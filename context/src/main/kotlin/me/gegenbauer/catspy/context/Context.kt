package me.gegenbauer.catspy.context

/**
 * Context is a container for a set of contexts.
 * Like a tree, a context can have a parent context and multiple child contexts.
 * 1. Parent contexts are responsible for the lifecycle of their children.
 * Disposing a parent context will also dispose all its children.
 * 2. Successor contexts can access their ancestor Context.
 */
interface Context: Disposable {
    /**
     * Associated contexts with this context. Generally, it contains ancestor contexts of this context.
     * It can also contain other contexts that are not ancestor contexts of this context,
     * use [putContext] to put a context into this context.
     */
    val contexts: Contexts

    fun getId(): Long {
        return hashCode().toLong()
    }

    /**
     * Sets the parent context of this context.
     * It will put all ancestor contexts of the parent context into this context as well as the parent context itself.
     */
    fun setParent(context: Context) {
        this.contexts.set(context.contexts)
        configureContext(this)
    }

    /**
     * Puts a context into this context.
     */
    fun putContext(context: Context) {
        contexts.putContext(context)
    }

    fun configureContext(context: Context) {
        contexts.putContext(context)
    }

    /**
     * Destroys this context and all its children.
     * It will also dispose all services that are scoped to this context.
     */
    override fun destroy() {
        ServiceManager.dispose(this)
    }

    companion object {
        val process = object : Context {
            override val contexts: Contexts = Contexts.default
            override fun getId(): Long {
                return ProcessHandle.current().pid()
            }
        }

        val thread = object : Context {
            override val contexts: Contexts = Contexts.default
            override fun getId(): Long {
                return Thread.currentThread().id
            }
        }
    }
}