package me.gegenbauer.catspy.ui.panel

import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.ui.menu.TabSelectorPopupMenu
import me.gegenbauer.catspy.ui.supportedTabs
import me.gegenbauer.catspy.utils.TAB_ICON_SIZE
import me.gegenbauer.catspy.view.button.GButton
import me.gegenbauer.catspy.view.tab.TabPanel
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingUtilities

class HomePanel(override val contexts: Contexts = Contexts.default) : JPanel(), TabPanel {
    override val tabName: String = STRINGS.ui.tabHome
    override val tabIcon: Icon = GIcons.Tab.Home.get(TAB_ICON_SIZE, TAB_ICON_SIZE)
    override val closeable: Boolean = false

    private val tabSelector = GButton(STRINGS.ui.selectTab)
    private val selectMenu = TabSelectorPopupMenu()

    init {
        componentName = this::class.java.simpleName

        layout = GridBagLayout()
        tabSelector.toolTipText = STRINGS.toolTip.selectTabBtn
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

    override fun getTabContent(): JPanel {
        return this
    }

}

data class TabInfo(
    val tabName: String,
    val tabIcon: Icon?,
    val tabClazz: Class<out TabPanel>,
    val tooltip: String? = null
)