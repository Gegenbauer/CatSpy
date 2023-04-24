package me.gegenbauer.logviewer.ui.dialog

import me.gegenbauer.logviewer.databinding.bind.withName
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.addHSeparator
import me.gegenbauer.logviewer.ui.combobox.FilterComboBox
import me.gegenbauer.logviewer.ui.combobox.getFilterComboBox
import me.gegenbauer.logviewer.utils.Utils
import me.gegenbauer.logviewer.utils.loadIcon
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.plaf.basic.BasicScrollBarUI

class AppearanceSettingsDialog (private var mainUI: MainUI) : JDialog(mainUI, STRINGS.ui.appearance, true), ActionListener {
    private val settingsPanel = JPanel()
    private val scrollPane = JScrollPane()
    private val lnFPanel = LnFPanel()
    private val filterComboPanel = FilterComboPanel()
    private val fontColorPanel = FontColorPanel()

    private val okBtn = JButton(STRINGS.ui.ok)
    private val cancelBtn = JButton(STRINGS.ui.cancel)

    init {
        addWindowListener(filterComboPanel)
        addWindowListener(fontColorPanel)
        scrollPane.verticalScrollBar.unitIncrement = 10
        settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        addHSeparator(settingsPanel, " ${STRINGS.ui.lookAndFeel}, ${STRINGS.ui.options} ")
        settingsPanel.add(lnFPanel)
        addHEmptySeparator(settingsPanel, 20)
        addHSeparator(settingsPanel, " ${STRINGS.ui.filterStyle} ")
        settingsPanel.add(filterComboPanel)
        addHEmptySeparator(settingsPanel, 20)
        addHSeparator(settingsPanel, " ${STRINGS.ui.log} ${STRINGS.ui.font} & ${STRINGS.ui.color} ")
        settingsPanel.add(fontColorPanel)

        okBtn.addActionListener(this)
        cancelBtn.addActionListener(this)
        val bottomPanel = JPanel()
        bottomPanel.add(okBtn)
        bottomPanel.add(cancelBtn)

        val settingsPanelWrapper = JPanel(BorderLayout())
        settingsPanelWrapper.add(settingsPanel, BorderLayout.NORTH)
        scrollPane.setViewportView(settingsPanelWrapper)

        contentPane.layout = BorderLayout()
        contentPane.add(scrollPane, BorderLayout.CENTER)
        contentPane.add(bottomPanel, BorderLayout.SOUTH)

        preferredSize = Dimension(940, 900)
        minimumSize = Dimension(940, 800)

        pack()

        Utils.installKeyStrokeEscClosing(this)
    }

    private fun addHEmptySeparator(target:JPanel, height: Int) {
        val panel = JPanel()
        panel.preferredSize = Dimension(1, height)
        target.add(panel)
    }

    override fun actionPerformed(e: ActionEvent) {
        if (e.source == okBtn) {
            lnFPanel.actionBtn(true)
            filterComboPanel.actionBtn(true)
            fontColorPanel.actionBtn(true)
            this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        } else if (e.source == cancelBtn) {
            lnFPanel.actionBtn(false)
            filterComboPanel.actionBtn(false)
            fontColorPanel.actionBtn(false)
            this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        }
    }

    inner class LnFPanel : JPanel() {
        private var fontSlider: JSlider
        private var dividerSlider: JSlider
        private var laFGroup: ButtonGroup
        private var exampleLabel: JLabel
        private var baseFontSize = 0
        private val prevDividerSize = mainUI.splitLogPane.dividerSize

        init {
            layout = FlowLayout(FlowLayout.LEFT)

            val lafPanel = JPanel(FlowLayout(FlowLayout.LEFT))
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
            lafPanel.add(ImagePanel(loadIcon("appearance_flat_light.png")))

            lafItem = JRadioButton(MainUI.FLAT_DARK_LAF)
            laFGroup.add(lafItem)
            lafPanel.add(lafItem)
            lafPanel.add(ImagePanel(loadIcon("appearance_flat_dark.png")))

            lafPanel.add(JLabel("   (Restart)"))

            for (item in laFGroup.elements) {
                if (ConfigManager.LaF == item.text) {
                    item.isSelected = true
                    break
                }
            }

            val examplePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            exampleLabel = JLabel(EXAMPLE_TEXT)
            examplePanel.preferredSize = Dimension(exampleLabel.preferredSize.width, 50)
            examplePanel.add(exampleLabel)
            examplePanel.border = BorderFactory.createLineBorder(Color.GRAY)

            val sliderPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val sliderLabel = JLabel("UI Size(%, Restart)")
            sliderPanel.add(sliderLabel)
            baseFontSize = exampleLabel.font.size * 100 / mainUI.uiFontPercent
            fontSlider = JSlider(MIN_FONT_POS, MAX_FONT_POS, mainUI.uiFontPercent)
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
            sizePanel.add(examplePanel)
            sizePanel.add(sliderPanel)

            val lafSizePanel = JPanel(BorderLayout())
            lafSizePanel.add(lafPanel, BorderLayout.NORTH)
            lafSizePanel.add(sizePanel, BorderLayout.CENTER)

            val dividerPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val dividerLabel = JLabel("Divider Size(1 ~ 20) [${mainUI.splitLogPane.dividerSize}]")
            dividerPanel.add(dividerLabel)
            dividerSlider = JSlider(0, MAX_DIVIDER_POS, mainUI.splitLogPane.dividerSize)
            dividerSlider.majorTickSpacing = 5
            dividerSlider.minorTickSpacing = 1
            dividerSlider.paintTicks = true
            dividerSlider.paintLabels = true
            dividerSlider.addChangeListener {
                if (dividerSlider.value == 0) {
                    dividerSlider.value = MIN_DIVIDER_POS
                }
                mainUI.splitLogPane.dividerSize = dividerSlider.value
                dividerLabel.text = "Divider Size(1 ~ 20) [${mainUI.splitLogPane.dividerSize}]"
            }
            dividerPanel.add(dividerSlider)

            val optionsPanel = JPanel(BorderLayout())
            optionsPanel.add(dividerPanel, BorderLayout.CENTER)

            val dialogPanel = JPanel(BorderLayout())
            dialogPanel.add(lafSizePanel, BorderLayout.NORTH)
            dialogPanel.add(optionsPanel, BorderLayout.CENTER)
            val panel = JPanel(BorderLayout())
            panel.add(dialogPanel, BorderLayout.CENTER)

            add(panel)
        }

        inner class ImagePanel(private val icon: ImageIcon) : JPanel() {
            init {
                preferredSize = Dimension(150, 106)
                background = Color.RED
            }

            override fun paint(g: Graphics) {
                super.paint(g)
                g.drawImage(icon.image, 0, 0, icon.iconWidth, icon.iconHeight, null)
            }
        }

        fun actionBtn(isOK: Boolean) {
            if (isOK) {
                AppearanceDialog.saveConfiguration(laFGroup, mainUI, fontSlider)
            } else {
                mainUI.splitLogPane.dividerSize = prevDividerSize
            }
        }
    }

