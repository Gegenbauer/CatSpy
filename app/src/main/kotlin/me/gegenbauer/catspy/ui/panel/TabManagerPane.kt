package me.gegenbauer.catspy.ui.panel

import com.formdev.flatlaf.FlatClientProperties.*
import com.github.weisj.darklaf.iconset.AllIcons
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.serialize.toLogMetadata
import me.gegenbauer.catspy.log.ui.tab.DeviceLogMainPanel
import me.gegenbauer.catspy.log.ui.tab.FileLogMainPanel
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.script.ui.ScriptTabPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.ui.menu.TabSelectorPopupMenu
import me.gegenbauer.catspy.utils.persistence.Preferences
import me.gegenbauer.catspy.utils.ui.Key
import me.gegenbauer.catspy.utils.ui.TAB_ICON_SIZE
import me.gegenbauer.catspy.utils.ui.registerStroke
import me.gegenbauer.catspy.view.button.ClosableTabHeader
import me.gegenbauer.catspy.view.tab.FunctionTab
import me.gegenbauer.catspy.view.tab.TabManager
import me.gegenbauer.catspy.view.tab.TabPanel
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

class TabManagerPane(override val contexts: Contexts = Contexts.default) : TabManager, JTabbedPane() {

    override val supportedTabs = mutableListOf<FunctionTab>()

    private val homeTab: FunctionTab by lazy {
        FunctionTab(
            STRINGS.ui.tabHome,
            GIcons.Tab.Home.get(TAB_ICON_SIZE, TAB_ICON_SIZE),
            HomePanel::class.java,
            STRINGS.toolTip.tabHome,
            false
        )
    }

    private val defaultSupportedTabs: List<FunctionTab> by lazy {
        listOf(
            FunctionTab(
                STRINGS.ui.tabLogFile,
                GIcons.Tab.FileLog.get(TAB_ICON_SIZE, TAB_ICON_SIZE),
                FileLogMainPanel::class.java,
                STRINGS.toolTip.tabLogFile
            ),
            FunctionTab(
                STRINGS.ui.tabDeviceLog,
                GIcons.Tab.DeviceLog.get(TAB_ICON_SIZE, TAB_ICON_SIZE),
                DeviceLogMainPanel::class.java,
                STRINGS.toolTip.tabDeviceLog
            ),
        )
    }

    private val selectMenu = TabSelectorPopupMenu()
    private val scope = MainScope()
    private val addTabButton = JButton().apply {
        icon = AllIcons.Action.Add.get(TAB_ICON_SIZE, TAB_ICON_SIZE)
        addActionListener(createNewTabAction())
    }

    private val logMetadataManager: LogMetadataManager by lazy {
        ServiceManager.getContextService(LogMetadataManager::class.java)
    }

    private val dataTransferHandler = object : TransferHandler() {
        override fun canImport(info: TransferSupport): Boolean {
            return getSelectedTab().isDataImportSupported(info)
        }

        override fun importData(info: TransferSupport): Boolean {
            GLog.d(
                TAG, "os:$currentPlatform, drop:${info.dropAction}, " +
                        "sourceDrop:${info.sourceDropActions},userDrop:${info.userDropAction}"
            )
            return getSelectedTab().handleDataImport(info)
        }
    }

    init {
        loadHomeTab()

        collectTabs()

        model.addChangeListener {
            val selectedTab = getSelectedTab()
            getAllTabs().filter { it != selectedTab }.forEach { it.isTabSelected = false }
            selectedTab.isTabSelected = true
        }

        selectMenu.onTabSelected = { tabInfo ->
            contexts.getContext(MainFrame::class.java)?.addTab(tabInfo)
        }

        configureAddTabButton()

        tabLayoutPolicy = SCROLL_TAB_LAYOUT
        tabPlacement = BOTTOM
        putClientProperty(PROPERTY_TAB_OPAQUE, false)
        putClientProperty(TABBED_PANE_TABS_POPUP_POLICY, TABBED_PANE_POLICY_AS_NEEDED)

        addTabSelectStroke()

        transferHandler = dataTransferHandler
    }

    private fun loadHomeTab() {
        scope.launch { addTab(homeTab) }
    }

    private fun collectTabs() {
        scope.launch {
            supportedTabs.addAll(defaultSupportedTabs)
        }
    }

    private fun configureAddTabButton() {
        val trailingComponent = JToolBar().apply {
            isFloatable = false
            border = null
            add(addTabButton)
        }
        putClientProperty(TABBED_PANE_TRAILING_COMPONENT, trailingComponent)
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

    override fun addTab(tabInfo: FunctionTab): TabPanel {
        val tabPanel = tabInfo.clazz.getConstructor().newInstance()
        tabPanel.setup(getTabBundle(tabPanel))
        add(tabInfo.name, tabPanel.getTabContent())
        tabPanel.setParent(this)
        selectTab(tabPanel)
        val tabHeader = createTabHeader(tabPanel, tabInfo)
        tabPanel.setTabNameController { tabHeader.setTabName(it) }
        tabPanel.setTabTooltipController { tabHeader.setTabTooltip(it ?: EMPTY_STRING) }
        setTabComponentAt(indexOfComponent(tabPanel.getTabContent()), tabHeader)
        return tabPanel
    }

    override fun addTab(tableClazz: Class<out TabPanel>): TabPanel {
        val tabInfo = supportedTabs.firstOrNull { it.clazz == tableClazz }
            ?: throw IllegalArgumentException("${tableClazz.name} not supported")
        return addTab(tabInfo)
    }

    private fun getTabBundle(tabPanel: TabPanel): Bundle? {
        val metadata = when (tabPanel) {
            is FileLogMainPanel -> {
                val lastUsedLogType = Preferences.getString(LogMetadata.KEY)
                logMetadataManager.getMetadata(lastUsedLogType)
                    ?: logMetadataManager.getMetadata(LogMetadataManager.LOG_TYPE_RAW)
            }

            is DeviceLogMainPanel -> logMetadataManager.getMetadata(LogMetadataManager.LOG_TYPE_DEVICE)
            else -> null
        }
        return metadata?.let { Bundle().apply { put(LogMetadata.KEY, it.toLogMetadata()) } }
    }

    private fun createTabHeader(tabPanel: TabPanel, tabInfo: FunctionTab): ClosableTabHeader {
        return ClosableTabHeader(
            tabInfo.name, this,
            tabInfo.icon, tabInfo.closeable,
            false, tabInfo.tooltip
        ).apply {
            onCloseClicked = { removeTab(tabPanel) }
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

    companion object {
        private const val TAG = "TabManagerPane"
        private const val PROPERTY_TAB_OPAQUE = "TabbedPane.tabsOpaque"
    }
}