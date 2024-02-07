package me.gegenbauer.catspy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.cache.PatternProvider
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.conf.DebugConfiguration
import me.gegenbauer.catspy.conf.GlobalConfSync
import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.configuration.LogColorScheme
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.ddmlib.adb.AdbConf
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceMonitor
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.GlobalProperties
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.strings.StringResourceManager
import me.gegenbauer.catspy.strings.registerLocaleChangeListener
import me.gegenbauer.catspy.ui.MainFrame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.UIManager
import kotlin.system.exitProcess

object Application : WindowAdapter() {
    private const val TAG = "Application"

    private lateinit var mainFrame: MainFrame

    init {
        Locale.setDefault(Locale.CHINA)
        currentPlatform.configureUIProperties()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        AppScope.launch(Dispatchers.UI) {

            ThemeManager.registerThemeUpdateListener { theme ->
                themeAwareControllers.forEach { it.onThemeChanged(theme, UIManager.getDefaults()) }
            }

            withContext(Dispatchers.APP_LAUNCH) {
                GLog.init(filesDir, GlobalStrings.LOG_NAME)
                SettingsManager.init()
                GlobalConfSync.init()
                DebugConfiguration.apply()
                GLog.i(TAG, "[currentPlatform] $currentPlatform")
                registerGlobalService()
            }
            showMainFrame()

            registerLocaleChangeListener { old, new ->
                GLog.i(TAG, "[registerLocaleChangeListener] $old -> $new")
                StringResourceManager.loadStrings()
                showMainFrame()
            }
        }
    }

    override fun windowClosing(e: WindowEvent?) {
        GLog.i(TAG, "[windowClosing]")
        AppScope.launch(Dispatchers.GIO) {
            GLog.i(TAG, "[windowClosing] handle dispose start")
            mainFrame.destroy()
            AppScope.cancel()
            GLog.flush()
            GLog.i(TAG, "[windowClosing] handle dispose end")
            exitProcess(0)
        }
    }

    private fun showMainFrame() {
        if (::mainFrame.isInitialized) {
            mainFrame.destroy()
        }
        StringResourceManager.loadStrings()
        mainFrame = MainFrame(GlobalProperties.APP_NAME)
        mainFrame.configureContext(mainFrame)
        mainFrame.isVisible = true
        mainFrame.addWindowListener(this@Application)
    }

    private fun registerGlobalService() {
        ServiceManager.registerContextService(PatternProvider::class.java)
        ServiceManager.registerContextService(AdamDeviceMonitor::class.java)

        val adbPath = currentPlatform.adbPath
        GLog.i(TAG, "[registerGlobalService] detected adbPath: $adbPath")
        ServiceManager.getContextService(AdamDeviceMonitor::class.java).configure(AdbConf(currentPlatform.adbPath))
    }

    private val themeAwareControllers = listOf(
        LogColorScheme
    )
}