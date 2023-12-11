package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Disposable

interface TabManager: Disposable, Context {
    val supportedTabs: List<TabInfo>

    fun selectTab(tabPanel: TabPanel)

    fun selectTab(index: Int)

    fun addTab(tabPanel: TabPanel)

    fun removeTab(tabPanel: TabPanel)

    fun getTabCount(): Int

    fun getTab(index: Int): TabPanel

    fun getSelectedTabIndex(): Int

    fun getAllTabs(): List<TabPanel>

    fun getSelectedTab(): TabPanel

    override fun destroy() {
        super.destroy()
        getAllTabs().forEach { it.destroy() }
    }
}