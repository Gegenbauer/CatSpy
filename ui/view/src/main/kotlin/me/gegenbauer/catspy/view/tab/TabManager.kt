package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Disposable

interface TabManager: Disposable, Context {
    fun selectTab(tabPanel: TabPanel)

    fun addTab(tabPanel: TabPanel)

    fun removeTab(tabPanel: TabPanel)

    fun getTabCount(): Int

    fun getTab(index: Int): TabPanel

    fun getSelectedTabIndex(): Int

    fun getAllTabs(): List<TabPanel>

    fun getSelectedTab(): TabPanel

    override fun destroy() {
        getAllTabs().forEach { it.destroy() }
    }
}