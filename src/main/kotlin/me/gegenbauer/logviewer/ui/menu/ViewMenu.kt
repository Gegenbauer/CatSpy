package me.gegenbauer.logviewer.ui.menu

import me.gegenbauer.logviewer.strings.STRINGS
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JMenuItem

class ViewMenu : JMenu(), ActionListener {
    val itemFull = JCheckBoxMenuItem(STRINGS.ui.viewFull)
    val itemSearch = JCheckBoxMenuItem(STRINGS.ui.search)
    val itemRotation = JMenuItem(STRINGS.ui.rotation)

    var onItemFullClicked: (Boolean) -> Unit = {}
    var onItemSearchClicked: () -> Unit = {}
    var onItemRotationClicked: () -> Unit = {}

    init {
        text = STRINGS.ui.view
        mnemonic = KeyEvent.VK_V

        itemFull.addActionListener(this)
        itemSearch.addActionListener(this)
        itemRotation.addActionListener(this)

        add(itemFull)
        addSeparator()
        add(itemSearch)
        addSeparator()
        add(itemRotation)
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.source) {
            itemFull -> {
                onItemFullClicked(itemFull.state)
            }
            itemSearch -> {
                onItemSearchClicked()
            }
            itemRotation -> {
                onItemRotationClicked()
            }
        }
    }
}