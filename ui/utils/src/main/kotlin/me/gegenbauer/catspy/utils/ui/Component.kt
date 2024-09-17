package me.gegenbauer.catspy.utils.ui

import com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE
import com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_BORDERLESS
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionListener
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.text.JTextComponent

const val BORDER_TYPE_NONE = "none"

infix fun <T : JComponent> T.applyTooltip(tooltip: String?): T {
    this.toolTipText = tooltip
    return this
}

inline val <E> JComboBox<E>.editorComponent: JTextComponent
    get() = editor.editorComponent as JTextComponent


fun JMenuItem.addActionListenerIfNotExist(actionListener: ActionListener) {
    if (!actionListeners.contains(actionListener)) {
        addActionListener(actionListener)
    }
}

fun <T: JFrame> Component.findFrameFromParent(): T {
    return findFrameFromParentOrNull() ?: error("No JFrame found in parent hierarchy")
}

@Suppress("UNCHECKED_CAST")
fun <T: JFrame> Component.findFrameFromParentOrNull(): T? {
    var current = parent
    while (current != null) {
        if (current is JFrame) {
            return current as? T
        }
        current = current.parent
    }
    return null
}

fun JComponent.setWidth(width: Int) {
    preferredSize = Dimension(width, preferredSize.height)
}

fun JComponent.setHeight(height: Int) {
    preferredSize = Dimension(preferredSize.width, height)
}

fun JComponent.setBorderless() {
    putClientProperty(BUTTON_TYPE, BUTTON_TYPE_BORDERLESS)
}