    enum class ComboIdx(val value: Int) {
        LOG(0),
        TAG(1),
        PID(2),
        TID(3),
        BOLD(4),
        SIZE(5);
    }

    inner class FilterComboPanel : JPanel(), WindowListener {

        private var exampleLabel: JLabel
        private var exampleCombo: FilterComboBox

        private val comboLabelArray = arrayOfNulls<ColorLabel>(ComboIdx.SIZE.value)
        private val styleComboArray = arrayOfNulls<JComboBox<String>>(ComboIdx.SIZE.value)

        private var confirmLabel: JLabel

        private val titleLabelArray = arrayOfNulls<ColorLabel>(ColorManager.filterStyle.size)
        private val colorLabelArray = arrayOfNulls<ColorLabel>(ColorManager.filterStyle.size)
        private val mouseHandler = MouseHandler()
        private val prevColorArray = arrayOfNulls<String>(ColorManager.filterStyle.size)
        private var isNeedRestore = true

        init {
            layout = FlowLayout(FlowLayout.LEFT)
            confirmLabel = JLabel("To apply \"Style\" need to restart")

            exampleLabel = JLabel("Ex : ")
            exampleCombo = getFilterComboBox(useColorTag = true) withName "exampleCombo"
            exampleCombo.isEditable = true
            exampleCombo.preferredSize = Dimension(250, 30)
            exampleCombo.addItem("ABC|DEF|-GHI|JKL")

            val styleLabelPanel = JPanel()
            styleLabelPanel.layout = BoxLayout(styleLabelPanel, BoxLayout.Y_AXIS)

            val styleComboPanel = JPanel()
            styleComboPanel.layout = BoxLayout(styleComboPanel, BoxLayout.Y_AXIS)

            val rightWidth = 240
            for (idx in comboLabelArray.indices) {
                comboLabelArray[idx] = ColorLabel(idx)
                comboLabelArray[idx]!!.isOpaque = true
                comboLabelArray[idx]!!.horizontalAlignment = JLabel.LEFT
                comboLabelArray[idx]!!.foreground = Color.DARK_GRAY
                comboLabelArray[idx]!!.background = Color.WHITE

                comboLabelArray[idx]!!.verticalAlignment = JLabel.CENTER
                comboLabelArray[idx]!!.border = BorderFactory.createLineBorder(Color.BLACK)
                comboLabelArray[idx]!!.minimumSize = Dimension(200, 20)
                comboLabelArray[idx]!!.preferredSize = Dimension(200, 20)
                comboLabelArray[idx]!!.maximumSize = Dimension(200, 20)

                styleComboArray[idx] = JComboBox()
                styleComboArray[idx]!!.border = BorderFactory.createLineBorder(Color.BLACK)
                styleComboArray[idx]!!.minimumSize = Dimension(rightWidth, 20)
                styleComboArray[idx]!!.preferredSize = Dimension(rightWidth, 20)
                styleComboArray[idx]!!.maximumSize = Dimension(rightWidth, 20)
                styleComboArray[idx]!!.addItem("SINGLE LINE")
                styleComboArray[idx]!!.addItem("SINGLE LINE / HIGHLIGHT")
                styleComboArray[idx]!!.addItem("MULTI LINE")
                styleComboArray[idx]!!.addItem("MULTI LINE / HIGHLIGHT")
            }

            comboLabelArray[ComboIdx.LOG.value]!!.text = "Combo Style : Log"
            comboLabelArray[ComboIdx.TAG.value]!!.text = "Combo Style : Tag"
            comboLabelArray[ComboIdx.PID.value]!!.text = "Combo Style : PID"
            comboLabelArray[ComboIdx.TID.value]!!.text = "Combo Style : TID"
            comboLabelArray[ComboIdx.BOLD.value]!!.text = "Combo Style : BOLD"

            for (idx in comboLabelArray.indices) {
                styleLabelPanel.add(comboLabelArray[idx])
                styleLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))
                styleComboPanel.add(styleComboArray[idx])
                styleComboPanel.add(Box.createRigidArea(Dimension(5, 3)))
            }

