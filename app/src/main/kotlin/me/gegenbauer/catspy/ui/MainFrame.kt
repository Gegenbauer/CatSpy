package me.gegenbauer.catspy.ui

import com.formdev.flatlaf.extras.FlatSVGUtils
import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.conf.GlobalConfSync
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.GlobalContextManager
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.iconset.appIcons
import me.gegenbauer.catspy.java.ext.FileSaveEvent
import me.gegenbauer.catspy.java.ext.NormalEvent
import me.gegenbauer.catspy.network.update.data.Release
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.ui.dialog.UpdateDialog
import me.gegenbauer.catspy.ui.menu.HelpMenu
import me.gegenbauer.catspy.ui.menu.SettingsMenu
import me.gegenbauer.catspy.ui.panel.MemoryStatusBar
import me.gegenbauer.catspy.ui.panel.TabManagerPane
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.tab.OnTabChangeListener
import me.gegenbauer.catspy.view.tab.TabManager
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.JOptionPane
import javax.swing.JTabbedPane

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

    private val globalStatus = ServiceManager.getContextService(StatusPanel::class.java)
    private val memoryStatusBar = MemoryStatusBar()
    private val mainViewModel = ServiceManager.getContextService(this, MainViewModel::class.java)
    private val scope = MainScope()

    init {
        configureWindow()

        createUI()

        registerEvents()

        bindGlobalProperties()

        GlobalContextManager.register(this)

        mainViewModel.startMemoryMonitor()
        mainViewModel.checkUpdate()
    }

    private fun createUI() {
        jMenuBar = menuBar
        layout = BorderLayout()

        add(tabbedPane, BorderLayout.CENTER)
        add(globalStatus, BorderLayout.SOUTH)

        globalStatus.memoryMonitorBar = memoryStatusBar

        mainViewModel.startMemoryMonitor()
    }

    private fun bindGlobalProperties() {
        selectedProperty(settingsMenu.globalDebug) bindDual GlobalConfSync.globalDebug
        selectedProperty(settingsMenu.bindingDebug) bindDual GlobalConfSync.dataBindingDebug
        selectedProperty(settingsMenu.taskDebug) bindDual GlobalConfSync.taskDebug
        selectedProperty(settingsMenu.ddmDebug) bindDual GlobalConfSync.ddmDebug
        selectedProperty(settingsMenu.cacheDebug) bindDual GlobalConfSync.cacheDebug
        selectedProperty(settingsMenu.logDebug) bindDual GlobalConfSync.logDebug
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        tabbedPane.getAllTabs().forEach { it.setParent(this) }
        settingsMenu.setParent(this)
        mainViewModel.setParent(this)
        memoryStatusBar.setParent(this)
        helpMenu.setParent(this)
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
        observeEventFlow()
    }

    private fun observeEventFlow() {
        scope.launch {
            mainViewModel.eventFlow.collect {
                when (it) {
                    is NormalEvent -> {
                        handleNormalEvent(it)
                    }
                    is Release -> {
                        handleReleaseEvent(it)
                    }
                    is FileSaveEvent -> {
                        handleFileSaveEvent(it)
                    }
                }
            }
        }
    }

    private fun handleNormalEvent(event: NormalEvent) {
        event.takeIf { event.message.isNotBlank() }?.let { msg ->
            JOptionPane.showMessageDialog(
                this@MainFrame,
                msg,
                "",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    private fun handleReleaseEvent(release: Release) {
        UpdateDialog(this@MainFrame, release) {
            mainViewModel.startDownloadRelease(release)
        }.show()
    }

    private fun handleFileSaveEvent(fileSaveEvent: FileSaveEvent) {
        val result = JOptionPane.showOptionDialog(
            this@MainFrame,
            fileSaveEvent.message,
            fileSaveEvent.title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            arrayOf(STRINGS.ui.showFileInFileManager, STRINGS.ui.cancel),
            STRINGS.ui.showFileInFileManager
        )
        if (result == JOptionPane.OK_OPTION) {
            val file = File(fileSaveEvent.fileAbsolutePath)
            if (file.exists()) {
                currentPlatform.showFileInExplorer(file)
            }
        }
    }

    private fun configureWindow() {
        iconImages = appIcons

        SettingsManager.updateSettings {
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

    override fun destroy() {
        super.destroy()
        scope.cancel()
        ServiceManager.dispose(this)
        ServiceManager.dispose(Context.process)
        saveConfigOnDestroy()
        dispose()
    }

    private fun saveConfigOnDestroy() {
        SettingsManager.updateSettings {
            frameX = location.x
            frameY = location.y
            frameWidth = size.width
            frameHeight = size.height
            frameExtendedState = extendedState
        }
    }
}



