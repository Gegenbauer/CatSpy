package me.gegenbauer.logviewer.ui.dialog

import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.button.GButton
import me.gegenbauer.logviewer.utils.Utils
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowEvent
import javax.swing.*


class AppearanceDialog(private val parent: MainUI) : JDialog(parent, STRINGS.ui.appearance, true), ActionListener {
    private var fontSlider: JSlider
    private var dividerSlider: JSlider
    private var laFGroup: ButtonGroup
    private var exampleLabel: JLabel
    private var baseFontSize = 0

    private var okBtn: JButton = GButton(STRINGS.ui.ok)
    private var cancelBtn: JButton
    private val freDividerSize = parent.splitLogPane.dividerSize

    init {
        okBtn.addActionListener(this)
        cancelBtn = GButton(STRINGS.ui.cancel)
        cancelBtn.addActionListener(this)

        val lafPanel = JPanel()
        laFGroup = ButtonGroup()

        var lafItem = JRadioButton(MainUI.CROSS_PLATFORM_LAF)
        laFGroup.add(lafItem)
        lafPanel.add(lafItem)

        lafItem = JRadioButton(MainUI.SYSTEM_LAF)
        laFGroup.add(lafItem)
        lafPanel.add(lafItem)

        lafItem = JRadioButton(MainUI.FLAT_LIGHT_LAF)
        laFGroup.add(lafItem)
        lafPanel.add(lafItem)

        lafItem = JRadioButton(MainUI.FLAT_DARK_LAF)
        laFGroup.add(lafItem)
        lafPanel.add(lafItem)

        for (item in laFGroup.elements) {
            if (ConfigManager.LaF == item.text) {
                item.isSelected = true
                break
            }
        }

        exampleLabel = JLabel(EXAMPLE_TEXT)
        exampleLabel.preferredSize = Dimension(350, 50)

        val sliderPanel = JPanel()
        val sliderLabel = JLabel("UI Size(%)")
        sliderPanel.add(sliderLabel)
        baseFontSize = exampleLabel.font.size * 100 / parent.uiFontPercent
        fontSlider = JSlider(MIN_FONT_POS, MAX_FONT_POS, parent.uiFontPercent)
        fontSlider.majorTickSpacing = 50
        fontSlider.minorTickSpacing = 10
        fontSlider.paintTicks = true
        fontSlider.paintLabels = true
        fontSlider.addChangeListener {
            exampleLabel.text = "${fontSlider.value} % : $EXAMPLE_TEXT"
            exampleLabel.font =
                Font(exampleLabel.font.name, exampleLabel.font.style, baseFontSize * fontSlider.value / 100)
        }
        sliderPanel.add(fontSlider)

        val sizePanel = JPanel()
        sizePanel.layout = BoxLayout(sizePanel, BoxLayout.Y_AXIS)
        sizePanel.add(exampleLabel)
        sizePanel.add(sliderPanel)

        val lafSizePanel = JPanel(BorderLayout())
        lafSizePanel.border = BorderFactory.createTitledBorder(STRINGS.ui.lookAndFeel)
        lafSizePanel.add(lafPanel, BorderLayout.NORTH)
        lafSizePanel.add(sizePanel, BorderLayout.CENTER)

        val dividerPanel = JPanel()
        val dividerLabel = JLabel("Divider Size(1 ~ 20) [${parent.splitLogPane.dividerSize}]")
        dividerPanel.add(dividerLabel)
        dividerSlider = JSlider(0, MAX_DIVIDER_POS, parent.splitLogPane.dividerSize)
        dividerSlider.majorTickSpacing = 5
        dividerSlider.minorTickSpacing = 1
        dividerSlider.paintTicks = true
        dividerSlider.paintLabels = true
        dividerSlider.addChangeListener {
            if (dividerSlider.value == 0) {
                dividerSlider.value = MIN_DIVIDER_POS
            }
            parent.splitLogPane.dividerSize = dividerSlider.value
            dividerLabel.text = "Divider Size(1 ~ 20) [${parent.splitLogPane.dividerSize}]"
        }
        dividerPanel.add(dividerSlider)

        val optionsPanel = JPanel(BorderLayout())
        optionsPanel.border = BorderFactory.createTitledBorder(STRINGS.ui.options)
        optionsPanel.add(dividerPanel, BorderLayout.CENTER)

        val confirmPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        confirmPanel.preferredSize = Dimension(350, 40)
        confirmPanel.alignmentX = JPanel.RIGHT_ALIGNMENT
        confirmPanel.add(okBtn)
        confirmPanel.add(cancelBtn)

        val dialogPanel = JPanel(BorderLayout())
        dialogPanel.add(lafSizePanel, BorderLayout.NORTH)
        dialogPanel.add(optionsPanel, BorderLayout.CENTER)
        dialogPanel.add(confirmPanel, BorderLayout.SOUTH)
        val panel = JPanel(BorderLayout())
        panel.add(dialogPanel, BorderLayout.CENTER)

        contentPane.add(panel)

        pack()
        Utils.installKeyStrokeEscClosing(this)
    }

    override fun actionPerformed(e: ActionEvent) {
        if (e.source == okBtn) {
            saveConfiguration(laFGroup, parent, fontSlider)
            this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        } else if (e.source == cancelBtn) {
            parent.splitLogPane.dividerSize = freDividerSize
            this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        }
    }

    companion object {
        private const val MIN_FONT_POS = 50
        private const val MAX_FONT_POS = 200
        private const val EXAMPLE_TEXT = "To apply \"Changes\" need to restart"
        private const val MIN_DIVIDER_POS = 1
        private const val MAX_DIVIDER_POS = 20

        fun saveConfiguration(laFGroup: ButtonGroup, parent: MainUI, fontSlider: JSlider) {
            for (item in laFGroup.elements) {
                if (item.isSelected) {
                    UIConfManager.uiConf.laf = item.text
                    UIConfManager.uiConf.uiFontScale = fontSlider.value
                    UIConfManager.uiConf.dividerSize = parent.splitLogPane.dividerSize
                    break
                }
            }
        }
    }
}