            val stylePanel = JPanel()
            stylePanel.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
            stylePanel.add(styleLabelPanel)
            stylePanel.add(styleComboPanel)

            val colorLabelPanel = JPanel()
            colorLabelPanel.layout = BoxLayout(colorLabelPanel, BoxLayout.Y_AXIS)

            val titleLabelPanel = JPanel()
            titleLabelPanel.layout = BoxLayout(titleLabelPanel, BoxLayout.Y_AXIS)

            for (idx in colorLabelArray.indices) {
                prevColorArray[idx] = ColorManager.filterStyle[idx].strColor
                colorLabelArray[idx] = ColorLabel(idx)
                colorLabelArray[idx]!!.text = " ${ColorManager.filterStyle[idx].name} ${ColorManager.filterStyle[idx].strColor} "
                colorLabelArray[idx]!!.toolTipText = colorLabelArray[idx]!!.text
                colorLabelArray[idx]!!.isOpaque = true
                colorLabelArray[idx]!!.horizontalAlignment = JLabel.LEFT

                colorLabelArray[idx]!!.verticalAlignment = JLabel.CENTER
                colorLabelArray[idx]!!.border = BorderFactory.createLineBorder(Color.BLACK)
                colorLabelArray[idx]!!.minimumSize = Dimension(rightWidth, 20)
                colorLabelArray[idx]!!.preferredSize = Dimension(rightWidth, 20)
                colorLabelArray[idx]!!.maximumSize = Dimension(rightWidth, 20)
                colorLabelArray[idx]!!.addMouseListener(mouseHandler)

                titleLabelArray[idx] = ColorLabel(idx)
                titleLabelArray[idx]!!.text = " ${ColorManager.filterStyle[idx].name}"
                titleLabelArray[idx]!!.toolTipText = colorLabelArray[idx]!!.text
                titleLabelArray[idx]!!.isOpaque = true
                titleLabelArray[idx]!!.horizontalAlignment = JLabel.LEFT
                titleLabelArray[idx]!!.foreground = Color.DARK_GRAY
                titleLabelArray[idx]!!.background = Color.WHITE

                titleLabelArray[idx]!!.verticalAlignment = JLabel.CENTER
                titleLabelArray[idx]!!.border = BorderFactory.createLineBorder(Color.BLACK)
                titleLabelArray[idx]!!.minimumSize = Dimension(200, 20)
                titleLabelArray[idx]!!.preferredSize = Dimension(200, 20)
                titleLabelArray[idx]!!.maximumSize = Dimension(200, 20)
                titleLabelArray[idx]!!.addMouseListener(mouseHandler)
            }

