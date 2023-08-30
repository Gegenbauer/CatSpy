package me.gegenbauer.catspy.log.ui.panel

import me.gegenbauer.catspy.utils.getKeyEventInfo
import me.gegenbauer.catspy.utils.keyEventInfo
import java.awt.Component
import java.awt.event.KeyEvent
import javax.swing.JScrollPane
import javax.swing.KeyStroke

class LogScrollPane @JvmOverloads constructor(view: Component? = null) : JScrollPane(view) {

    override fun processKeyBinding(ks: KeyStroke, e: KeyEvent, condition: Int, pressed: Boolean): Boolean {
        if (disabledKeys.contains(e.keyEventInfo)) {
            return false
        }
        return super.processKeyBinding(ks, e, condition, pressed)
    }

    companion object {
        private val KEY_SCROLL_TO_LAST_POSITION = getKeyEventInfo(KeyEvent.VK_END, KeyEvent.CTRL_DOWN_MASK)
        private val KEY_SCROLL_TO_FIRST_POSITION = getKeyEventInfo(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK)
        private val KEY_SCROLL_TO_FIRST_ROW = getKeyEventInfo(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_DOWN_MASK)
        private val KEY_SCROLL_TO_LAST_ROW = getKeyEventInfo(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_DOWN_MASK)
        private val disabledKeys = listOf(
            KEY_SCROLL_TO_LAST_POSITION,
            KEY_SCROLL_TO_FIRST_POSITION,
            KEY_SCROLL_TO_FIRST_ROW,
            KEY_SCROLL_TO_LAST_ROW,
        )
    }
}