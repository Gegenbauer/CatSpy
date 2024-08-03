package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.utils.ui.Key
import me.gegenbauer.catspy.utils.ui.keyEventInfo
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
        private val KEY_SCROLL_TO_LAST_POSITION = Key.C_END
        private val KEY_SCROLL_TO_FIRST_POSITION = Key.C_HOME
        private val KEY_SCROLL_TO_FIRST_ROW = Key.C_PAGE_DOWN
        private val KEY_SCROLL_TO_LAST_ROW = Key.C_PAGE_UP
        private val disabledKeys = listOf(
            KEY_SCROLL_TO_LAST_POSITION,
            KEY_SCROLL_TO_FIRST_POSITION,
            KEY_SCROLL_TO_FIRST_ROW,
            KEY_SCROLL_TO_LAST_ROW,
        )
    }
}