package me.gegenbauer.catspy.ui

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.catspy.common.configuration.UIConfManager
import me.gegenbauer.catspy.common.ui.tab.OnTabChangeListener
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.GlobalContextManager
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.ddmlib.AndroidDebugBridgeManager
import me.gegenbauer.catspy.ddmlib.device.DeviceManager
import me.gegenbauer.catspy.log.ui.LogMainUI
import me.gegenbauer.catspy.ui.menu.HelpMenu
import me.gegenbauer.catspy.ui.menu.SettingsMenu
import me.gegenbauer.catspy.script.ui.ScriptMainUI
import me.gegenbauer.catspy.utils.loadIconWithRealSize
import me.gegenbauer.catspy.common.viewmodel.GlobalViewModel
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.system.exitProcess

/**
 *  TODO 将底部状态栏抽出，并增加进度条，显示某些任务进度
 */
class MainUI(title: String, override val contexts: Contexts = Contexts.default) : JFrame(title), Context {

    //region menu
    private val helpMenu = HelpMenu()
    private val settingsMenu = SettingsMenu()
    private val menuBar = JMenuBar().apply {
        add(this@MainUI.settingsMenu)
        add(this@MainUI.helpMenu)
    }
    //endregion

    private val tabbedPane = JTabbedPane()
    private val logMainUI = LogMainUI()
    private val scriptMainUI = ScriptMainUI()

    init {
        configureWindow()

        createUI()

        registerEvents()

        bindGlobalViewModel()

        GlobalContextManager.register(this)
    }

    private fun startServices() {
        val adbManager = ServiceManager.getContextService(AndroidDebugBridgeManager::class.java)
        adbManager.init("adb")
        val deviceManager = ServiceManager.getContextService(DeviceManager::class.java)
        adbManager.addListener(deviceManager)
    }

    private fun bindGlobalViewModel() {
        selectedProperty(settingsMenu.globalDebug) bindDual GlobalViewModel.globalDebug
        selectedProperty(settingsMenu.bindingDebug) bindDual GlobalViewModel.dataBindingDebug
    }

    override fun configureContext(context: Context) {
        startServices()
        super.configureContext(context)
        logMainUI.setContexts(contexts)
        scriptMainUI.setContexts(contexts)
    }

    private fun registerEvents() {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                exit()
            }
        })
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
    }

    private fun configureWindow() {
        iconImage = loadIconWithRealSize<DerivableImageIcon>("logo.png").image
        defaultCloseOperation = EXIT_ON_CLOSE

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

    private fun exit() {
        ServiceManager.dispose(this)
        saveConfigOnDestroy()
        exitProcess(0)
    }

    private fun saveConfigOnDestroy() {
        UIConfManager.uiConf.frameX = location.x
        UIConfManager.uiConf.frameY = location.y
        UIConfManager.uiConf.frameWidth = size.width
        UIConfManager.uiConf.frameHeight = size.height
        UIConfManager.uiConf.frameExtendedState = extendedState
        UIConfManager.saveUI()
    }

    private fun createUI() {
        jMenuBar = menuBar
        layout = BorderLayout()

        add(tabbedPane)
        tabbedPane.add("日志", logMainUI)
        tabbedPane.add("脚本", scriptMainUI)
    }
}



