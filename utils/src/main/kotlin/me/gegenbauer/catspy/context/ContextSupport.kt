package me.gegenbauer.catspy.context

import javax.swing.JComponent

infix fun <T : JComponent> T.withFrameContext(context: Context): T {
    putClientProperty(ContextScope.FRAME, context.getId())
    if (this is ContextConfigurable) {
        this.configureContext(context.getId())
    }
    return this
}

inline val JComponent.parentFrame: Context?
    get() {
        val contextId = getClientProperty(ContextScope.FRAME) as Int
        return GlobalContextManager.getContext(contextId)
    }