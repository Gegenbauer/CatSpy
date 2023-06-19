package me.gegenbauer.catspy.context

import java.util.Objects

/**
 * A context is a collection of services that can be used by the application.
 */
interface Context {
    val contexts: Contexts

    val scope: ContextScope

    fun getId(): Int {
        return scope.getId(this)
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
        val globalScope = object : Context {
            override val contexts: Contexts = Contexts.default
            override val scope: ContextScope
                get() = ContextScope.PROCESS
        }
    }
}

enum class ContextScope : ContextIdentifier {
    PROCESS {
        override fun getId(context: Context): Int {
            return ProcessHandle.current().pid().hashCode()
        }
    },
    THREAD {
        override fun getId(context: Context): Int {
            return Thread.currentThread().id.hashCode()
        }
    },
    FRAME {
        override fun getId(context: Context): Int {
            return context.hashCode()
        }
    },
    WINDOW {
        override fun getId(context: Context): Int {
            return context.hashCode()
        }
    },
    COMPONENT {
        override fun getId(context: Context): Int {
            return context.hashCode()
        }
    }
}

interface ContextIdentifier {
    /**
     * Returns an identifier for the given context. Context in same scope should have the same identifier.
     */
    fun getId(context: Context): Int {
        return Objects.hash(context.scope, context)
    }
}