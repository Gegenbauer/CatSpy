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
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.ui.ColorScheme
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.VStatusPanel
import me.gegenbauer.catspy.viewmodel.GlobalViewModel
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
                    GlobalViewModel.init()
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