package me.gegenbauer.catspy.common.ui.combobox

import javax.swing.JComboBox

open class HistoryComboBox<T>: JComboBox<HistoryItem<T>>() {
    init {
        renderer = HistoryComboBoxRenderer()
        model = HistoryComboBoxModel()
    }
}