package me.gegenbauer.catspy.view.combobox

import com.formdev.flatlaf.ui.FlatComboBoxUI
import javax.swing.JList

open class RemoveButtonComboBoxUI: FlatComboBoxUI() {

    fun getList(): JList<*>? {
        return popup.list
    }
}