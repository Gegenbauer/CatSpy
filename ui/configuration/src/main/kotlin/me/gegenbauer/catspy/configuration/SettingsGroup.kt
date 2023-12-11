package me.gegenbauer.catspy.configuration

import me.gegenbauer.catspy.databinding.property.support.PROPERTY_ENABLED
import java.awt.*
import javax.swing.*

class SettingsGroup(override val title: String) : ISettingsGroup {
    private val gridPanel = JPanel(GridBagLayout())

    private val panel = JPanel().apply {
        layout = BorderLayout(5, 5)
        border = BorderFactory.createTitledBorder(title)
    }
    private val c = GridBagConstraints().apply {
        insets = Insets(5, 5, 5, 5)
        weighty = 1.0
    }

    private var row = 0

    init {
        panel.add(gridPanel, BorderLayout.PAGE_START)
    }

    fun addRow(label: String, comp: JComponent): JLabel {
        return addRow(label, null, comp)
    }

    fun addRow(label: String, tooltip: String?, comp: JComponent): JLabel {
        c.gridy = row++
        val rowLbl = JLabel(label)
        rowLbl.labelFor = comp
        rowLbl.horizontalAlignment = SwingConstants.LEFT
        c.gridx = 0
        c.gridwidth = 1
        c.anchor = GridBagConstraints.LINE_START
        c.weightx = 0.8
        c.fill = GridBagConstraints.NONE
        gridPanel.add(rowLbl, c)
        c.gridx = 1
        c.gridwidth = GridBagConstraints.REMAINDER
        c.anchor = GridBagConstraints.CENTER
        c.weightx = 0.2
        c.fill = GridBagConstraints.HORIZONTAL

        if (tooltip != null) {
            rowLbl.toolTipText = tooltip
            comp.toolTipText = tooltip
        }
        gridPanel.add(comp, c)
        comp.addPropertyChangeListener(PROPERTY_ENABLED) { evt -> rowLbl.isEnabled = evt.newValue as Boolean }
        return rowLbl
    }

    fun end() {
        gridPanel.add(Box.createVerticalGlue())
    }

    override fun buildComponent(): JComponent {
        return panel
    }

    override fun toString(): String {
        return title
    }
}