package me.gegenbauer.logviewer.utils

import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.text.JTextComponent

infix fun <T: JComponent> T.applyTooltip(tooltip: String?): T {
    this.toolTipText = tooltip
    return this
}

inline val <E> JComboBox<E>.editorComponent: JTextComponent
    get() = editor.editorComponent as JTextComponent