            for (order in colorLabelArray.indices) {
                for (idx in colorLabelArray.indices) {
                    if (order == ColorManager.filterStyle[idx].order) {
                        colorLabelPanel.add(colorLabelArray[idx])
                        colorLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))
                        titleLabelPanel.add(titleLabelArray[idx])
                        titleLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))
                        break
                    }
                }
            }

            val colorPanel = JPanel()
            colorPanel.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
            colorPanel.add(titleLabelPanel)
            colorPanel.add(colorLabelPanel)

            updateLabelColor()

            val examplePanel = JPanel()
            examplePanel.add(exampleLabel)
            examplePanel.add(exampleCombo)

            val schemePanel = JPanel()
            val schemeLabel = JLabel("${STRINGS.ui.builtInSchemes} : ")
            val radioLight = JRadioButton(STRINGS.ui.light)
            val radioDark = JRadioButton(STRINGS.ui.dark)
            val buttonGroup = ButtonGroup()
            val schemeBtn = JButton(STRINGS.ui.apply)

            schemeBtn.addActionListener {
                if (radioLight.isSelected) {
                    applyColorScheme(ColorManager.filterColorSchemeLight)
                } else if (radioDark.isSelected) {
                    applyColorScheme(ColorManager.filterColorSchemeDark)
                }
            }

            buttonGroup.add(radioLight)
            buttonGroup.add(radioDark)
            schemePanel.add(schemeLabel)
            schemePanel.add(radioLight)
            schemePanel.add(radioDark)
            schemePanel.add(schemeBtn)

            val colorSettingPanel = JPanel(BorderLayout())
            colorSettingPanel.add(examplePanel, BorderLayout.NORTH)
            colorSettingPanel.add(colorPanel, BorderLayout.CENTER)
            colorSettingPanel.add(schemePanel, BorderLayout.SOUTH)

            val panel = JPanel(BorderLayout())
            panel.add(stylePanel, BorderLayout.WEST)
            panel.add(JLabel("   "), BorderLayout.CENTER)
            panel.add(colorSettingPanel, BorderLayout.EAST)

            add(panel)
        }

        override fun windowClosing(e: WindowEvent) {
            GLog.d(TAG, "exit Filter Style, restore $isNeedRestore")

            if (isNeedRestore) {
                for (idx in colorLabelArray.indices) {
                    ColorManager.filterStyle[idx].strColor = prevColorArray[idx]!!
                }
                ColorManager.applyFilterStyle()
            }
        }

        override fun windowOpened(e: WindowEvent) {
            // nothing
        }

        override fun windowClosed(e: WindowEvent) {
            // nothing
        }

        override fun windowIconified(e: WindowEvent) {
            // nothing
        }

        override fun windowDeiconified(e: WindowEvent) {
            // nothing
        }

        override fun windowActivated(e: WindowEvent) {
            // nothing
        }

        override fun windowDeactivated(e: WindowEvent) {
            // nothing
        }

        private fun applyColorScheme(scheme: Array<String>) {
            for(idx in scheme.indices) {
                colorLabelArray[idx]!!.text = " ${ColorManager.filterStyle[idx].name} ${scheme[idx]} "
                ColorManager.filterStyle[idx].strColor = scheme[idx]
                colorLabelArray[idx]!!.background = Color.decode(scheme[idx])
            }

            ColorManager.applyFilterStyle()
            updateLabelColor()
            val selectedItem = exampleCombo.selectedItem
            exampleCombo.selectedItem = ""
            exampleCombo.selectedItem = selectedItem
        }

        fun updateLabelColor() {
            val commonFg = Color.BLACK

            for (idx in colorLabelArray.indices) {
                colorLabelArray[idx]!!.foreground = commonFg
                colorLabelArray[idx]!!.background = Color.decode(ColorManager.filterStyle[idx].strColor)
            }
        }

        inner class ColorLabel(val idx: Int) :JLabel()

        fun actionBtn(isOK: Boolean) {
            if (isOK) {
                isNeedRestore = false
            }
        }

        internal inner class MouseHandler: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val colorChooser = JColorChooser()
                val panels = colorChooser.chooserPanels
                var rgbPanel: JPanel? = null
                for (panel in panels) {
                    if (panel.displayName.contains("RGB", true)) {
                        rgbPanel = panel
                    }
                }

                if (rgbPanel != null) {
                    val tmpColorLabel = e.source as ColorLabel
                    val idx = tmpColorLabel.idx
                    val colorLabel = colorLabelArray[idx]!!
                    colorChooser.color = colorLabel.background

                    val ret = JOptionPane.showConfirmDialog(this@AppearanceSettingsDialog, rgbPanel, "Color Chooser", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    if (ret == JOptionPane.OK_OPTION) {
                        val hex = "#" + Integer.toHexString(colorChooser.color.rgb).substring(2).uppercase()
                        colorLabel.text = " ${ColorManager.filterStyle[idx].name} $hex "
                        ColorManager.filterStyle[idx].strColor = hex
                        colorLabel.background = colorChooser.color
                        ColorManager.applyFilterStyle()
                        updateLabelColor()
                        val selectedItem = exampleCombo.selectedItem
                        exampleCombo.selectedItem = ""
                        exampleCombo.selectedItem = selectedItem
                    }
                }

                super.mouseClicked(e)
            }
        }
    }

    inner class FontColorPanel : JPanel(), WindowListener {
        private var nameScrollPane: JScrollPane
        private var nameList: JList<String>
        private var sizeLabel: JLabel
        private var sizeSpinner: JSpinner
        private var exampleLabel: JLabel
        private val prevFont = mainUI.customFont

        private val fullTableColor = ColorManager.fullTableColor
        private val filterTableColor = ColorManager.filterTableColor

        private val titleLabelArray = arrayOfNulls<ColorLabel>(fullTableColor.colorArray.size)
        private val fullColorLabelArray = arrayOfNulls<ColorLabel>(fullTableColor.colorArray.size)
        private val fullPrevColorArray = arrayOfNulls<String>(fullTableColor.colorArray.size)
        private val filterColorLabelArray = arrayOfNulls<ColorLabel>(filterTableColor.colorArray.size)
        private val filterPrevColorArray = arrayOfNulls<String>(filterTableColor.colorArray.size)
        private val mouseHandler = MouseHandler()
        private var isNeedRestore = true

        init {
            layout = FlowLayout(FlowLayout.LEFT)
            nameList = JList(GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames)
            nameList.selectionMode = ListSelectionModel.SINGLE_SELECTION
            nameScrollPane = JScrollPane(nameList)
            nameScrollPane.preferredSize = Dimension(400, 100)
            nameList.setSelectedValue(mainUI.customFont.family, true)
            nameScrollPane.verticalScrollBar.setUI(BasicScrollBarUI())
            nameScrollPane.horizontalScrollBar.setUI(BasicScrollBarUI())
            nameList.addListSelectionListener(ListSelectionHandler())

            sizeLabel = JLabel(STRINGS.ui.size)
            sizeSpinner = JSpinner(SpinnerNumberModel())
            sizeSpinner.model.value = mainUI.customFont.size
            sizeSpinner.preferredSize = Dimension(70, 30)
            sizeSpinner.addChangeListener(ChangeHandler())
            exampleLabel = JLabel("123 가나다 ABC abc", SwingConstants.CENTER)
            exampleLabel.font = mainUI.customFont
            exampleLabel.border = BorderFactory.createLineBorder(Color(0x50, 0x50, 0x50))
            exampleLabel.preferredSize = Dimension(250, 30)

            val fullColorLabelPanel = JPanel()
            fullColorLabelPanel.layout = BoxLayout(fullColorLabelPanel, BoxLayout.Y_AXIS)

            val filterColorLabelPanel = JPanel()
            filterColorLabelPanel.layout = BoxLayout(filterColorLabelPanel, BoxLayout.Y_AXIS)

            val titleLabelPanel = JPanel()
            titleLabelPanel.layout = BoxLayout(titleLabelPanel, BoxLayout.Y_AXIS)

            for (idx in titleLabelArray.indices) {
                fullPrevColorArray[idx] = fullTableColor.colorArray[idx].strColor
                fullColorLabelArray[idx] = ColorLabel(ColorManager.TableColorType.FULL_LOG_TABLE, idx)
                fullColorLabelArray[idx]!!.text = " ${fullTableColor.colorArray[idx].name} ${fullTableColor.colorArray[idx].strColor} "
                fullColorLabelArray[idx]!!.toolTipText = fullColorLabelArray[idx]!!.text
                fullColorLabelArray[idx]!!.isOpaque = true
                if (fullTableColor.colorArray[idx].name.contains("BG")) {
                    fullColorLabelArray[idx]!!.horizontalAlignment = JLabel.RIGHT
                }
                else {
                    fullColorLabelArray[idx]!!.horizontalAlignment = JLabel.LEFT
                }

                fullColorLabelArray[idx]!!.verticalAlignment = JLabel.CENTER
                fullColorLabelArray[idx]!!.border = BorderFactory.createLineBorder(Color.BLACK)
                fullColorLabelArray[idx]!!.minimumSize = Dimension(330, 20)
                fullColorLabelArray[idx]!!.preferredSize = Dimension(330, 20)
                fullColorLabelArray[idx]!!.maximumSize = Dimension(330, 20)
                fullColorLabelArray[idx]!!.addMouseListener(mouseHandler)

                filterPrevColorArray[idx] = filterTableColor.colorArray[idx].strColor
                filterColorLabelArray[idx] = ColorLabel(ColorManager.TableColorType.FILTER_LOG_TABLE, idx)
                filterColorLabelArray[idx]!!.text = " ${filterTableColor.colorArray[idx].name} ${filterTableColor.colorArray[idx].strColor} "
                filterColorLabelArray[idx]!!.toolTipText = filterColorLabelArray[idx]!!.text
                filterColorLabelArray[idx]!!.isOpaque = true
                if (filterTableColor.colorArray[idx].name.contains("BG")) {
                    filterColorLabelArray[idx]!!.horizontalAlignment = JLabel.RIGHT
                }
                else {
                    filterColorLabelArray[idx]!!.horizontalAlignment = JLabel.LEFT
                }

                filterColorLabelArray[idx]!!.verticalAlignment = JLabel.CENTER
                filterColorLabelArray[idx]!!.border = BorderFactory.createLineBorder(Color.BLACK)
                filterColorLabelArray[idx]!!.minimumSize = Dimension(330, 20)
                filterColorLabelArray[idx]!!.preferredSize = Dimension(330, 20)
                filterColorLabelArray[idx]!!.maximumSize = Dimension(330, 20)
                filterColorLabelArray[idx]!!.addMouseListener(mouseHandler)

                titleLabelArray[idx] = ColorLabel(ColorManager.TableColorType.FULL_LOG_TABLE, idx)
                titleLabelArray[idx]!!.text = " ${fullTableColor.colorArray[idx].name}"
                titleLabelArray[idx]!!.toolTipText = fullColorLabelArray[idx]!!.text
                titleLabelArray[idx]!!.isOpaque = true
                titleLabelArray[idx]!!.horizontalAlignment = JLabel.LEFT
                if (titleLabelArray[idx]!!.text.contains("BG")) {
                    titleLabelArray[idx]!!.foreground = Color.WHITE
                    titleLabelArray[idx]!!.background = Color.DARK_GRAY
                }
                else {
                    titleLabelArray[idx]!!.foreground = Color.DARK_GRAY
                    titleLabelArray[idx]!!.background = Color.WHITE
                }

                titleLabelArray[idx]!!.verticalAlignment = JLabel.CENTER
                titleLabelArray[idx]!!.border = BorderFactory.createLineBorder(Color.BLACK)
                titleLabelArray[idx]!!.minimumSize = Dimension(250, 20)
                titleLabelArray[idx]!!.preferredSize = Dimension(250, 20)
                titleLabelArray[idx]!!.maximumSize = Dimension(250, 20)
            }

            var label = JLabel("  ")
            label.horizontalAlignment = JLabel.CENTER
            label.maximumSize = Dimension(250, 20)
            titleLabelPanel.add(label)
            titleLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))

            label = JLabel("<html><font color=\"#000000\">--- <b><font color=\"#FF0000\">${STRINGS.ui.full}</font></b> ${STRINGS.ui.log} ${STRINGS.ui.color} ---</font></html>")
            label.horizontalAlignment = JLabel.CENTER
            label.isOpaque = true
            label.preferredSize = Dimension(330, 20)
            label.background = Color.WHITE
            fullColorLabelPanel.add(label)
            fullColorLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))

            label = JLabel("<html><font color=\"#000000\">--- <b><font color=\"#0000FF\">${STRINGS.ui.filter}</font></b> ${STRINGS.ui.log} ${STRINGS.ui.color} ---</font></html>")
            label.horizontalAlignment = JLabel.CENTER
            label.isOpaque = true
            label.preferredSize = Dimension(330, 20)
            label.background = Color.WHITE
            filterColorLabelPanel.add(label)
            filterColorLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))


            for (order in titleLabelArray.indices) {
                for (idx in titleLabelArray.indices) {
                    if (order == fullTableColor.colorArray[idx].order) {
                        titleLabelPanel.add(titleLabelArray[idx])
                        titleLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))

                        fullColorLabelPanel.add(fullColorLabelArray[idx])
                        fullColorLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))

                        filterColorLabelPanel.add(filterColorLabelArray[idx])
                        filterColorLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))

                        break
                    }
                }
            }

            val colorPanel = JPanel()
            colorPanel.layout = FlowLayout(FlowLayout.LEFT, 0, 0)
            colorPanel.add(titleLabelPanel)
            colorPanel.add(fullColorLabelPanel)
            colorPanel.add(filterColorLabelPanel)

            updateLabelColor(ColorManager.TableColorType.FULL_LOG_TABLE)
            updateLabelColor(ColorManager.TableColorType.FILTER_LOG_TABLE)


            val sizePanel = JPanel()
            sizePanel.add(sizeLabel)
            sizePanel.add(sizeSpinner)
            sizePanel.add(exampleLabel)

            val schemePanel = JPanel()
            val schemeLabel = JLabel("${STRINGS.ui.builtInSchemes} : ")
            val fullCheckbox = JCheckBox("${STRINGS.ui.full} ${STRINGS.ui.log}", true)
            val filterCheckbox = JCheckBox("${STRINGS.ui.filter} ${STRINGS.ui.log}", true)
            val radioLight = JRadioButton(STRINGS.ui.light)
            val radioDark = JRadioButton(STRINGS.ui.dark)
            val buttonGroup = ButtonGroup()
            val schemeBtn = JButton(STRINGS.ui.apply)

            schemeBtn.addActionListener(ActionListener {
                val scheme: Array<String> = if (radioLight.isSelected) {
                    ColorManager.colorSchemeLight
                } else if (radioDark.isSelected) {
                    ColorManager.colorSchemeDark
                } else {
                    GLog.d(TAG, "Scheme is not selected")
                    return@ActionListener
                }

                if (fullCheckbox.isSelected && filterCheckbox.isSelected) {
                    applyColorScheme(scheme)
                }
                else if (fullCheckbox.isSelected) {
                    applyColorScheme(ColorManager.TableColorType.FULL_LOG_TABLE, scheme, true)
                }
                else if (filterCheckbox.isSelected) {
                    applyColorScheme(ColorManager.TableColorType.FILTER_LOG_TABLE, scheme, true)
                }
                else {
                    GLog.d(TAG, "Target log(full/filter) is not selected")
                }
            })

            buttonGroup.add(radioLight)
            buttonGroup.add(radioDark)

            val schemePanelSub = JPanel(BorderLayout())
            val schemePanelSubNorth = JPanel()
            val schemePanelSubSouth = JPanel()

            schemePanelSubNorth.add(fullCheckbox)
            schemePanelSubNorth.add(filterCheckbox)
            schemePanelSubSouth.add(radioLight)
            schemePanelSubSouth.add(radioDark)
            schemePanelSubSouth.add(schemeBtn)

            schemePanelSub.add(schemePanelSubNorth, BorderLayout.NORTH)
            schemePanelSub.add(schemePanelSubSouth, BorderLayout.SOUTH)

            schemePanel.add(schemeLabel)
            schemePanel.add(schemePanelSub)


            val sizeSchemePanel = JPanel()
            sizeSchemePanel.layout = BoxLayout(sizeSchemePanel, BoxLayout.Y_AXIS)
            sizeSchemePanel.add(sizePanel)
            sizeSchemePanel.add(schemePanel)

            val namePanel = JPanel()
            namePanel.layout = GridLayout(1, 2, 3, 3)
            namePanel.add(nameScrollPane)
            namePanel.add(sizeSchemePanel)

            val bottomPanel = JPanel(BorderLayout())
            bottomPanel.add(JLabel("   "), BorderLayout.NORTH)
            bottomPanel.add(colorPanel, BorderLayout.CENTER)
            val panel = JPanel(BorderLayout())
            panel.add(namePanel, BorderLayout.CENTER)
            panel.add(bottomPanel, BorderLayout.SOUTH)

            add(panel)
        }

        override fun windowClosing(e: WindowEvent) {
            GLog.d(TAG, "exit Font Color, restore $isNeedRestore")

            if (isNeedRestore) {
                for (idx in fullColorLabelArray.indices) {
                    fullTableColor.colorArray[idx].strColor = fullPrevColorArray[idx]!!
                    filterTableColor.colorArray[idx].strColor = filterPrevColorArray[idx]!!
                }
                fullTableColor.applyColor()
                filterTableColor.applyColor()
                mainUI.customFont = prevFont
            }
            else {
                ConfigManager.saveFontColors(mainUI.customFont.family, mainUI.customFont.size)
            }
        }

        override fun windowOpened(e: WindowEvent) {
            // nothing
        }

        override fun windowClosed(e: WindowEvent) {
            // nothing
        }

        override fun windowIconified(e: WindowEvent) {
            // nothing
        }

        override fun windowDeiconified(e: WindowEvent) {
            // nothing
        }

        override fun windowActivated(e: WindowEvent) {
            // nothing
        }

        override fun windowDeactivated(e: WindowEvent) {
            // nothing
        }

        private fun applyColorScheme(type: ColorManager.TableColorType, scheme: Array<String>, isUpdateUI: Boolean) {
            val colorLabelArray = if (type == ColorManager.TableColorType.FULL_LOG_TABLE) {
                fullColorLabelArray
            } else {
                filterColorLabelArray
            }

            val tableColor = if (type == ColorManager.TableColorType.FULL_LOG_TABLE) {
                fullTableColor
            } else {
                filterTableColor
            }

            val logPanel = if (type == ColorManager.TableColorType.FULL_LOG_TABLE) {
                mainUI.splitLogPane.fullLogPanel
            } else {
                mainUI.splitLogPane.filteredLogPanel
            }

            for (idx in colorLabelArray.indices) {
                tableColor.colorArray[idx].strColor = scheme[idx]
                colorLabelArray[idx]!!.text = " ${fullTableColor.colorArray[idx].name} ${scheme[idx]} "

                if (colorLabelArray[idx]!!.text.contains("BG")) {
                    colorLabelArray[idx]!!.background = Color.decode(scheme[idx])
                } else {
                    colorLabelArray[idx]!!.foreground = Color.decode(scheme[idx])
                }
            }
            tableColor.applyColor()
            updateLabelColor(type)

            if (isUpdateUI) {
                logPanel.repaint()
            }
        }

        private fun applyColorScheme(scheme: Array<String>) {
            applyColorScheme(ColorManager.TableColorType.FULL_LOG_TABLE, scheme, false)
            applyColorScheme(ColorManager.TableColorType.FILTER_LOG_TABLE, scheme, false)
            mainUI.splitLogPane.fullLogPanel.repaint()
            mainUI.splitLogPane.filteredLogPanel.repaint()
        }

        fun updateLabelColor(type: ColorManager.TableColorType) {
            var logBg:Color? = null
            var logFg:Color? = null
            var lineNumBg:Color? = null
            var lineNumFg:Color? = null
            var filteredBg:Color? = null
            var filteredFg:Color? = null
            var highlightBg:Color? = null
            var highlightFg:Color? = null
            var searchBg:Color? = null
            var searchFg:Color? = null

            val colorLabelArray = if (type == ColorManager.TableColorType.FULL_LOG_TABLE) {
                fullColorLabelArray
            } else {
                filterColorLabelArray
            }

            val tableColor = if (type == ColorManager.TableColorType.FULL_LOG_TABLE) {
                fullTableColor
            } else {
                filterTableColor
            }

            for (idx in colorLabelArray.indices) {
                when (tableColor.colorArray[idx].name) {
                    "Log BG"->logBg = Color.decode(tableColor.colorArray[idx].strColor)
                    "Log Level None"->logFg = Color.decode(tableColor.colorArray[idx].strColor)
                    "LineNum BG"->lineNumBg = Color.decode(tableColor.colorArray[idx].strColor)
                    "LineNum FG"->lineNumFg = Color.decode(tableColor.colorArray[idx].strColor)
                    "Filtered BG"->filteredBg = Color.decode(tableColor.colorArray[idx].strColor)
                    "Filtered FG"->filteredFg = Color.decode(tableColor.colorArray[idx].strColor)
                    "Highlight BG"->highlightBg = Color.decode(tableColor.colorArray[idx].strColor)
                    "Highlight FG"->highlightFg = Color.decode(tableColor.colorArray[idx].strColor)
                    "Search BG"->searchBg = Color.decode(tableColor.colorArray[idx].strColor)
                    "Search FG"->searchFg = Color.decode(tableColor.colorArray[idx].strColor)
                }
            }

            logFg = logFg ?: Color.BLACK
            logBg = logBg ?: Color.WHITE
            lineNumFg = lineNumFg ?: Color.BLACK
            lineNumBg = lineNumBg ?: Color.WHITE
            filteredFg = filteredFg ?: Color.BLACK
            filteredBg = filteredBg ?: Color.WHITE
            highlightFg = highlightFg ?: Color.BLACK
            highlightBg = highlightBg ?: Color.WHITE
            searchFg = searchFg ?: Color.BLACK
            searchBg = searchBg ?: Color.WHITE

            for (idx in colorLabelArray.indices) {
                if (tableColor.colorArray[idx].name.contains("BG")) {
                    colorLabelArray[idx]!!.background = Color.decode(tableColor.colorArray[idx].strColor)
                    when (tableColor.colorArray[idx].name) {
                        "LineNum BG" -> {
                            colorLabelArray[idx]!!.foreground = lineNumFg
                        }
                        "Filtered BG" -> {
                            colorLabelArray[idx]!!.foreground = filteredFg
                        }
                        "Highlight BG" -> {
                            colorLabelArray[idx]!!.foreground = highlightFg
                        }
                        "Search BG" -> {
                            colorLabelArray[idx]!!.foreground = searchFg
                        }
                        else -> {
                            if ((tableColor.colorArray[idx].name == "Filtered 1 BG")
                                    || (tableColor.colorArray[idx].name == "Filtered 2 BG")
                                    || (tableColor.colorArray[idx].name == "Filtered 3 BG")
                                    || (tableColor.colorArray[idx].name == "Filtered 4 BG")
                                    || (tableColor.colorArray[idx].name == "Filtered 5 BG")
                                    || (tableColor.colorArray[idx].name == "Filtered 6 BG")
                                    || (tableColor.colorArray[idx].name == "Filtered 7 BG")
                                    || (tableColor.colorArray[idx].name == "Filtered 8 BG")
                                    || (tableColor.colorArray[idx].name == "Filtered 9 BG")) {
                                colorLabelArray[idx]!!.foreground = Color.decode(tableColor.colorArray[idx - 9].strColor)
                            }
                            else {
                                colorLabelArray[idx]!!.foreground = logFg
                            }
                        }
                    }
                }
                else {
                    colorLabelArray[idx]!!.foreground = Color.decode(tableColor.colorArray[idx].strColor)
                    when (tableColor.colorArray[idx].name) {
                        "LineNum FG" -> {
                            colorLabelArray[idx]!!.background = lineNumBg
                        }
                        "Filtered FG" -> {
                            colorLabelArray[idx]!!.background = filteredBg
                        }
                        "Highlight FG" -> {
                            colorLabelArray[idx]!!.background = highlightBg
                        }
                        "Search FG" -> {
                            colorLabelArray[idx]!!.background = searchBg
                        }
                        else -> {
                            if ((tableColor.colorArray[idx].name == "Filtered 1 FG")
                                    || (tableColor.colorArray[idx].name == "Filtered 2 FG")
                                    || (tableColor.colorArray[idx].name == "Filtered 3 FG")
                                    || (tableColor.colorArray[idx].name == "Filtered 4 FG")
                                    || (tableColor.colorArray[idx].name == "Filtered 5 FG")
                                    || (tableColor.colorArray[idx].name == "Filtered 6 FG")
                                    || (tableColor.colorArray[idx].name == "Filtered 7 FG")
                                    || (tableColor.colorArray[idx].name == "Filtered 8 FG")
                                    || (tableColor.colorArray[idx].name == "Filtered 9 FG")) {
                                colorLabelArray[idx]!!.background = Color.decode(tableColor.colorArray[idx + 9].strColor)
                            }
                            else {
                                colorLabelArray[idx]!!.background = logBg
                            }
                        }
                    }
                }
            }
        }

        inner class ColorLabel(val type: ColorManager.TableColorType, val idx: Int) :JLabel()

        fun actionBtn(isOK: Boolean) {
            if (isOK) {
                isNeedRestore = false
            }
        }

        private fun setFont() {
            val selection = nameList.selectedValue
            val size = sizeSpinner.model.value as Int
            exampleLabel.font = Font(selection.toString(), Font.PLAIN, size)
            mainUI.customFont = Font(selection.toString(), Font.PLAIN, size)
        }

        internal inner class ChangeHandler: ChangeListener {
            override fun stateChanged(e: ChangeEvent) {
                setFont()
            }
        }

        internal inner class ListSelectionHandler : ListSelectionListener {
            override fun valueChanged(event: ListSelectionEvent) {
                if (event.source == nameList) {
                    setFont()
                }
            }
        }

        val optionFullCheckbox = JCheckBox(STRINGS.ui.fullLogTable)
        val optionFilterCheckbox = JCheckBox(STRINGS.ui.filterLogTable)
        internal inner class MouseHandler: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val colorChooser = JColorChooser()
                val panels = colorChooser.chooserPanels
                var rgbPanel:JPanel? = null
                for (panel in panels) {
                    if (panel.displayName.contains("RGB", true)) {
                        rgbPanel = panel
                    }
                }

                if (rgbPanel != null) {
                    val colorLabel = e.source as ColorLabel
                    if (colorLabel.text.contains("BG")) {
                        colorChooser.color = colorLabel.background
                    } else {
                        colorChooser.color = colorLabel.foreground
                    }

                    val optionPanel = JPanel()
                    val optionTitleLabel = JLabel("${titleLabelArray[colorLabel.idx]!!.text} : ")
                    if (!optionFullCheckbox.isSelected || !optionFilterCheckbox.isSelected) {
                        if (colorLabel.type == ColorManager.TableColorType.FULL_LOG_TABLE) {
                            optionFullCheckbox.isSelected = true
                            optionFilterCheckbox.isSelected = false
                        } else {
                            optionFullCheckbox.isSelected = false
                            optionFilterCheckbox.isSelected = true
                        }
                    }

                    optionPanel.add(optionTitleLabel)
                    optionPanel.add(optionFullCheckbox)
                    optionPanel.add(optionFilterCheckbox)

                    val colorPanel = JPanel(BorderLayout())
                    colorPanel.add(rgbPanel, BorderLayout.CENTER)
                    colorPanel.add(optionPanel, BorderLayout.SOUTH)

                    val ret = JOptionPane.showConfirmDialog(this@AppearanceSettingsDialog, colorPanel, "Color Chooser", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    if (ret == JOptionPane.OK_OPTION) {
                        if (optionFullCheckbox.isSelected) {
                            updateColor(fullColorLabelArray[colorLabel.idx]!!, colorChooser.color)
                        }
                        if (optionFilterCheckbox.isSelected) {
                            updateColor(filterColorLabelArray[colorLabel.idx]!!, colorChooser.color)
                        }

                        mainUI.splitLogPane.fullLogPanel.repaint()
                        mainUI.splitLogPane.filteredLogPanel.repaint()
                    }
                }

                super.mouseClicked(e)
            }

            private fun updateColor(colorLabel: ColorLabel, color: Color) {
                val hex = "#" + Integer.toHexString(color.rgb).substring(2).uppercase()
                colorLabel.text = " ${fullTableColor.colorArray[colorLabel.idx].name} $hex "
                val tableColor = if (colorLabel.type == ColorManager.TableColorType.FULL_LOG_TABLE) {
                    fullTableColor
                }
                else {
                    filterTableColor
                }
                tableColor.colorArray[colorLabel.idx].strColor = hex
                if (colorLabel.text.contains("BG")) {
                    colorLabel.background = color
                } else {
                    colorLabel.foreground = color
                }
                tableColor.applyColor()
                updateLabelColor(colorLabel.type)
            }
        }
    }

    companion object {
        private const val TAG = "AppearanceSettingsDialog"
        private const val MIN_FONT_POS = 50
        private const val MAX_FONT_POS = 200
        private const val EXAMPLE_TEXT = "ABC def GHI jkl 0123456789"

        private const val MIN_DIVIDER_POS = 1
        private const val MAX_DIVIDER_POS = 20
    }
}