package me.gegenbauer.catspy

import com.github.weisj.darklaf.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.configuration.toFont
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.ui.ColorScheme
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.VStatusPanel
import me.gegenbauer.catspy.utils.currentPlatform
import me.gegenbauer.catspy.utils.filesDir
import me.gegenbauer.catspy.utils.isInDebugMode
import me.gegenbauer.catspy.viewmodel.GlobalViewModel
import me.gegenbauer.catspy.viewmodel.MainViewModel
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
                }
                ThemeManager.registerDefaultsAdjustmentTask(::adjustAfterThemeLoaded)
                ThemeManager.registerInitTask(::adjustBeforeThemeLoaded)
                ThemeManager.installTheme()
                val mainUI = MainUI(STRINGS.ui.app)
                ThemeManager.registerThemeUpdateListener { _ ->
                    mainUI.registerComboBoxEditorEvent()
                }
                ThemeManager.applyTempTheme()
                mainUI.isVisible = true
                ThemeManager.registerDefaultThemeUpdateListener()

                //addClickListenerForAllComponents(mainUI.components)
            }
        }

        private val themeAwareControllers = listOf(
            VStatusPanel,
            ColorScheme
        )

        private fun adjustBeforeThemeLoaded(theme: Theme, defaults: UIDefaults) {
            themeAwareControllers.forEach { it.onThemeChanged(theme, defaults) }
        }

        private fun adjustAfterThemeLoaded(theme: Theme, properties: Properties) {
            themeAwareControllers.forEach { it.onThemeChanged(theme, properties) }
            MainViewModel.logFont.updateValue(theme.toFont())
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