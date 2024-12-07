package me.gegenbauer.catspy.view.combobox.highlight

import com.formdev.flatlaf.ui.FlatComboBoxUI
import me.gegenbauer.catspy.utils.ui.DefaultFocusListener
import java.awt.event.FocusEvent
import javax.swing.ComboBoxEditor

class CustomDarkComboBoxUI(private val customEditor: ComboBoxEditor): FlatComboBoxUI() {

    override fun createEditor(): ComboBoxEditor {
        val comboBoxEditor = customEditor
        val comp = comboBoxEditor.editorComponent
        comp.addFocusListener(object : DefaultFocusListener() {
            override fun focusChanged(e: FocusEvent) {
                comboBox?.revalidate()
                comboBox?.repaint()
            }
        })
        return comboBoxEditor
    }
}