package me.gegenbauer.catspy.viewmodel

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.context.Contexts
import javax.swing.JComponent

abstract class ViewModel(override val contexts: Contexts) : Context, ContextService {
    abstract val componentClazz: Class<JComponent>
}

