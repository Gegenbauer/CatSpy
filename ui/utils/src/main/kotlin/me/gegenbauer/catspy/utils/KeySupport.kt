package me.gegenbauer.catspy.utils

import me.gegenbauer.catspy.glog.GLog
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

fun installKeyStrokeEscClosing(container: RootPaneContainer) {
    if (container !is Window) {
        GLog.w(TAG, "container is not java.awt.Window")
        return
    }

    val escStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
    val actionMapKey = container.javaClass.name + ":WINDOW_CLOSING"
    val closingAction: Action = object : AbstractAction() {
        override fun actionPerformed(event: ActionEvent) {
            container.dispatchEvent(WindowEvent(container, WindowEvent.WINDOW_CLOSING))
        }
    }

    val rootPane = container.rootPane
    rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escStroke, actionMapKey)
    rootPane.actionMap.put(actionMapKey, closingAction)
}

object Key {
    val C_B = getKeyEventInfo(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK)
    val C_C = getKeyEventInfo(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK)

    val DELETE = getKeyEventInfo(KeyEvent.VK_DELETE)
    val C_DELETE = getKeyEventInfo(KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK)

    val UP = getKeyEventInfo(KeyEvent.VK_UP)

    val DOWN = getKeyEventInfo(KeyEvent.VK_DOWN)

    val PAGE_UP = getKeyEventInfo(KeyEvent.VK_PAGE_UP)
    val C_PAGE_UP = getKeyEventInfo(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_DOWN_MASK)

    val PAGE_DOWN = getKeyEventInfo(KeyEvent.VK_PAGE_DOWN)
    val C_PAGE_DOWN = getKeyEventInfo(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_DOWN_MASK)

    val ENTER = getKeyEventInfo(KeyEvent.VK_ENTER)
    val C_ENTER = getKeyEventInfo(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK)
    val S_ENTER = getKeyEventInfo(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK)

    val TAB = getKeyEventInfo(KeyEvent.VK_TAB)

    val C_END = getKeyEventInfo(KeyEvent.VK_END, KeyEvent.CTRL_DOWN_MASK)
    val C_HOME = getKeyEventInfo(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK)

    val C_L = getKeyEventInfo(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK)
    val C_G = getKeyEventInfo(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK)
    val C_K = getKeyEventInfo(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK)
    val C_P = getKeyEventInfo(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK)
    val C_O = getKeyEventInfo(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK)
    val ESCAPE = getKeyEventInfo(KeyEvent.VK_ESCAPE)

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
)

fun KeyEventInfo.pressed(): KeyEventInfo {
    return copy(action = KeyEvent.KEY_PRESSED)
}

fun KeyEventInfo.released(): KeyEventInfo {
    return copy(action = KeyEvent.KEY_RELEASED)
}

val KeyEvent.keyEventInfo: KeyEventInfo
    get() = KeyEventInfo(keyCode, modifiersEx, id)

fun JComponent.registerStroke(key: KeyEventInfo, strokeName: String, action: KeyStrokeAction) {
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
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