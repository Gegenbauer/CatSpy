package me.gegenbauer.catspy.ui

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.catspy.common.configuration.UIConfManager
import me.gegenbauer.catspy.common.ui.tab.OnTabChangeListener
import me.gegenbauer.catspy.common.ui.tab.TabManager
import me.gegenbauer.catspy.common.viewmodel.GlobalViewModel
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.GlobalContextManager
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.ui.menu.HelpMenu
import me.gegenbauer.catspy.ui.menu.SettingsMenu
import me.gegenbauer.catspy.ui.panel.TabManagerPane
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.JTabbedPane

/**
 *  TODO 将底部状态栏抽出，并增加进度条，显示某些任务进度
 */
class MainFrame(
    title: String,
    override val contexts: Contexts = Contexts.default,
    private val tabbedPane: TabManagerPane = TabManagerPane(contexts),
) : JFrame(title), TabManager by tabbedPane {

    //region menu
    private val helpMenu = HelpMenu()
    private val settingsMenu = SettingsMenu()
    private val menuBar = JMenuBar().apply {
        add(this@MainFrame.settingsMenu)
        add(this@MainFrame.helpMenu)
    }
    //endregion

    init {
        configureWindow()

        createUI()

        registerEvents()

        bindGlobalViewModel()

        GlobalContextManager.register(this)
    }

    private fun createUI() {
        jMenuBar = menuBar
        layout = BorderLayout()

        add(tabbedPane)
    }

    private fun startServices() {
        val deviceManager = ServiceManager.getContextService(AdamDeviceManager::class.java)
        deviceManager.startMonitor()
    }

    private fun bindGlobalViewModel() {
        selectedProperty(settingsMenu.globalDebug) bindDual GlobalViewModel.globalDebug
        selectedProperty(settingsMenu.bindingDebug) bindDual GlobalViewModel.dataBindingDebug
        selectedProperty(settingsMenu.taskDebug) bindDual GlobalViewModel.taskDebug
        selectedProperty(settingsMenu.ddmDebug) bindDual GlobalViewModel.ddmDebug
    }

    override fun configureContext(context: Context) {
        startServices()
        super.configureContext(context)
        tabbedPane.getAllTabs().forEach { it.setContexts(contexts) }
    }

    private fun registerEvents() {
        tabbedPane.addChangeListener { event ->
            if (event.source is JTabbedPane) {
                val pane = event.source as JTabbedPane
                val index = pane.selectedIndex
                val focusedTabs = pane.components.filterIndexed { i, _ -> i == index }
                focusedTabs.forEach { (it as? OnTabChangeListener)?.onTabFocusChanged(true) }
                val unfocusedTabs = pane.components.filterIndexed { i, _ -> i != index }
                unfocusedTabs.forEach { (it as? OnTabChangeListener)?.onTabFocusChanged(false) }
            }
        }
        addWindowFocusListener(object : WindowAdapter() {
            override fun windowLostFocus(e: WindowEvent) {
                getWindows().filter { it.type == Type.POPUP }.forEach { it.dispose() }
            }
        })
    }

    private fun configureWindow() {
        iconImage = (GIcons.Logo.get(200, 200) as DerivableImageIcon).image

        UIConfManager.uiConf.run {
            extendedState = if (frameX == 0 || frameY == 0 || frameWidth == 0 || frameHeight == 0) {
                MAXIMIZED_BOTH
            } else {
                frameExtendedState
            }
            if (frameX != 0 && frameY != 0) {
                setLocation(frameX, frameY)
            }
            if (frameWidth != 0 && frameHeight != 0) {
                setSize(frameWidth, frameHeight)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tabbedPane.onDestroy()
        ServiceManager.dispose(this)
        saveConfigOnDestroy()
    }

    private fun saveConfigOnDestroy() {
        UIConfManager.uiConf.frameX = location.x
        UIConfManager.uiConf.frameY = location.y
        UIConfManager.uiConf.frameWidth = size.width
        UIConfManager.uiConf.frameHeight = size.height
        UIConfManager.uiConf.frameExtendedState = extendedState
        UIConfManager.saveUI()
    }
}



