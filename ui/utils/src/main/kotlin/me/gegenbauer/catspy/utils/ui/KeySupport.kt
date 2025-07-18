package me.gegenbauer.catspy.utils.ui

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.currentPlatform
import java.awt.Component
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import javax.swing.*

private const val TAG = "KeySupport"

/**
 * Intercepts a key event for a specific component and calls the callback if the key event matches.
 * @param source The component to intercept the key event for.
 * @param key The key event to be intercepted. see [Key]
 */
class KeyEventInterceptor(
    private val source: Component,
    private val key: KeyEventInfo
) {
    private val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    private var callback: ((KeyEvent) -> Unit)? = null
    private var dispatcher: KeyEventDispatcher? = null

    fun enable(callback: (KeyEvent) -> Unit) {
        this.callback = callback

        dispatcher = KeyEventDispatcher { keyEvent ->
            if (keyEvent.source == source && keyEvent.keyEventInfo == key) {
                callback(keyEvent)
                keyEvent.consume()
                return@KeyEventDispatcher true
            }
            false
        }

        manager.addKeyEventDispatcher(dispatcher)
    }

    fun disable() {
        manager.removeKeyEventDispatcher(dispatcher)
        dispatcher = null
        callback = null
    }
}

fun installKeyStroke(container: RootPaneContainer, stroke: KeyStroke?, actionMapKey: String?, action: Action?) {
    val rootPane = container.rootPane
    rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, actionMapKey)
    rootPane.actionMap.put(actionMapKey, action)
}

fun installKeyStrokeEscClosing(container: RootPaneContainer, action: () -> Unit) {
    if (container !is Window) {
        GLog.w(TAG, "container is not java.awt.Window")
        return
    }

    val escStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    val actionMapKey = container.javaClass.name + ":WINDOW_CLOSING"
    val closingAction: Action = object : AbstractAction() {
        override fun actionPerformed(event: ActionEvent) {
            action()
        }
    }

    val rootPane = container.rootPane
    rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escStroke, actionMapKey)
    rootPane.actionMap.put(actionMapKey, closingAction)
}

fun installKeyStrokeEscClosing(container: RootPaneContainer) {
    if (container !is Window) {
        GLog.w(TAG, "container is not java.awt.Window")
        return
    }
    installKeyStrokeEscClosing(container) {
        container.dispatchEvent(WindowEvent(container, WindowEvent.WINDOW_CLOSING))
    }
}

object Key {
    private val vkMask: Int = currentPlatform.vkMask

    val C_B = getKeyEventInfo(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK)
    val C_C = getKeyEventInfo(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK)

    val DELETE = getKeyEventInfo(KeyEvent.VK_DELETE)
    val C_DELETE = getKeyEventInfo(KeyEvent.VK_DELETE, currentPlatform.vkMask)

    val UP = getKeyEventInfo(KeyEvent.VK_UP)

    val DOWN = getKeyEventInfo(KeyEvent.VK_DOWN)

    val PAGE_UP = getKeyEventInfo(KeyEvent.VK_PAGE_UP)
    val C_PAGE_UP = getKeyEventInfo(KeyEvent.VK_PAGE_UP, vkMask)

    val PAGE_DOWN = getKeyEventInfo(KeyEvent.VK_PAGE_DOWN)
    val C_PAGE_DOWN = getKeyEventInfo(KeyEvent.VK_PAGE_DOWN, vkMask)

    val ENTER = getKeyEventInfo(KeyEvent.VK_ENTER)
    val C_ENTER = getKeyEventInfo(KeyEvent.VK_ENTER, vkMask)
    val S_ENTER = getKeyEventInfo(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK)

    val TAB = getKeyEventInfo(KeyEvent.VK_TAB)

    val C_END = getKeyEventInfo(KeyEvent.VK_END, vkMask)
    val C_HOME = getKeyEventInfo(KeyEvent.VK_HOME, vkMask)

    val C_A = getKeyEventInfo(KeyEvent.VK_A, vkMask)
    val C_F = getKeyEventInfo(KeyEvent.VK_F, vkMask)
    val C_L = getKeyEventInfo(KeyEvent.VK_L, vkMask)
    val C_G = getKeyEventInfo(KeyEvent.VK_G, vkMask)
    val C_K = getKeyEventInfo(KeyEvent.VK_K, vkMask)
    val C_P = getKeyEventInfo(KeyEvent.VK_P, vkMask)
    val C_O = getKeyEventInfo(KeyEvent.VK_O, vkMask)
    val C_S = getKeyEventInfo(KeyEvent.VK_S, vkMask)
    val C_Y = getKeyEventInfo(KeyEvent.VK_Y, vkMask)
    val C_Z = getKeyEventInfo(KeyEvent.VK_Z, vkMask)

    val C_M = getKeyEventInfo(KeyEvent.VK_M, vkMask)
    val ESCAPE = getKeyEventInfo(KeyEvent.VK_ESCAPE)
    val F3 = getKeyEventInfo(KeyEvent.VK_F3)
    val F4 = getKeyEventInfo(KeyEvent.VK_F4)

    fun getKeyEventInfo(
        keyCode: Int,
        modifiers: Int = 0,
        action: Int = KeyEvent.KEY_PRESSED
    ): KeyEventInfo {
        return KeyEventInfo(keyCode, modifiers, action)
    }
}

data class KeyEventInfo(
    val keyCode: Int,
    val modifiers: Int = 0,
    var action: Int = KeyEvent.KEY_PRESSED,
) {
    override fun toString(): String {
        return "${KeyEvent.getModifiersExText(modifiers)} ${KeyEvent.getKeyText(keyCode)}"
    }
}

fun KeyEventInfo.pressed(): KeyEventInfo {
    return copy(action = KeyEvent.KEY_PRESSED)
}

fun KeyEventInfo.released(): KeyEventInfo {
    return copy(action = KeyEvent.KEY_RELEASED)
}

val KeyEvent.keyEventInfo: KeyEventInfo
    get() = KeyEventInfo(keyCode, modifiersEx, id)

fun JComponent.registerStroke(key: KeyEventInfo, strokeName: String, action: KeyStrokeAction) {
    registerStroke(key, strokeName, JComponent.WHEN_IN_FOCUSED_WINDOW, action)
}

fun JComponent.registerStrokeWhenFocused(key: KeyEventInfo, strokeName: String, action: KeyStrokeAction) {
    registerStroke(key, strokeName, JComponent.WHEN_FOCUSED, action)
}

val customKeyStrokes = mutableListOf<Pair<String, KeyEventInfo>>()

fun JComponent.registerStroke(key: KeyEventInfo, strokeName: String, focusedCondition: Int, action: KeyStrokeAction) {
    customKeyStrokes.add(strokeName to key)
    getInputMap(focusedCondition).put(
        KeyStroke.getKeyStroke(key.keyCode, key.modifiers),
        strokeName
    )
    actionMap.put(strokeName, object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            action.actionPerformed(e)
        }
    })
}

fun interface KeyStrokeAction {
    fun actionPerformed(e: ActionEvent)
}