package me.gegenbauer.catspy.ui.dialog

import me.gegenbauer.catspy.configuration.*
import me.gegenbauer.catspy.file.gson
import me.gegenbauer.catspy.java.ext.copyFields
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.globalLocale
import me.gegenbauer.catspy.strings.supportLocales
import me.gegenbauer.catspy.utils.installKeyStrokeEscClosing
import say.swing.JFontChooser
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * 从上至下添加设置项
 */
class GThemeSettingsDialog(parent: Window) : JDialog(parent, ModalityType.MODELESS) {

    private var tree = SettingsTree()

    private val startSettings = SettingsManager.string

    init {
        title = STRINGS.ui.preferences

        initUI()

        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        modalityType = ModalityType.APPLICATION_MODAL
        pack()

        setLocationRelativeTo(null)
    }

    private fun initUI() {
        val wrapGroupPanel = JPanel(BorderLayout(10, 10))
        val groups = ArrayList<ISettingsGroup>()
        groups.add(makeAppearanceGroup())

        tree.init(wrapGroupPanel, groups)

        val rightPane = JScrollPane(wrapGroupPanel)
        rightPane.verticalScrollBar.unitIncrement = 16
        rightPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        rightPane.border = BorderFactory.createEmptyBorder(10, 3, 3, 10)

        val leftPane = JScrollPane(tree)
        leftPane.border = BorderFactory.createEmptyBorder(10, 10, 3, 3)

        val splitPane = JSplitPane()
        splitPane.resizeWeight = 0.2
        splitPane.leftComponent = leftPane
        splitPane.rightComponent = rightPane

        contentPane = JPanel(BorderLayout())
        contentPane.add(splitPane, BorderLayout.CENTER)
        contentPane.add(buildButtonsPane(), BorderLayout.PAGE_END)

        installKeyStrokeEscClosing(this)
    }

    private fun makeAppearanceGroup(): SettingsGroup {
        val languageCbx = JComboBox(supportLocales.map { it.displayName }.toTypedArray())
        languageCbx.addActionListener {
            val locale = supportLocales[languageCbx.selectedIndex]
            if (locale == globalLocale) return@addActionListener
            SettingsManager.updateSettings { this.locale = locale.ordinal }
        }
        languageCbx.selectedItem = globalLocale.displayName

        val lafCbx = JComboBox(ThemeManager.getThemes())
        lafCbx.selectedItem = ThemeManager.currentTheme.name
        lafCbx.addActionListener {
            SettingsManager.updateSettings { theme = lafCbx.selectedItem as String }
        }

        val changeFontBtn = JButton(STRINGS.ui.change)

        val group = SettingsGroup(STRINGS.ui.appearance)
        group.addRow(STRINGS.ui.language, languageCbx)
        group.addRow(STRINGS.ui.menuTheme, lafCbx)
        val fontLabel = group.addRow(getFontLabelStr(), changeFontBtn)
        group.end()

        changeFontBtn.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val fontChooser = JFontChooser()
                fontChooser.selectedFont = SettingsManager.settings.font
                val result: Int = fontChooser.showDialog(this@GThemeSettingsDialog)
                if (result == JFontChooser.OK_OPTION) {
                    val font: Font = fontChooser.selectedFont
                    SettingsManager.settings.font = font
                    updateUIWithAnim { FontSupport.setUIFont(font) }
                    fontLabel.text = getFontLabelStr()
                }
            }
        })

        return group
    }

    private fun getFontLabelStr(): String {
        val font = SettingsManager.settings.font
        val fontStyleName = FontSupport.convertFontStyleToString(font.style)
        return "${STRINGS.ui.font}: ${font.fontName} $fontStyleName ${font.size}"
    }

    private fun buildButtonsPane(): JPanel {
        val saveBtn = JButton(STRINGS.ui.save)
        saveBtn.addActionListener { save() }

        val cancelButton = JButton(STRINGS.ui.cancel)
        cancelButton.addActionListener { cancel() }

        val resetBtn = JButton(STRINGS.ui.reset)
        resetBtn.addActionListener { reset() }

        val copyBtn = JButton(STRINGS.ui.copyToClipboard)
        copyBtn.addActionListener { copySettings() }

        val buttonPane = JPanel()
        buttonPane.layout = BoxLayout(buttonPane, BoxLayout.LINE_AXIS)
        buttonPane.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        buttonPane.add(resetBtn)
        buttonPane.add(copyBtn)
        buttonPane.add(Box.createHorizontalGlue())
        buttonPane.add(saveBtn)
        buttonPane.add(Box.createRigidArea(Dimension(10, 0)))
        buttonPane.add(cancelButton)

        getRootPane().defaultButton = saveBtn
        return buttonPane
    }

    private fun save() {
        dispose()
        SettingsManager.checkAndUpdateLocale(startSettings)
    }

    private fun reset() {
        SettingsManager.updateSettings {
            val originalSettings = gson.fromJson(startSettings, GSettings::class.java)
            copyFields(originalSettings, SettingsManager.settings)
        }
    }

    private fun cancel() {
        reset()
        dispose()
    }

    private fun copySettings() {
        val settings = SettingsManager.settings
        val json = gson.toJson(settings)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(json), null)
    }
}