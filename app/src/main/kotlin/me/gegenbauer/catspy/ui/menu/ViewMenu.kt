package me.gegenbauer.catspy.ui.menu

import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.Menu
import me.gegenbauer.catspy.utils.loadThemedIcon
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JMenuItem

class ViewMenu : JMenu(), ActionListener {
    val itemFull = JCheckBoxMenuItem(STRINGS.ui.viewFull)
    val itemSearch = JCheckBoxMenuItem(STRINGS.ui.search)
    val itemRotation = JMenuItem(STRINGS.ui.rotation).apply {
        icon = loadThemedIcon("rotate.svg", Menu.MENU_ITEM_ICON_SIZE)
    }

    var onItemRotationClicked: () -> Unit = {}

    init {
        text = STRINGS.ui.view
        mnemonic = KeyEvent.VK_V

        itemRotation.addActionListener(this)

        add(itemFull)
        addSeparator()
        add(itemSearch)
        addSeparator()
        add(itemRotation)
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.source) {
            itemRotation -> {
                onItemRotationClicked()
            }
        }
    }
}