package me.gegenbauer.catspy.utils

import java.awt.event.MouseEvent

inline val MouseEvent.isDoubleClick: Boolean
    get() = clickCount == 2

inline val MouseEvent.isSingleClick: Boolean
    get() = clickCount == 1

inline val MouseEvent.isLeftClick: Boolean
    get() = button == MouseEvent.BUTTON1