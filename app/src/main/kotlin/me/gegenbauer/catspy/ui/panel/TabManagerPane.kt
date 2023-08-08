package me.gegenbauer.catspy.ui.panel

import com.github.weisj.darklaf.ui.tabbedpane.DarkTabbedPaneUI
import me.gegenbauer.catspy.common.ui.button.ClosableTabHeader
import me.gegenbauer.catspy.common.ui.tab.TabManager
import me.gegenbauer.catspy.common.ui.tab.TabPanel
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.ui.menu.TabSelectorPopupMenu
import me.gegenbauer.catspy.ui.supportedTabs
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import javax.swing.plaf.TabbedPaneUI

class TabManagerPane(override val contexts: Contexts) : TabManager, JTabbedPane() {
    private val tabHeaders = mutableMapOf<Int, ClosableTabHeader>()
    private val selectMenu = TabSelectorPopupMenu()

    init {
        addTab(HomePanel())

        model.addChangeListener {
            val selectedTab = getSelectedTab()
            getAllTabs().filter { it != selectedTab }.forEach { it.onTabUnselected() }
            selectedTab.onTabSelected()
        }
        putClientProperty(DarkTabbedPaneUI.KEY_NEW_TAB_ACTION, createNewTabAction())

        selectMenu.onTabSelected = { tab ->
            contexts.getContext(MainFrame::class.java)?.addTab(tab.tabClazz.getConstructor().newInstance())
        }
    }

    private fun createNewTabAction(): Action {
        return object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                selectMenu.isVisible = false
                SwingUtilities.updateComponentTreeUI(selectMenu)
                selectMenu.show(supportedTabs, e.source as JComponent)
            }
        }
    }

    override fun selectTab(tabPanel: TabPanel) {
        selectedIndex = indexOfComponent(tabPanel.getTabContent())
    }

    override fun addTab(tabPanel: TabPanel) {
        add(tabPanel.tabName, tabPanel.getTabContent())
        tabPanel.setContexts(contexts)
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
        remove(tabPanel.getTabContent())
        tabPanel.dispose()
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

    override fun setUI(ui: TabbedPaneUI?) {
        super.setUI(ui)
        tabLayoutPolicy = SCROLL_TAB_LAYOUT
        putClientProperty(DarkTabbedPaneUI.KEY_DND, true)
        putClientProperty(DarkTabbedPaneUI.KEY_SHOW_NEW_TAB_BUTTON, true)
        putClientProperty("TabbedPane.tabsOpaque", false)
    }

    override fun getSelectedTab(): TabPanel {
        return getTab(getSelectedTabIndex())
    }
}