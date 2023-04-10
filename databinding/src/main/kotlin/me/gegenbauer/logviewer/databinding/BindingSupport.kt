package me.gegenbauer.logviewer.databinding

import javax.swing.JComponent

private const val KEY_COMPONENT_NAME = "ComponentName"

/**
 * put component name in client property, to be able to identify the component in the binding
 * and can also be used in log
 */

var JComponent.componentName: String
    get() {
        return getClientProperty(KEY_COMPONENT_NAME) as? String ?: ""
    }
    set(value) {
        putClientProperty(KEY_COMPONENT_NAME, value)
    }

inline infix fun <reified T: JComponent> T.withName(name: String): T {
    componentName = name
    return this
}