package me.gegenbauer.catspy.utils

import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.MouseEvent
import javax.swing.JFrame

inline val MouseEvent.isDoubleClick: Boolean
    get() = clickCount == 2

inline val MouseEvent.isSingleClick: Boolean
    get() = clickCount == 1

inline val MouseEvent.isLeftClick: Boolean
    get() = button == MouseEvent.BUTTON1

val dismissOnClickOutsideWindows = listOf("AutoCompletePopupWindow")
fun registerDismissOnClickOutsideListener(condition: (Window) -> Boolean) {
    Toolkit.getDefaultToolkit().addAWTEventListener({ event ->
        if (event is MouseEvent && event.id == MouseEvent.MOUSE_CLICKED) {
            // traverse all windows and dispose all popups if clicked outside of it
            JFrame.getWindows().filter { condition(it) && !it.contains(event.point) }.forEach {
                it.dispose()
            }
        }
    }, AWTEvent.MOUSE_EVENT_MASK)
}

