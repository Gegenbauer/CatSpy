package me.gegenbauer.logviewer.ui.settings

import me.gegenbauer.logviewer.Utils
import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.strings.Strings
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.button.ColorButton
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.ListSelectionModel.SINGLE_SELECTION
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.plaf.basic.BasicScrollBarUI


class FontDialog(parent: MainUI) : JDialog(parent, Strings.FONT + " & " + Strings.COLOR + " " + Strings.SETTING, true),
    ActionListener {
    private var nameList: JList<String> =
        JList(GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames)
    private var nameScrollPane: JScrollPane
    private var sizeLabel: JLabel
    private var sizeSpinner: JSpinner
    private var exampleLabel: JLabel
    private var okBtn: ColorButton
    private var cancelBtn: ColorButton
    private var parent = parent
    private val prevFont = parent.customFont

    private val colorManager = ColorManager.getInstance()
    private val fullTableColor = colorManager.fullTableColor
    private val filterTableColor = colorManager.filterTableColor

    private val titleLabelArray = arrayOfNulls<ColorLabel>(fullTableColor.colorArray.size)
    private val fullColorLabelArray = arrayOfNulls<ColorLabel>(fullTableColor.colorArray.size)
    private val fullPrevColorArray = arrayOfNulls<String>(fullTableColor.colorArray.size)
    private val filterColorLabelArray = arrayOfNulls<ColorLabel>(filterTableColor.colorArray.size)
    private val filterPrevColorArray = arrayOfNulls<String>(filterTableColor.colorArray.size)
    private val mouseHandler = MouseHandler()
    private var isNeedRestore = true

    init {
        nameList.selectionMode = SINGLE_SELECTION
        nameScrollPane = JScrollPane(nameList)
        nameScrollPane.preferredSize = Dimension(400, 150)
        nameList.setSelectedValue(parent.customFont.family, true)
        nameScrollPane.verticalScrollBar.setUI(BasicScrollBarUI())
        nameScrollPane.horizontalScrollBar.setUI(BasicScrollBarUI())
        nameList.addListSelectionListener(ListSelectionHandler())
        okBtn = ColorButton(Strings.OK)
        okBtn.addActionListener(this)
        cancelBtn = ColorButton(Strings.CANCEL)
        cancelBtn.addActionListener(this)

        sizeLabel = JLabel(Strings.SIZE)
        sizeSpinner = JSpinner(SpinnerNumberModel())
        sizeSpinner.model.value = parent.customFont.size
        sizeSpinner.preferredSize = Dimension(70, 30)
        sizeSpinner.addChangeListener(ChangeHandler())
        exampleLabel = JLabel("123 가나다 ABC abc", SwingConstants.CENTER)
        exampleLabel.font = parent.customFont
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
            fullColorLabelArray[idx]!!.text =
                " ${fullTableColor.colorArray[idx].name} ${fullTableColor.colorArray[idx].strColor} "
            fullColorLabelArray[idx]!!.toolTipText = fullColorLabelArray[idx]!!.text
            fullColorLabelArray[idx]!!.isOpaque = true
            if (fullTableColor.colorArray[idx].name.contains("BG")) {
                fullColorLabelArray[idx]!!.horizontalAlignment = JLabel.RIGHT
            } else {
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
            filterColorLabelArray[idx]!!.text =
                " ${filterTableColor.colorArray[idx].name} ${filterTableColor.colorArray[idx].strColor} "
            filterColorLabelArray[idx]!!.toolTipText = filterColorLabelArray[idx]!!.text
            filterColorLabelArray[idx]!!.isOpaque = true
            if (filterTableColor.colorArray[idx].name.contains("BG")) {
                filterColorLabelArray[idx]!!.horizontalAlignment = JLabel.RIGHT
            } else {
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
            } else {
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

        label = JLabel(Strings.FULL_LOG_TABLE)
        label.horizontalAlignment = JLabel.CENTER
        label.maximumSize = Dimension(330, 20)
        fullColorLabelPanel.add(label)
        fullColorLabelPanel.add(Box.createRigidArea(Dimension(5, 3)))

        label = JLabel(Strings.FILTER_LOG_TABLE)
        label.horizontalAlignment = JLabel.CENTER
        label.maximumSize = Dimension(330, 20)
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
        val schemeLabel = JLabel("${Strings.BUILT_IN_SCHEMES} : ")
        val radioLight = JRadioButton(Strings.LIGHT)
        val radioDark = JRadioButton(Strings.DARK)
        val buttonGroup = ButtonGroup()
        val schemeBtn = JButton(Strings.APPLY)

        schemeBtn.addActionListener {
            if (radioLight.isSelected) {
                applyColorScheme(ColorManager.getInstance().colorSchemeLight)
            } else if (radioDark.isSelected) {
                applyColorScheme(ColorManager.getInstance().colorSchemeDark)
            }
        }

        buttonGroup.add(radioLight)
        buttonGroup.add(radioDark)
        schemePanel.add(schemeLabel)
        schemePanel.add(radioLight)
        schemePanel.add(radioDark)
        schemePanel.add(schemeBtn)

        val sizeSchemePanel = JPanel()
        sizeSchemePanel.layout = BoxLayout(sizeSchemePanel, BoxLayout.Y_AXIS)
        sizeSchemePanel.add(sizePanel)
        sizeSchemePanel.add(schemePanel)

        val namePanel = JPanel()
        namePanel.layout = GridLayout(1, 2, 3, 3)
        namePanel.add(nameScrollPane)
        namePanel.add(sizeSchemePanel)

        val confirmPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        confirmPanel.preferredSize = Dimension(300, 40)
        confirmPanel.alignmentX = JPanel.RIGHT_ALIGNMENT
        confirmPanel.add(okBtn)
        confirmPanel.add(cancelBtn)

        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.add(colorPanel, BorderLayout.CENTER)
        bottomPanel.add(confirmPanel, BorderLayout.SOUTH)
        val panel = JPanel(BorderLayout())
        panel.add(namePanel, BorderLayout.CENTER)
        panel.add(bottomPanel, BorderLayout.SOUTH)

        contentPane.add(panel)

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                println("exit Font Color dialog, restore $isNeedRestore")

                if (isNeedRestore) {
                    for (idx in fullColorLabelArray.indices) {
                        fullTableColor.colorArray[idx].strColor = fullPrevColorArray[idx]!!
                        filterTableColor.colorArray[idx].strColor = filterPrevColorArray[idx]!!
                    }
                    fullTableColor.applyColor()
                    filterTableColor.applyColor()
                    parent.customFont = prevFont
                } else {
                    parent.configManager.saveFontColors(parent.customFont.family, parent.customFont.size)
                }
            }
        })

        pack()
        Utils.installKeyStrokeEscClosing(this)
    }

    private fun applyColorScheme(type: ColorManager.TableColorType, scheme: Array<String>) {
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
            tableColor.colorArray[idx].strColor = scheme[idx]
            colorLabelArray[idx]!!.text = " ${fullTableColor.colorArray[idx].name} ${scheme[idx]} "

            if (colorLabelArray[idx]!!.text.contains("BG")) {
                colorLabelArray[idx]!!.background = Color.decode(scheme[idx])
            } else {
                colorLabelArray[idx]!!.foreground = Color.decode(scheme[idx])
            }
            tableColor.applyColor()
            updateLabelColor(type)
        }
    }

    private fun applyColorScheme(scheme: Array<String>) {
        applyColorScheme(ColorManager.TableColorType.FULL_LOG_TABLE, scheme)
        applyColorScheme(ColorManager.TableColorType.FILTER_LOG_TABLE, scheme)
        setFont()
    }

    fun updateLabelColor(type: ColorManager.TableColorType) {
        var commonBg: Color? = null
        var commonFg: Color? = null
        var lineNumBg: Color? = null
        var lineNumFg: Color? = null

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
                "FullLog BG" -> commonBg = Color.decode(tableColor.colorArray[idx].strColor)
                "Log Level None" -> commonFg = Color.decode(tableColor.colorArray[idx].strColor)
                "LineNum BG" -> lineNumBg = Color.decode(tableColor.colorArray[idx].strColor)
                "LineNum FG" -> lineNumFg = Color.decode(tableColor.colorArray[idx].strColor)
            }
        }

        if (commonFg == null) {
            commonFg = Color.BLACK
        }

        if (commonBg == null) {
            commonBg = Color.WHITE
        }

        if (lineNumFg == null) {
            lineNumFg = Color.BLACK
        }

        if (lineNumBg == null) {
            lineNumBg = Color.WHITE
        }

        for (idx in colorLabelArray.indices) {
            if (tableColor.colorArray[idx].name.contains("BG")) {
                colorLabelArray[idx]!!.background = Color.decode(tableColor.colorArray[idx].strColor)
                if (tableColor.colorArray[idx].name == "LineNum BG") {
                    colorLabelArray[idx]!!.foreground = lineNumFg
                } else {
                    colorLabelArray[idx]!!.foreground = commonFg
                }
            } else {
                colorLabelArray[idx]!!.foreground = Color.decode(tableColor.colorArray[idx].strColor)
                if (tableColor.colorArray[idx].name == "LineNum FG") {
                    colorLabelArray[idx]!!.background = lineNumBg
                } else {
                    colorLabelArray[idx]!!.background = commonBg
                }
            }
        }
    }

    class ColorLabel(val type: ColorManager.TableColorType, val idx: Int) : JLabel()

    override fun actionPerformed(e: ActionEvent?) {
        if (e?.source == okBtn) {
            isNeedRestore = false
            this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        } else if (e?.source == cancelBtn) {
            this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        }
    }

    private fun setFont() {
        val selection = nameList.selectedValue
        val size = sizeSpinner.model.value as Int
        exampleLabel.font = Font(selection.toString(), Font.PLAIN, size)

        parent.customFont = Font(selection.toString(), Font.PLAIN, size)
    }

    internal inner class ChangeHandler : ChangeListener {
        override fun stateChanged(e: ChangeEvent?) {
            setFont()
        }
    }

    internal inner class ListSelectionHandler : ListSelectionListener {
        override fun valueChanged(p0: ListSelectionEvent?) {
            if (p0?.source == nameList) {
                setFont()
            }
        }
    }

    val optionFullCheckbox = JCheckBox()
    val optionFilterCheckbox = JCheckBox()

    internal inner class MouseHandler : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
            val colorChooser = JColorChooser()
            val panels = colorChooser.chooserPanels
            var rgbPanel: JPanel? = null
            for (panel in panels) {
                if (panel.displayName.contains("RGB", true)) {
                    rgbPanel = panel
                }
            }

            if (rgbPanel != null) {
                val colorLabel = e!!.source as ColorLabel
                if (colorLabel.text.contains("BG")) {
                    colorChooser.color = colorLabel.background
                } else {
                    colorChooser.color = colorLabel.foreground
                }

                val optionPanel = JPanel()
                val optionTitleLabel = JLabel("${titleLabelArray[colorLabel.idx]!!.text} : ")
                val optionFullLabel = JLabel("${Strings.FULL_LOG_TABLE}  ")
                val optionFilterLabel = JLabel(Strings.FILTER_LOG_TABLE)
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
                optionPanel.add(optionFullLabel)
                optionPanel.add(optionFilterCheckbox)
                optionPanel.add(optionFilterLabel)

                val colorPanel = JPanel(BorderLayout())
                colorPanel.add(rgbPanel, BorderLayout.CENTER)
                colorPanel.add(optionPanel, BorderLayout.SOUTH)

                val ret = JOptionPane.showConfirmDialog(
                    this@FontDialog,
                    colorPanel,
                    "Color Chooser",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                )
                if (ret == JOptionPane.OK_OPTION) {
                    if (optionFullCheckbox.isSelected) {
                        updateColor(fullColorLabelArray[colorLabel.idx]!!, colorChooser.color)
                    }
                    if (optionFilterCheckbox.isSelected) {
                        updateColor(filterColorLabelArray[colorLabel.idx]!!, colorChooser.color)
                    }
                    setFont() // refresh log table
                }
            }

            super.mouseClicked(e)
        }

        private fun updateColor(colorLabel: ColorLabel, color: Color) {
            val hex = "#" + Integer.toHexString(color.rgb).substring(2).uppercase()
            colorLabel.text = " ${fullTableColor.colorArray[colorLabel.idx].name} $hex "
            val tableColor = if (colorLabel.type == ColorManager.TableColorType.FULL_LOG_TABLE) {
                fullTableColor
            } else {
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

