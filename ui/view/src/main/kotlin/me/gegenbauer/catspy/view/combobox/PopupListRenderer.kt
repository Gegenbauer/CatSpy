package me.gegenbauer.catspy.view.combobox

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

class PopupListRenderer<E>(private val comboBox: JComboBox<E>) : ListCellRenderer<E> {
    private val EVEN_COLOR = Color(0xE6_FF_E6)
    private var targetIndex = 0
    var rolloverIndex = -1
    private val panel = object : JPanel(BorderLayout()) {
        override fun getPreferredSize(): Dimension {
            val d = super.getPreferredSize()
            d.width = 0
            return d
        }
    }
    private val renderer: ListCellRenderer<in E> = DefaultListCellRenderer()
    private val deleteButton = object : JButton("x") {
        override fun getPreferredSize(): Dimension {
            return Dimension(16, 16)
        }

        override fun updateUI() {
            super.updateUI()
            border = BorderFactory.createEmptyBorder()
            isFocusable = false
            isRolloverEnabled = false
            isContentAreaFilled = false
        }
    }

    init {
        deleteButton.addActionListener {
            val model = comboBox.model
            val oneOrMore = model.size > 1
            if (oneOrMore && model is MutableComboBoxModel<*>) {
                (model as MutableComboBoxModel<*>).removeElementAt(targetIndex)
                comboBox.selectedIndex = -1
                comboBox.showPopup()
            }
        }
        panel.isOpaque = true
        panel.add(deleteButton, BorderLayout.EAST)
    }

    override fun getListCellRendererComponent(
        list: JList<out E>,
        value: E,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val c = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (index < 0) {
            return c
        }
        if (c is JComponent) {
            c.isOpaque = false
        }
        targetIndex = index
        panel.background = if (isSelected) list.selectionBackground else if (index % 2 == 0) EVEN_COLOR else list?.background
        val showDeleteButton = list.model.size > 1
        deleteButton.isVisible = showDeleteButton
        if (showDeleteButton) {
            val isRollover = index == rolloverIndex
            deleteButton.model.isRollover = isRollover
            deleteButton.foreground = if (isRollover) Color.WHITE else list.foreground
        }
        panel.add(c)
        return panel
    }
}