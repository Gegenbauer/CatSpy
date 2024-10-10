package me.gegenbauer.catspy.configuration

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import me.gegenbauer.catspy.databinding.property.support.PROPERTY_ENABLED
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

open class SettingsGroup(override val title: String) : ISettingsGroup {
    private val gridPanel = JPanel()

    private val panel = JPanel().apply {
        layout = BorderLayout(5, 5)
        border = BorderFactory.createTitledBorder(title)
    }
    private val rows = mutableListOf<Row>()

    private var row = 0

    init {
        panel.add(gridPanel, BorderLayout.PAGE_START)
    }

    override fun initGroup() {
        // no-op
    }

    override fun addRow(label: String, comp: JComponent): JLabel {
        return addRow(label, null, comp)
    }

    override fun addRow(label: String, tooltip: String?, comp: JComponent): JLabel {
        val rowLbl = JLabel(label)
        rowLbl.labelFor = comp
        rowLbl.horizontalAlignment = SwingConstants.LEFT

        if (tooltip != null) {
            rowLbl.toolTipText = tooltip
            comp.toolTipText = tooltip
        }
        comp.addPropertyChangeListener(PROPERTY_ENABLED) { evt -> rowLbl.isEnabled = evt.newValue as Boolean }
        rows.add(Row(rowLbl, comp))
        row++
        return rowLbl
    }

    private fun createTableLayout(): TableLayout {
        val layout = TableLayout(
            doubleArrayOf(0.25, 0.05, 0.7),
            DoubleArray(row) { PREFERRED }
        )
        layout.hGap = 3
        layout.vGap = 3
        return layout
    }

    override fun end() {
        gridPanel.layout = createTableLayout()
        rows.forEachIndexed { index, row ->
            gridPanel.add(row.label, "0, $index")
            gridPanel.add(row.component, "2, $index")
        }
    }

    override fun buildComponent(): JComponent {
        return panel
    }

    override fun toString(): String {
        return title
    }

    private class Row(val label: JLabel, val component: JComponent)
}