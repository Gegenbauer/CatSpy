package me.gegenbauer.catspy.context

/**
 * A context is a collection of services that can be used by the application.
 */
interface Context {
    val scope: ContextScope

    companion object {
        val globalScope = object : Context {
            override val scope: ContextScope
                get() = ContextScope.PROCESS
        }
    }
}

enum class ContextScope {
    PROCESS,
    THREAD,
    FRAME,
    WINDOW
}