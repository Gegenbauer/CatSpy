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
import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.resource.strings.app
import me.gegenbauer.logviewer.ui.MainUI
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
                    loadConfig()
                    GlobalPropertySynchronizer.init()
                }
                ThemeManager.installTheme()
                ThemeManager.registerDefaultsAdjustmentTask(::adjustAfterThemeLoaded)
                ThemeManager.registerInitTask(::adjustBeforeThemeLoaded)
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

        private fun loadConfig() {
            ConfigManager.loadConfig()
            ColorManager.fullTableColor.getConfig()
            ColorManager.fullTableColor.applyColor()
            ColorManager.filterTableColor.getConfig()
            ColorManager.filterTableColor.applyColor()
            ColorManager.getConfigFilterStyle()
            ConfigManager.saveConfig()
        }

        private fun adjustBeforeThemeLoaded(theme: Theme, defaults: UIDefaults) {
            defaults["JButton.contentAreaFilled"] = true
        }

        private fun adjustAfterThemeLoaded(theme: Theme, properties: Properties) {
            properties["JButton.contentAreaFilled"] = true
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