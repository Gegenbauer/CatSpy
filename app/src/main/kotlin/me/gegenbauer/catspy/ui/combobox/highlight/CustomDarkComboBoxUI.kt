package me.gegenbauer.catspy.ui.combobox.highlight

import com.github.weisj.darklaf.ui.combobox.DarkComboBoxUI
import me.gegenbauer.catspy.databinding.property.support.DefaultFocusListener
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.ComboBoxEditor

class CustomEditorDarkComboBoxUI(private val customEditor: ComboBoxEditor): DarkComboBoxUI() {

    override fun createEditor(): ComboBoxEditor {
        val comboBoxEditor = customEditor
        val comp = comboBoxEditor.editorComponent
        comp.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                process(e)
            }

            private fun process(e: KeyEvent) {
                val code = e.keyCode
                if ((code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) && e.modifiersEx == 0) {
                    comboBox.dispatchEvent(e)
                }
            }

            override fun keyReleased(e: KeyEvent) {
                process(e)
            }
        })
        comp.addFocusListener(object : DefaultFocusListener() {
            override fun focusChanged(e: FocusEvent) {
                comboBox.revalidate()
                comboBox.repaint()
            }
        })
        return comboBoxEditor
    }

    companion object {
        private const val TAG = "CustomDarkComboBoxUI"
    }
}