package me.gegenbauer.catspy.ui

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import kotlinx.coroutines.MainScope
import me.gegenbauer.catspy.common.configuration.UIConfManager
import me.gegenbauer.catspy.common.ui.tab.OnTabChangeListener
import me.gegenbauer.catspy.common.viewmodel.GlobalViewModel
import me.gegenbauer.catspy.context.*
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.log.ui.LogMainUI
import me.gegenbauer.catspy.script.ui.ScriptMainUI
import me.gegenbauer.catspy.ui.menu.HelpMenu
import me.gegenbauer.catspy.ui.menu.SettingsMenu
import me.gegenbauer.catspy.utils.loadIconWithRealSize
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.JTabbedPane

/**
 *  TODO 将底部状态栏抽出，并增加进度条，显示某些任务进度
 */
class MainUI(title: String, override val contexts: Contexts = Contexts.default) : JFrame(title), Context, Disposable {

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
        val deviceManager = ServiceManager.getContextService(AdamDeviceManager::class.java)
        deviceManager.startMonitor()
    }

    private fun bindGlobalViewModel() {
        selectedProperty(settingsMenu.globalDebug) bindDual GlobalViewModel.globalDebug
        selectedProperty(settingsMenu.bindingDebug) bindDual GlobalViewModel.dataBindingDebug
        selectedProperty(settingsMenu.taskDebug) bindDual GlobalViewModel.taskDebug
    }

    override fun configureContext(context: Context) {
        startServices()
        super.configureContext(context)
        logMainUI.setContexts(contexts)
        scriptMainUI.setContexts(contexts)
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
    }

    private fun configureWindow() {
        iconImage = loadIconWithRealSize<DerivableImageIcon>("logo.png").image

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
    override fun dispose() {
        ServiceManager.dispose(this)
        logMainUI.dispose()
        scriptMainUI.dispose()
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

    private fun createUI() {
        jMenuBar = menuBar
        layout = BorderLayout()

        add(tabbedPane)
        tabbedPane.add("日志", logMainUI)
        tabbedPane.add("脚本", scriptMainUI)
    }
}



