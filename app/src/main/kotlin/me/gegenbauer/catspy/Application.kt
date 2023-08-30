package me.gegenbauer.catspy

import com.github.weisj.darklaf.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.cache.PatternProvider
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.LogColorScheme
import me.gegenbauer.catspy.configuration.VStatusPanelTheme
import me.gegenbauer.catspy.configuration.GlobalConfSync
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.platform.isInDebugMode
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.app
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.utils.toArgb
import org.jetbrains.skia.Color
import java.awt.Container
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.JComponent
import javax.swing.UIDefaults
import kotlin.system.exitProcess

object Application {
    private const val TAG = "Main"

    init {
        Color.BLACK.toArgb()
        if (isInDebugMode()) {
            // ubuntu 调试会拦截所有事件导致无法操作
            System.setProperty("Dsun.awt.disablegrab", "true")
        }
        // 启用系统抗锯齿，极大提升字体渲染速度
        System.setProperty("awt.useSystemAAFontSettings", "on")
        System.setProperty("swing.aatext", "true")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        AppScope.launch(Dispatchers.UI) {
            withContext(Dispatchers.APP_LAUNCH) {
                GLog.init(filesDir, "glog.txt")
                ThemeManager.init()
                GlobalConfSync.init()
                GLog.i(TAG, "[currentPlatform] $currentPlatform")
                registerGlobalService()
            }
            ThemeManager.registerDefaultsAdjustmentTask(::adjustAfterThemeLoaded)
            ThemeManager.registerInitTask(::adjustBeforeThemeLoaded)
            ThemeManager.installTheme()
            val mainFrame = MainFrame(STRINGS.ui.app, Contexts())
            mainFrame.configureContext(mainFrame)
            ThemeManager.applyTempTheme()
            mainFrame.isVisible = true
            ThemeManager.registerDefaultThemeUpdateListener()

            addClickListenerForAllComponents(mainFrame.components)
            mainFrame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    GLog.i(TAG, "[windowClosing]")
                    AppScope.launch(Dispatchers.GIO) {
                        GLog.i(TAG, "[windowClosing] handle dispose start")
                        mainFrame.destroy()
                        GLog.i(TAG, "[windowClosing] handle dispose end")
                        exitProcess(0)
                    }
                }
            })
        }
    }

    private fun registerGlobalService() {
        ServiceManager.registerContextService(AdamDeviceManager::class.java)
        ServiceManager.registerContextService(PatternProvider::class.java)
    }

    private val themeAwareControllers = listOf(
        VStatusPanelTheme,
        LogColorScheme,
    )

    private fun adjustBeforeThemeLoaded(theme: Theme, defaults: UIDefaults) {
        themeAwareControllers.forEach { it.onThemeChanged(theme, defaults) }
    }

    private fun adjustAfterThemeLoaded(theme: Theme, properties: Properties) {
        themeAwareControllers.forEach { it.onThemeChanged(theme, properties) }
    }

    private fun addClickListenerForAllComponents(components: Array<java.awt.Component>) {
        components.forEach { component ->
            component.addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                    GLog.d(TAG, "${component.javaClass.name} ${if (component is JComponent) component.componentName else ""} clicked")
                }
            })
            if (component is Container) {
                addClickListenerForAllComponents(component.components)
            }
        }
    }
}