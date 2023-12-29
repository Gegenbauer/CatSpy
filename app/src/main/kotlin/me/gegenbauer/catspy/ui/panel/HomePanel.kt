package me.gegenbauer.catspy.ui.panel

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.ui.menu.TabSelectorPopupMenu
import me.gegenbauer.catspy.utils.TAB_ICON_SIZE
import me.gegenbauer.catspy.view.button.GButton
import me.gegenbauer.catspy.view.hint.HintManager
import me.gegenbauer.catspy.view.panel.StatusBar
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.tab.TabInfo
import me.gegenbauer.catspy.view.tab.TabPanel
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class HomePanel(override val contexts: Contexts = Contexts.default) : JPanel(), TabPanel {
    override val tabName: String = STRINGS.ui.tabHome
    override val tabIcon: Icon = GIcons.Tab.Home.get(TAB_ICON_SIZE, TAB_ICON_SIZE)
    override val closeable: Boolean = false

    private val tabSelector = GButton(STRINGS.ui.selectTab)
    private val selectMenu = TabSelectorPopupMenu()

    override val hint = HintManager.Hint(STRINGS.hints.selectFunctionTab, tabSelector, SwingConstants.TOP, SELECT_TAB_HINT_KEY)

    private val supportedTabs: List<TabInfo>
        get() = contexts.getContext(MainFrame::class.java)?.supportedTabs ?: emptyList()
    private val statusBar = ServiceManager.getContextService(StatusPanel::class.java)
    private val scope = MainScope()

    override fun setup() {
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
        statusBar.logStatus = StatusBar.LogStatus.NONE
    }

    override fun onTabUnselected() {
        // no-op
    }

    override fun getTabContent(): JPanel {
        return this
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

    companion object {
        private const val SELECT_TAB_HINT_KEY = "selectFunctionTab"
    }
}