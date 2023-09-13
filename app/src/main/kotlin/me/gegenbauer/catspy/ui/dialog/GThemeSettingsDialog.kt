package me.gegenbauer.catspy.ui.dialog

import com.github.weisj.darklaf.components.DefaultButton
import com.github.weisj.darklaf.components.DynamicUI
import com.github.weisj.darklaf.properties.icons.IconLoader
import com.github.weisj.darklaf.settings.ThemeSettings
import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.configuration.newFont
import me.gegenbauer.catspy.configuration.toFont
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.listProperty
import me.gegenbauer.catspy.databinding.property.support.selectedIndexProperty
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.globalLocale
import me.gegenbauer.catspy.strings.supportLocales
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.*

/**
 * 从上至下添加设置项
 */
class GThemeSettingsDialog(parent: Window) : JDialog(parent, ModalityType.MODELESS) {
    private val darklafThemeSettingsPanel = ThemeSettings.getInstance().settingsPanel
    private val languageSettingsPanel = LanSettingsPanel()

    init {
        setIconImage(IconLoader.createFrameIcon(ThemeSettings.getIcon(), this))
        setTitle(ThemeSettings.getInstance().title)

        val contentPane = JPanel(BorderLayout())
        val settingsContentPanel = JPanel(BorderLayout())
        settingsContentPanel.layout = BoxLayout(settingsContentPanel, BoxLayout.Y_AXIS)

        settingsContentPanel.add(darklafThemeSettingsPanel)
        // 添加分割线
        settingsContentPanel.add(JPanel().apply {
            border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
        })
        settingsContentPanel.add(languageSettingsPanel)
        // 添加确认按钮
        contentPane.add(settingsContentPanel, BorderLayout.CENTER)
        contentPane.add(createButtonPanel(), BorderLayout.SOUTH)
        setContentPane(contentPane)

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        pack()
    }

    private fun createButtonPanel(): Component {
        val ok: JButton = DynamicUI.withLocalizedText(DefaultButton(""), "settings.dialog_ok")
        ok.setDefaultCapable(true)
        ok.addActionListener {
            UIConfManager.uiConf.locale = languageSettingsPanel.binding.currentSelectIndex.value ?: 0
            ThemeSettings.getInstance().apply()
            dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        }
        val cancel = DynamicUI.withLocalizedText(JButton(), "settings.dialog_cancel")
        cancel.addActionListener {
            ThemeSettings.getInstance().revert()
            dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        }
        val apply = DynamicUI.withLocalizedText(JButton(), "settings.dialog_apply")
        apply.addActionListener { ThemeSettings.getInstance().apply() }
        val box = Box.createHorizontalBox()
        box.add(Box.createHorizontalGlue())
        box.add(ok)
        box.add(cancel)
        box.add(apply)
        box.setBorder(darklafThemeSettingsPanel.border)
        return box
    }

    class LanSettingsPanel : JPanel() {
        val binding = LanBinding()
        private val lanSelectCombo = JComboBox<String>().apply {
            maximumSize = Dimension(400, 30)
        }
        private val hint = JLabel(STRINGS.ui.languageSettingHint).apply {
            font = ThemeSettings.getInstance().theme.toFont().newFont(baseFontSize = 10)
            isEnabled = false
            border = BorderFactory.createEmptyBorder(2, 8, 0, 0)
        }

        init {
            layout = TableLayout(
                doubleArrayOf(TableLayoutConstants.PREFERRED),
                doubleArrayOf(TableLayoutConstants.PREFERRED, TableLayoutConstants.PREFERRED)
            )
            add(lanSelectCombo, "0, 0")
            add(hint, "0, 1")
            border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
            listProperty(lanSelectCombo) bindDual binding.lans
            selectedIndexProperty(lanSelectCombo) bindDual binding.currentSelectIndex

            binding.lans.updateValue(supportLocales.map { it.displayName })
            binding.currentSelectIndex.updateValue(globalLocale.ordinal)
        }

        inner class LanBinding {
            val lans = ObservableViewModelProperty<List<String>>()
            val currentSelectIndex = ObservableViewModelProperty<Int>()
        }
    }
}