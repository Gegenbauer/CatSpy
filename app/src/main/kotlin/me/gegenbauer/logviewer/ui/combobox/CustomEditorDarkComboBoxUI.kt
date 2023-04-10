package me.gegenbauer.logviewer.ui.combobox

import com.github.weisj.darklaf.ui.combobox.DarkComboBoxUI
import java.awt.Insets
import javax.swing.JComponent

class CustomEditorDarkComboBoxUI: DarkComboBoxUI() {
    override fun getEditorInsets(c: JComponent?): Insets {
        return Insets(0, 0, 0, 0)
    }
}