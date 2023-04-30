package me.gegenbauer.catspy.utils

import java.awt.Color
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

fun Color?.toArgb(): Int {
    this ?: return 0
    return (this.alpha shl 24) or (this.red shl 16) or (this.green shl 8) or this.blue
}

fun Color.toHtml(): String {
    return "#%02x%02x%02x".format(red, green, blue)
}

fun Int.toArgb(): Color {
    val alpha = this shr 24 and 0xFF
    val red = this shr 16 and 0xFF
    val green = this shr 8 and 0xFF
    val blue = this and 0xFF
    return Color(red, green, blue, alpha)
}