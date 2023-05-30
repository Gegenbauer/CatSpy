package me.gegenbauer.catspy.ui.combobox

import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class HistoryComboBoxRenderer<T> : JLabel(), ListCellRenderer<HistoryItem<T>> {
    override fun getListCellRendererComponent(
        list: JList<out HistoryItem<T>>?,
        value: HistoryItem<T>?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (value != null) {
            text = value.content.toString()
        }
        return this
    }
}