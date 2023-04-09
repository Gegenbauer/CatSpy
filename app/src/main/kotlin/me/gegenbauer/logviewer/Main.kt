package me.gegenbauer.logviewer

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

class Main {
    companion object {
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
                val mainUI = MainUI(STRINGS.ui.app)
                mainUI.updateUIAfterVisible(args)
                ThemeManager.applyTempTheme()
                mainUI.isVisible = true
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
    }
}