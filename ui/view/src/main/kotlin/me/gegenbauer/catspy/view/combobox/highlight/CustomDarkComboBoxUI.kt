package me.gegenbauer.catspy.view.combobox.highlight

import com.github.weisj.darklaf.ui.combobox.DarkComboBoxUI
import me.gegenbauer.catspy.utils.DefaultFocusListener
import java.awt.event.FocusEvent
import javax.swing.ComboBoxEditor

class CustomEditorDarkComboBoxUI(private val customEditor: ComboBoxEditor): DarkComboBoxUI() {

    override fun createEditor(): ComboBoxEditor {
        val comboBoxEditor = customEditor
        val comp = comboBoxEditor.editorComponent
        comp.addFocusListener(object : DefaultFocusListener() {
            override fun focusChanged(e: FocusEvent) {
                comboBox.revalidate()
                comboBox.repaint()
            }
        })
        return comboBoxEditor
    }
}