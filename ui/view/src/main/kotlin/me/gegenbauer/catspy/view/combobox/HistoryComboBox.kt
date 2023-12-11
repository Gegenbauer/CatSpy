package me.gegenbauer.catspy.view.combobox

import javax.swing.JComboBox

open class HistoryComboBox<T>(items: List<T>): JComboBox<HistoryItem<T>>(items.map { HistoryItem(it) }.toTypedArray()) {
    init {
        model = HistoryComboBoxModel()
    }
}