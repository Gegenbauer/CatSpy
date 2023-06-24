package me.gegenbauer.catspy.ui

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.GlobalContextManager
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.ddmlib.device.DeviceManager
import me.gegenbauer.catspy.ui.log.LogMainUI
import me.gegenbauer.catspy.ui.menu.HelpMenu
import me.gegenbauer.catspy.ui.menu.SettingsMenu
import me.gegenbauer.catspy.ui.script.ScriptMainUI
import me.gegenbauer.catspy.ui.tab.OnTabChangeListener
import me.gegenbauer.catspy.utils.loadIconWithRealSize
import me.gegenbauer.catspy.viewmodel.GlobalViewModel
import me.gegenbauer.catspy.viewmodel.MainViewModel
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
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

        bindViewModel()

        GlobalContextManager.register(this)
    }

    private fun startServices() {
        val deviceManager = ServiceManager.getContextService(DeviceManager::class.java)
        deviceManager.startMonitor()
    }

    private fun bindViewModel() {
        selectedProperty(settingsMenu.itemDebug) bindDual GlobalViewModel.debug
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
        
        registerSearchStroke()
    }

    private fun registerSearchStroke() {
        var stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
        var actionMapKey = javaClass.name + ":SEARCH_CLOSING"
        var action: Action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                MainViewModel.searchPanelVisible.updateValue(false)
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)
        actionMapKey = javaClass.name + ":SEARCH_OPENING"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
                MainViewModel.searchPanelVisible.updateValue(true)
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)
        actionMapKey = javaClass.name + ":SEARCH_MOVE_PREV"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
//                if (searchPanel.isVisible) {
//                    searchPanel.moveToPrev()
//                }
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0)
        actionMapKey = javaClass.name + ":SEARCH_MOVE_NEXT"
        action = object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent) {
//                if (searchPanel.isVisible) {
//                    searchPanel.moveToNext()
//                }
            }
        }
        rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, actionMapKey)
        rootPane.actionMap.put(actionMapKey, action)
    }
}



