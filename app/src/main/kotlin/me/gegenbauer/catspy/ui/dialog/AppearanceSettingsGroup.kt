package me.gegenbauer.catspy.ui.dialog

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.icons.FlatAbstractIcon
import com.formdev.flatlaf.util.ColorFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.configuration.FontSupport
import me.gegenbauer.catspy.configuration.GSettings
import me.gegenbauer.catspy.configuration.SettingsContainer
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.configuration.updateUIWithAnim
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.ui.customize.CenteredDualDirectionPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.globalLocale
import me.gegenbauer.catspy.strings.supportLocales
import me.gegenbauer.catspy.utils.ui.setWidth
import me.gegenbauer.catspy.view.panel.VerticalFlexibleWidthLayout
import me.gegenbauer.catspy.view.textpane.HintTextPane
import say.swing.JFontChooser
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JToggleButton
import javax.swing.JToolBar
import javax.swing.UIManager

class AppearanceSettingsGroup(
    scope: CoroutineScope,
    container: SettingsContainer
) : BaseSettingsGroup(STRINGS.ui.appearance, scope, container) {

    override fun initGroup() {
        val languageModifiedHint = HintTextPane()
        languageModifiedHint.text = EMPTY_STRING
        languageModifiedHint.foreground = UIManager.getColor("CatSpy.accent.red")
        val languageCbx = JComboBox(supportLocales.map { it.displayName }.toTypedArray())
        languageCbx.addActionListener {
            val locale = supportLocales[languageCbx.selectedIndex]
            SettingsManager.updateSettings { this.mainUISettings.locale = locale.ordinal }
            if (locale == globalLocale) {
                languageModifiedHint.text = EMPTY_STRING
                languageModifiedHint.parent?.revalidate()
                languageModifiedHint.parent?.repaint()
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

        addRow(STRINGS.ui.language, createSingleComponentRow(languageEditPanel))
        addRow(STRINGS.ui.menuTheme, createSingleComponentRow(lafCbx))
        val uiFontLabel = addRow(
            getFontLabelStr(
                STRINGS.ui.uiFont,
                SettingsManager.settings.themeSettings.uiFont.nativeFont
            ), createSingleComponentRow(changeUIFontBtn)
        )
        val logFontLabel = addRow(
            getFontLabelStr(
                STRINGS.ui.logFont,
                SettingsManager.settings.logSettings.font.nativeFont
            ), createSingleComponentRow(changeLogFontBtn)
        )
        if (ThemeManager.isAccentColorSupported) {
            addRow(STRINGS.ui.accentColor, createSingleComponentRow(createColorChoosePanel()))
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

    private fun createSingleComponentRow(component: JComponent): JPanel {
        val panel = CenteredDualDirectionPanel()
        component.maximumSize = Dimension(250, Int.MAX_VALUE)
        component.setWidth(300)
        panel.addRight(component)
        return panel
    }

    private fun createColorChoosePanel(): JToolBar {
        return JToolBar().apply {
            val group = ButtonGroup()
            val colors = GSettings.Theme.accentColors.map { UIManager.getColor(it.first) ?: Color.lightGray }
            GSettings.Theme.accentColors.forEachIndexed { index, color ->
                val colorButton = JToggleButton(AccentColorIcon(color.first))
                colorButton.isSelected = currentSettings.themeSettings.getAccentColor() == colors[index]
                colorButton.toolTipText = color.second
                colorButton.addActionListener {
                    scope.launch {
                        SettingsManager.suspendedUpdateSettings {
                            themeSettings.setAccentColor(color.second)
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

}