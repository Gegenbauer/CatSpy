package me.gegenbauer.catspy.context

object ServiceManager: ContextServiceProvider by ContextServiceProviderImpl() {
    init {
        MemoryState.register(this)
    }
}