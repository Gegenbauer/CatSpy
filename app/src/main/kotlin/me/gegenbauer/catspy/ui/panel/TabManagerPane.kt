package me.gegenbauer.catspy.ui.panel

import com.formdev.flatlaf.FlatClientProperties
import com.github.weisj.darklaf.iconset.AllIcons
import kotlinx.coroutines.*
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.ui.panel.DeviceLogPanel
import me.gegenbauer.catspy.log.ui.panel.FileLogPanel
import me.gegenbauer.catspy.script.ui.ScriptTabPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.ui.menu.TabSelectorPopupMenu
import me.gegenbauer.catspy.utils.Key
import me.gegenbauer.catspy.utils.TAB_ICON_SIZE
import me.gegenbauer.catspy.utils.registerStroke
import me.gegenbauer.catspy.view.button.ClosableTabHeader
import me.gegenbauer.catspy.view.tab.TabInfo
import me.gegenbauer.catspy.view.tab.TabManager
import me.gegenbauer.catspy.view.tab.TabPanel
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

class TabManagerPane(override val contexts: Contexts = Contexts.default) : TabManager, JTabbedPane() {

    override val supportedTabs = listOf(
        TabInfo(
            STRINGS.ui.logFile,
            GIcons.Tab.FileLog.get(TAB_ICON_SIZE, TAB_ICON_SIZE),
            FileLogPanel::class.java,
            STRINGS.toolTip.tabLogFile
        ),

        TabInfo(
            STRINGS.ui.deviceLog,
            GIcons.Tab.DeviceLog.get(TAB_ICON_SIZE, TAB_ICON_SIZE),
            DeviceLogPanel::class.java,
            STRINGS.toolTip.tabDeviceLog
        ),

        TabInfo(
            STRINGS.ui.script,
            GIcons.Tab.Script.get(TAB_ICON_SIZE, TAB_ICON_SIZE),
            ScriptTabPanel::class.java,
            STRINGS.toolTip.tabScript
        ),
    )

    private val tabHeaders = mutableMapOf<Int, ClosableTabHeader>()
    private val selectMenu = TabSelectorPopupMenu()
    private val homePanel = HomePanel()
    private val scope = MainScope()
    private val addTabButton = JButton().apply {
        icon = AllIcons.Action.Add.get(TAB_ICON_SIZE, TAB_ICON_SIZE)
        addActionListener(createNewTabAction())
    }

    init {
        addTab(homePanel)

        model.addChangeListener {
            val selectedTab = getSelectedTab()
            getAllTabs().filter { it != selectedTab }.forEach { it.onTabUnselected() }
            selectedTab.onTabSelected()
        }

        selectMenu.onTabSelected = { tab ->
            contexts.getContext(MainFrame::class.java)?.addTab(tab.tabClazz.getConstructor().newInstance())
        }

        val trailingComponent = JToolBar().apply {
            isFloatable = false
            border = null
            add(addTabButton)
        }
        putClientProperty(FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT, trailingComponent)

        tabLayoutPolicy = SCROLL_TAB_LAYOUT
        tabPlacement = BOTTOM
        putClientProperty("TabbedPane.tabsOpaque", false)

        addTabSelectStroke()
    }

    private fun addTabSelectStroke() {
        for (index in 0 until 8) {
            registerStroke(Key.getKeyEventInfo(KeyEvent.VK_1 + index, KeyEvent.ALT_DOWN_MASK), "Select Tab $index") {
                selectTab(index)
            }
        }
    }

    private fun createNewTabAction(): Action {
        return object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                selectMenu.isVisible = false
                SwingUtilities.updateComponentTreeUI(selectMenu)
                selectMenu.show(supportedTabs, addTabButton)
            }
        }
    }

    override fun selectTab(tabPanel: TabPanel) {
        selectTab(indexOfComponent(tabPanel.getTabContent()))
    }

    override fun selectTab(index: Int) {
        if (index in 0 until tabCount) {
            selectedIndex = index
        }
    }

    override fun addTab(tabPanel: TabPanel) {
        tabPanel.setup()
        add(tabPanel.tabName, tabPanel.getTabContent())
        tabPanel.setParent(this)
        selectTab(tabPanel)
        setTabComponentAt(indexOfComponent(tabPanel.getTabContent()), createTabHeader(tabPanel))
        SwingUtilities.updateComponentTreeUI(this)
        revalidate()
    }

    private fun createTabHeader(tabPanel: TabPanel): ClosableTabHeader {
        return ClosableTabHeader(
            tabPanel.tabName, this,
            tabPanel.tabIcon, tabPanel.closeable,
            tabPanel.closeable, tabPanel.tabTooltip
        ).apply {
            onCloseClicked = { removeTab(tabPanel) }
            tabHeaders[tabPanel.hashCode()] = this
        }
    }

    override fun removeTab(tabPanel: TabPanel) {
        tabPanel.destroy()
        remove(tabPanel.getTabContent())
    }

    override fun getTab(index: Int): TabPanel {
        return getComponentAt(index) as TabPanel
    }

    override fun getSelectedTabIndex(): Int {
        return selectedIndex
    }

    override fun getAllTabs(): List<TabPanel> {
        return (0 until tabCount).map { getTab(it) }
    }

    override fun getSelectedTab(): TabPanel {
        return getTab(getSelectedTabIndex())
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
        getAllTabs().forEach { it.destroy() }
    }
}