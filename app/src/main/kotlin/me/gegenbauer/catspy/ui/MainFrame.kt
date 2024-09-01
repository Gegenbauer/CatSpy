package me.gegenbauer.catspy.ui

import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.FileSaveEvent
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.GlobalEventManager
import me.gegenbauer.catspy.concurrency.GlobalMessageManager
import me.gegenbauer.catspy.concurrency.Message
import me.gegenbauer.catspy.concurrency.NormalEvent
import me.gegenbauer.catspy.concurrency.OpenAdbPathSettingsEvent
import me.gegenbauer.catspy.conf.GlobalConfSync
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.ddmlib.adb.AdbConf
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceMonitor
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.iconset.appIcons
import me.gegenbauer.catspy.java.ext.*
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.network.update.ReleaseEvent
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.ui.dialog.GThemeSettingsDialog
import me.gegenbauer.catspy.ui.dialog.GThemeSettingsDialog.Companion.GROUP_INDEX_ADB
import me.gegenbauer.catspy.ui.dialog.UpdateDialog
import me.gegenbauer.catspy.ui.menu.HelpMenu
import me.gegenbauer.catspy.ui.menu.SettingsMenu
import me.gegenbauer.catspy.ui.panel.MemoryStatusBar
import me.gegenbauer.catspy.ui.panel.TabManagerPane
import me.gegenbauer.catspy.utils.ui.dismissOnClickOutsideWindows
import me.gegenbauer.catspy.utils.ui.registerDismissOnClickOutsideListener
import me.gegenbauer.catspy.view.panel.StatusPanel
import me.gegenbauer.catspy.view.tab.OnTabChangeListener
import me.gegenbauer.catspy.view.tab.TabManager
import java.awt.BorderLayout
import java.awt.Frame
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
    private val mainViewModel = ServiceManager.getContextService(this, MainViewModel::class.java)
    private val memoryStatusBar = MemoryStatusBar()
    private val scope = MainScope()

    init {
        configureWindow()

        createUI()

        registerEvents()

        bindGlobalProperties()

        configureAdbPath()

        preTriggerKotlinReflection()

        loadLogMetadata()
    }

    private fun createUI() {
        jMenuBar = menuBar
        layout = BorderLayout()

        add(tabbedPane, BorderLayout.CENTER)
        add(globalStatus, BorderLayout.SOUTH)

        globalStatus.memoryMonitorBar = memoryStatusBar

        mainViewModel.startMemoryMonitor()
        mainViewModel.checkUpdate()
    }

    private fun bindGlobalProperties() {
        selectedProperty(settingsMenu.globalDebug) bindDual GlobalConfSync.globalDebug
        selectedProperty(settingsMenu.bindingDebug) bindDual GlobalConfSync.dataBindingDebug
        selectedProperty(settingsMenu.taskDebug) bindDual GlobalConfSync.taskDebug
        selectedProperty(settingsMenu.ddmDebug) bindDual GlobalConfSync.ddmDebug
        selectedProperty(settingsMenu.cacheDebug) bindDual GlobalConfSync.cacheDebug
        selectedProperty(settingsMenu.logDebug) bindDual GlobalConfSync.logDebug
    }

    private fun configureAdbPath() {
        val adbPath = SettingsManager.adbPath
        GLog.i(TAG, "[configureAdbPath] detected adbPath: $adbPath")
        ServiceManager.getContextService(AdamDeviceMonitor::class.java).configure(AdbConf(adbPath))
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
        observeGlobalMessage()
        observeGlobalEvent()
        registerDismissOnClickOutsideListener { it.javaClass.simpleName in dismissOnClickOutsideWindows }
    }

    private fun preTriggerKotlinReflection() {
        scope.launch {
            withContext(Dispatchers.GIO) {
                KotlinReflectionPreTrigger().trigger()
            }
        }
    }

    private fun loadLogMetadata() {
        scope.launch {
            withContext(Dispatchers.GIO) {
                ServiceManager.getContextService(LogMetadataManager::class.java).loadAllMetadata()
            }
        }
    }

    private fun observeEventFlow() {
        scope.launch {
            mainViewModel.eventFlow.collect {
                when (it) {
                    is NormalEvent -> {
                        handleNormalEvent(it)
                    }

                    is ReleaseEvent -> {
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
                EMPTY_STRING,
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    private fun handleReleaseEvent(event: ReleaseEvent) {
        when (event) {
            is ReleaseEvent.NewReleaseEvent -> {
                UpdateDialog(this@MainFrame, event.release) {
                    mainViewModel.startDownloadRelease(event.release)
                }.show()
            }

            is ReleaseEvent.ErrorEvent -> {
                val errorMsg = event.error?.message ?: STRINGS.ui.unknownError
                JOptionPane.showMessageDialog(
                    this@MainFrame,
                    STRINGS.ui.checkUpdateFailedMessage.get(errorMsg),
                    STRINGS.ui.checkUpdateTitle,
                    JOptionPane.ERROR_MESSAGE
                )
            }

            is ReleaseEvent.NoNewReleaseEvent -> {
                JOptionPane.showMessageDialog(
                    this@MainFrame,
                    STRINGS.ui.noUpdateAvailable,
                    STRINGS.ui.checkUpdateTitle,
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }
    }

    private fun handleFileSaveEvent(fileSaveEvent: FileSaveEvent) {
        when (fileSaveEvent) {
            is FileSaveEvent.FileSaveSuccess -> {
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

            is FileSaveEvent.FileSaveError -> {
                val errorMsg = fileSaveEvent.error.message ?: STRINGS.ui.unknownError
                JOptionPane.showMessageDialog(
                    this@MainFrame,
                    fileSaveEvent.message.get(errorMsg),
                    fileSaveEvent.title,
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun observeGlobalMessage() {
        scope.launch {
            GlobalMessageManager.collect {
                fun getMessageBoxType(message: Message): Int {
                    return when (message) {
                        is Message.Info -> JOptionPane.INFORMATION_MESSAGE
                        is Message.Error -> JOptionPane.ERROR_MESSAGE
                        is Message.Warning -> JOptionPane.WARNING_MESSAGE
                        is Message.Empty -> JOptionPane.INFORMATION_MESSAGE
                    }
                }

                JOptionPane.showMessageDialog(
                    this@MainFrame,
                    it.message,
                    EMPTY_STRING,
                    getMessageBoxType(it)
                )
            }
        }
    }

    private fun observeGlobalEvent() {
        scope.launch {
            GlobalEventManager.collect {
                when (it) {
                    is OpenAdbPathSettingsEvent -> {
                        val dialog = GThemeSettingsDialog(this@MainFrame, GROUP_INDEX_ADB)
                        dialog.setLocationRelativeTo(this@MainFrame)
                        dialog.isVisible = true
                    }
                }

            }
        }
    }

    private fun configureWindow() {
        iconImages = appIcons

        currentSettings.windowSettings.loadWindowSettings(this, Frame.MAXIMIZED_BOTH)
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
        ServiceManager.dispose(this)
        ServiceManager.dispose(Context.process)
        saveSettings()
        dispose()
    }

    private fun saveSettings() {
        SettingsManager.updateSettings {
            windowSettings.saveWindowSettings(this@MainFrame)
        }
    }

    companion object {
        private const val TAG = "MainFrame"
    }
}



