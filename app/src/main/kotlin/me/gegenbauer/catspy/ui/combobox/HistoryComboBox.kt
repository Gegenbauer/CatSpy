package me.gegenbauer.catspy.ui.combobox

import javax.swing.JComboBox

open class HistoryComboBox<T>: JComboBox<HistoryItem<T>>() {
    init {
        renderer = HistoryComboBoxRenderer()
        model = HistoryComboBoxModel()
    }
}