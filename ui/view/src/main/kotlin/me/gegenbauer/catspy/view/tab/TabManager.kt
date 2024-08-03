package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Context

interface TabManager : Context {
    val supportedTabs: MutableList<FunctionTab>

    fun selectTab(tabPanel: TabPanel)

    fun selectTab(index: Int)

    fun addTab(tabInfo: FunctionTab): TabPanel

    fun addTab(tableClazz: Class<out TabPanel>): TabPanel

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