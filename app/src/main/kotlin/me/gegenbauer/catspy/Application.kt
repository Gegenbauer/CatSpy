package me.gegenbauer.catspy

import com.github.weisj.darklaf.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.cache.PatternProvider
import me.gegenbauer.catspy.common.configuration.ThemeManager
import me.gegenbauer.catspy.common.support.ColorScheme
import me.gegenbauer.catspy.common.support.VStatusPanelTheme
import me.gegenbauer.catspy.common.viewmodel.GlobalViewModel
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.utils.currentPlatform
import me.gegenbauer.catspy.utils.filesDir
import me.gegenbauer.catspy.utils.isInDebugMode
import java.awt.Container
import java.util.*
import javax.swing.JComponent
import javax.swing.UIDefaults

class Application {
    companion object {
        private const val TAG = "Main"

        init {
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
                    GlobalViewModel.init()
                    GLog.i(TAG, "[currentPlatform] $currentPlatform")
                    registerGlobalService()
                }
                ThemeManager.registerDefaultsAdjustmentTask(::adjustAfterThemeLoaded)
                ThemeManager.registerInitTask(::adjustBeforeThemeLoaded)
                ThemeManager.installTheme()
                val mainUI = MainUI(STRINGS.ui.app, Contexts())
                mainUI.configureContext(mainUI)
                ThemeManager.applyTempTheme()
                mainUI.isVisible = true
                ThemeManager.registerDefaultThemeUpdateListener()

                //addClickListenerForAllComponents(mainUI.components)
                mainUI.addWindowListener(object : java.awt.event.WindowAdapter() {
                    override fun windowClosing(e: java.awt.event.WindowEvent?) {
                        GLog.i(TAG, "[windowClosing]")
                        AppScope.launch(Dispatchers.GIO) {
                            GLog.i(TAG, "[windowClosing] handle dispose start")
                            mainUI.dispose()
                            GLog.i(TAG, "[windowClosing] handle dispose end")
                            System.exit(0)
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
            ColorScheme,
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
                        GLog.d(
                            TAG,
                            "${component.javaClass.name} ${if (component is JComponent) component.componentName else ""} clicked"
                        )
                    }
                })
                if (component is Container) {
                    addClickListenerForAllComponents(component.components)
                }
            }
        }
    }
}