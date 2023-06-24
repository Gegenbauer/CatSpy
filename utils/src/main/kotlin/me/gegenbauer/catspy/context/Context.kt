package me.gegenbauer.catspy.context

/**
 * A context is a collection of services that can be used by the application.
 */
interface Context {
    val contexts: Contexts

    fun getId(): Long {
        return hashCode().toLong()
    }

    fun setContexts(contexts: Contexts): Context {
        this.contexts.set(contexts)
        configureContext(this)
        return this
    }

    fun configureContext(context: Context) {
        contexts.putContext(context)
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