package me.gegenbauer.catspy.utils

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

fun <T: JFrame> Component.findFrameFromParent(): T {
    var current = parent
    while (current != null) {
        if (current is JFrame) {
            return current as T
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

fun JPanel.addVSeparator2(height: Int = 20) {
    val separator1 = JSeparator(SwingConstants.VERTICAL)
    separator1.preferredSize = Dimension(separator1.preferredSize.width, height)
    val separator2 = JSeparator(SwingConstants.VERTICAL)
    separator2.preferredSize = Dimension(separator2.preferredSize.width, height)
    add(Box.createHorizontalStrut(5))
    add(separator1)
    add(separator2)
    add(Box.createHorizontalStrut(5))
}

fun JPanel.addVSeparator1(height: Int = 20) {
    val separator1 = JSeparator(SwingConstants.VERTICAL)
    separator1.preferredSize = Dimension(separator1.preferredSize.width / 2, height)
    add(Box.createHorizontalStrut(2))
    add(separator1)
    add(Box.createHorizontalStrut(2))
}

fun JComponent.setWidth(width: Int) {
    preferredSize = Dimension(width, preferredSize.height)
}