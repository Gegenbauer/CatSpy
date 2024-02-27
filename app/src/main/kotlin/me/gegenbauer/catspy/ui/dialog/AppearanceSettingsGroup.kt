package me.gegenbauer.catspy.ui.dialog

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.icons.FlatAbstractIcon
import com.formdev.flatlaf.util.ColorFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.configuration.*
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
        val languageCbx = JComboBox(supportLocales.map { it.displayName }.toTypedArray())
        languageCbx.addActionListener {
            val locale = supportLocales[languageCbx.selectedIndex]
            if (locale == globalLocale) return@addActionListener
            SettingsManager.updateSettings { this.mainUISettings.locale = locale.ordinal }
        }
        languageCbx.selectedItem = globalLocale.displayName

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

        val changeFontBtn = JButton(STRINGS.ui.change)

        addRow(STRINGS.ui.language, languageCbx)
        addRow(STRINGS.ui.menuTheme, lafCbx)
        val fontLabel = addRow(getFontLabelStr(), changeFontBtn)
        if (ThemeManager.isAccentColorSupported) {
            addRow(STRINGS.ui.accentColor, createColorChoosePanel())
        }
        end()

        changeFontBtn.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val fontChooser = JFontChooser()
                fontChooser.selectedFont = currentSettings.themeSettings.font.toNativeFont()
                val result: Int = fontChooser.showDialog(container as Component)
                if (result == JFontChooser.OK_OPTION) {
                    val font: Font = fontChooser.selectedFont
                    currentSettings.themeSettings.font.update(font)
                    updateUIWithAnim { FontSupport.setUIFont(font) }
                    fontLabel.text = getFontLabelStr()
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

    private fun getFontLabelStr(): String {
        val font = currentSettings.themeSettings.font.toNativeFont()
        val fontStyleName = FontSupport.convertFontStyleToString(font.style)
        return "${STRINGS.ui.font}: ${font.fontName} $fontStyleName ${font.size}"
    }

    companion object {
        private val accentColors = listOf(
            "CatSpy.accent.default" to "Default", "CatSpy.accent.blue" to "Blue",
            "CatSpy.accent.purple" to "Purple", "CatSpy.accent.red" to "Red", "CatSpy.accent.orange" to "Orange",
            "CatSpy.accent.yellow" to "Yellow", "CatSpy.accent.green" to "Green"
        )
    }
}