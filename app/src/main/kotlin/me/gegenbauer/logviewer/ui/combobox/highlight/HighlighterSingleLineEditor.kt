package me.gegenbauer.logviewer.ui.combobox.highlight

import java.awt.event.ActionListener
import javax.swing.JComboBox

class HighlighterSingleLineEditor(comboBox: JComboBox<*>) : BaseHighlighterEditor<HighlighterTextField>(HighlighterTextField(), comboBox) {

    init {
        textComponent.setTextComponentWrapper(textComponentWrapper)
    }

    override fun addActionListener(l: ActionListener) {
        textComponent.addActionListener(l)
    }

    override fun removeActionListener(l: ActionListener) {
        textComponent.removeActionListener(l)
    }
}