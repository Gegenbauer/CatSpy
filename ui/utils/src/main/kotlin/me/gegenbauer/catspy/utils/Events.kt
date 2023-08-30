package me.gegenbauer.catspy.utils

import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

/**
 * Intercepts a key event for a specific component and calls the callback if the key event matches.
 * @param source The component to intercept the key event for.
 * @param key The key code to intercept, eg: [KeyEvent.VK_ENTER].
 * @param action The action to intercept, eg: [KeyEvent.KEY_PRESSED].
 */
fun interceptEvent(source: Component, key: Int, action: Int, callback: (KeyEvent) -> Unit) {
    val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    manager.addKeyEventDispatcher { keyEvent ->
        if (keyEvent.source == source && keyEvent.id == action && keyEvent.keyCode == key) {
            callback(keyEvent)
            keyEvent.consume()
            return@addKeyEventDispatcher true
        }
        return@addKeyEventDispatcher false
    }
}