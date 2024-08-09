package me.gegenbauer.catspy.ui.dialog

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.icons.FlatAbstractIcon
import com.formdev.flatlaf.util.ColorFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.configuration.*
import me.gegenbauer.catspy.view.panel.VerticalFlexibleWidthLayout
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.globalLocale
import me.gegenbauer.catspy.strings.supportLocales
import say.swing.JFontChooser
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class AppearanceSettingsGroup(
    scope: CoroutineScope,
    container: SettingsContainer
) : BaseSettingsGroup(STRINGS.ui.appearance, scope, container) {

    override fun initGroup() {
        val languageModifiedHint = JLabel("")
        languageModifiedHint.foreground = UIManager.getColor("CatSpy.accent.red")
        val languageCbx = JComboBox(supportLocales.map { it.displayName }.toTypedArray())
        languageCbx.addActionListener {
            val locale = supportLocales[languageCbx.selectedIndex]
            SettingsManager.updateSettings { this.mainUISettings.locale = locale.ordinal }
            if (locale == globalLocale) {
                languageModifiedHint.text = ""
                return@addActionListener
            }
            languageModifiedHint.text = STRINGS.ui.languageSettingHint
        }
        languageCbx.selectedItem = SettingsManager.settings.mainUISettings.locale
            .let { if (it < 0 || it >= supportLocales.size) globalLocale.ordinal else it }
            .let { supportLocales[it].displayName }

        val lafCbx = JComboBox(ThemeManager.getThemes())
        lafCbx.selectedItem = ThemeManager.currentTheme.name
        lafCbx.addActionListener {
            scope.launch {
                SettingsManager.suspendedUpdateSettings {
                    themeSettings.theme = lafCbx.selectedItem as String
                }
                container.reloadUI()
            }
        }
        val languageEditPanel = JPanel()
        languageEditPanel.layout = VerticalFlexibleWidthLayout()
        languageEditPanel.add(languageCbx)
        languageEditPanel.add(languageModifiedHint)

        val changeUIFontBtn = JButton(STRINGS.ui.change)
        val changeLogFontBtn = JButton(STRINGS.ui.change)

        addRow(STRINGS.ui.language, languageEditPanel)
        addRow(STRINGS.ui.menuTheme, lafCbx)
        val uiFontLabel = addRow(
            getFontLabelStr(
                STRINGS.ui.uiFont,
                SettingsManager.settings.themeSettings.uiFont.nativeFont
            ), changeUIFontBtn
        )
        val logFontLabel = addRow(
            getFontLabelStr(
                STRINGS.ui.logFont,
                SettingsManager.settings.logSettings.font.nativeFont
            ), changeLogFontBtn
        )
        if (ThemeManager.isAccentColorSupported) {
            addRow(STRINGS.ui.accentColor, createColorChoosePanel())
        }
        end()

        changeUIFontBtn.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val fontChooser = JFontChooser()
                fontChooser.selectedFont = currentSettings.themeSettings.uiFont.nativeFont
                val result: Int = fontChooser.showDialog(container as Component)
                if (result == JFontChooser.OK_OPTION) {
                    val font: Font = fontChooser.selectedFont
                    currentSettings.themeSettings.uiFont.update(font)
                    updateUIWithAnim { FontSupport.setUIFont(font) }
                    uiFontLabel.text = getUIFontLabelStr()
                }
            }
        })
        changeLogFontBtn.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val fontChooser = JFontChooser()
                fontChooser.selectedFont = currentSettings.logSettings.font.nativeFont
                val result: Int = fontChooser.showDialog(container as Component)
                if (result == JFontChooser.OK_OPTION) {
                    val font: Font = fontChooser.selectedFont
                    currentSettings.logSettings.font.update(font)
                    updateUIWithAnim { container.reloadUI() }
                    logFontLabel.text = getFontLabelStr(STRINGS.ui.logFont, font)
                }
            }
        })
    }

    private fun createColorChoosePanel(): JToolBar {
        return JToolBar().apply {
            val group = ButtonGroup()
            val colors = accentColors.map { UIManager.getColor(it.first) ?: Color.lightGray }
            accentColors.forEachIndexed { index, color ->
                val colorButton = JToggleButton(AccentColorIcon(color.first))
                colorButton.isSelected = currentSettings.themeSettings.getAccentColor() == colors[index]
                colorButton.toolTipText = color.second
                colorButton.addActionListener {
                    scope.launch {
                        SettingsManager.suspendedUpdateSettings {
                            themeSettings.setAccentColor(UIManager.getColor(color.first) ?: Color.lightGray)
                        }
                    }
                }
                add(colorButton)
                group.add(colorButton)
            }
        }
    }

    private class AccentColorIcon(private val colorKey: String) : FlatAbstractIcon(16, 16, null) {
        override fun paintIcon(c: Component, g: Graphics2D) {
            var color = UIManager.getColor(colorKey)
            if (color == null) color = Color.lightGray
            else if (!c.isEnabled) {
                color = if (FlatLaf.isLafDark()
                ) ColorFunctions.shade(color, 0.5f)
                else ColorFunctions.tint(color, 0.6f)
            }

            g.color = color
            g.fillRoundRect(1, 1, width - 2, height - 2, 5, 5)
        }
    }

    private fun getFontLabelStr(label: String, font: Font): String {
        val fontStyleName = FontSupport.convertFontStyleToString(font.style)
        return "$label: ${font.fontName} $fontStyleName ${font.size}"
    }

    private fun getUIFontLabelStr(): String {
        val font = currentSettings.themeSettings.uiFont.nativeFont
        val fontStyleName = FontSupport.convertFontStyleToString(font.style)
        return "${STRINGS.ui.uiFont}: ${font.fontName} $fontStyleName ${font.size}"
    }

    companion object {
        private val accentColors = listOf(
            "CatSpy.accent.default" to "Default", "CatSpy.accent.blue" to "Blue",
            "CatSpy.accent.purple" to "Purple", "CatSpy.accent.red" to "Red", "CatSpy.accent.orange" to "Orange",
            "CatSpy.accent.yellow" to "Yellow", "CatSpy.accent.green" to "Green"
        )
    }
}