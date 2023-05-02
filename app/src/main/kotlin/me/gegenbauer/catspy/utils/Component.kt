package me.gegenbauer.catspy.utils

import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.text.JTextComponent

infix fun <T: JComponent> T.applyTooltip(tooltip: String?): T {
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

inline val MouseEvent.isDoubleClick: Boolean
    get() = clickCount == 2

inline val MouseEvent.isSingleClick: Boolean
    get() = clickCount == 1