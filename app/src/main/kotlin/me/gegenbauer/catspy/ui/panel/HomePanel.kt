package me.gegenbauer.catspy.ui.panel

import me.gegenbauer.catspy.common.ui.button.GButton
import me.gegenbauer.catspy.common.ui.tab.TabPanel
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.script.ui.ScriptTabPanel
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.ui.menu.TabSelectorPopupMenu
import me.gegenbauer.catspy.ui.supportedTabs
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingUtilities

class HomePanel(override val contexts: Contexts = Contexts.default) : JPanel(), TabPanel {
    override val tabName: String
        get() = "Home"
    override val tabIcon: Icon?
        get() = null
    override val tabTooltip: String?
        get() = null
    override val tabMnemonic: Char
        get() = ' '
    override val closeable: Boolean
        get() = false

    private val tabSelector = GButton("Select Tab")
    private val selectMenu = TabSelectorPopupMenu()

    init {
        componentName = "HomePanel"

        layout = GridBagLayout()
        add(tabSelector)
        tabSelector.addActionListener {
            selectMenu.isVisible = false
            SwingUtilities.updateComponentTreeUI(selectMenu)
            selectMenu.show(supportedTabs, tabSelector)
        }
        
        selectMenu.onTabSelected = { tab ->
            contexts.getContext(MainFrame::class.java)?.addTab(tab.tabClazz.getConstructor().newInstance())
        }
    }

    override fun onTabSelected() {

    }

    override fun onTabUnselected() {

    }

    override fun dispose() {

    }

    override fun getTabContent(): JPanel {
        return this
    }
}

data class TabInfo(
    val tabName: String,
    val tabIcon: Icon?,
    val tabClazz: Class<out TabPanel>,
)