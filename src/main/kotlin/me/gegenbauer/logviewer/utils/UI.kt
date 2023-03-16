package me.gegenbauer.logviewer.utils

import java.awt.Component
import javax.swing.JFrame

fun findFrameFromParent(component: Component): JFrame {
    var current = component.parent
    while (current != null) {
        if (current is JFrame) {
            return current
        }
        current = current.parent
    }
    throw IllegalStateException("No JFrame found in parent hierarchy")
}