package me.gegenbauer.logviewer

import com.github.weisj.darklaf.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.logviewer.concurrency.APP_LAUNCH
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.configuration.ThemeManager
import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.resource.strings.app
import me.gegenbauer.logviewer.ui.ColorScheme
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.VStatusPanel
import me.gegenbauer.logviewer.viewmodel.GlobalPropertySynchronizer
import java.awt.Container
import java.util.*
import javax.swing.UIDefaults

class Main {
    companion object {
        private const val TAG = "Main"

        @JvmStatic
        fun main(args: Array<String>) {
            AppScope.launch(Dispatchers.UI) {
                withContext(Dispatchers.APP_LAUNCH) {
                    GLog.DEBUG = UIConfManager.uiConf.debug
                    ThemeManager.init()
                    GlobalPropertySynchronizer.init()
                }
                ThemeManager.registerDefaultsAdjustmentTask(::adjustAfterThemeLoaded)
                ThemeManager.registerInitTask(::adjustBeforeThemeLoaded)
                ThemeManager.installTheme()
                val mainUI = MainUI(STRINGS.ui.app)
                ThemeManager.registerThemeUpdateListener { _->
                    mainUI.registerComboBoxEditorEvent()
                }
                ThemeManager.applyTempTheme()
                mainUI.isVisible = true
                ThemeManager.registerDefaultThemeUpdateListener()

                addClickListenerForAllComponents(mainUI.components)
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
        }

        private fun addClickListenerForAllComponents(components: Array<java.awt.Component>) {
            components.forEach { component ->
                component.addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                        GLog.d(TAG, component.javaClass.name)
                    }
                })
                if (component is Container) {
                    addClickListenerForAllComponents(component.components)
                }
            }
        }
    }
}