package me.gegenbauer.catspy.manager

import com.github.weisj.darklaf.settings.ThemeSettings
import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.ui.log.LogPanel
import me.gegenbauer.catspy.utils.Utils
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.ListSelectionListener


abstract class CustomListManager(val mainUI: MainUI, private val logPanel: LogPanel) {
    companion object {
        const val CMD_NEW = 1
        const val CMD_COPY = 2
        const val CMD_EDIT = 3
    }

    private var firstElement: CustomElement? = null
    private val customDialog = CustomDialog(mainUI)

    abstract fun loadList(): ArrayList<CustomElement>
    abstract fun saveList(list: ArrayList<CustomElement>)
    abstract fun getFirstElement(): CustomElement
    abstract fun getListSelectionListener(): ListSelectionListener
    abstract fun getListMouseListener(): MouseListener
    abstract fun getListKeyListener(): KeyListener

    fun showDialog() {
        customDialog.initDialog()
        customDialog.setLocationRelativeTo(mainUI)
        customDialog.isVisible = true
    }

    protected open val dialogTitle: String = "Custom List"

    data class CustomElement(val title: String, var value: String, val tableBar: Boolean)

    internal inner class CustomDialog(parent: MainUI) : JDialog(parent, dialogTitle, true), ActionListener {
        private var scrollPane: JScrollPane
        var jList = JList<CustomElement>()
        private val firstBtn = GButton("↑")
        private val prevBtn = GButton("∧")
        private val nextBtn = GButton("∨")
        private val lastBtn = GButton("↓")
        private val newBtn = GButton(STRINGS.ui.new)
        private val copyBtn = GButton(STRINGS.ui.copy)
        private val editBtn = GButton(STRINGS.ui.edit)
        private val deleteBtn = GButton(STRINGS.ui.delete)
        private val saveBtn = GButton(STRINGS.ui.save)
        private val closeBtn = GButton(STRINGS.ui.close)
        private val model = DefaultListModel<CustomElement>()

        init {
            jList = JList<CustomElement>()
            jList.model = model
            jList.addListSelectionListener(getListSelectionListener())
            jList.addMouseListener(getListMouseListener())
            jList.addKeyListener(getListKeyListener())
            jList.cellRenderer = CustomCellRenderer()

            val componentListener: ComponentListener = object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    jList.fixedCellHeight = 10
                    jList.fixedCellHeight = -1
                }
            }

            jList.addComponentListener(componentListener)
            scrollPane = JScrollPane(jList)
            scrollPane.preferredSize = Dimension(800, 500)
            firstBtn.addActionListener(this)
            prevBtn.addActionListener(this)
            nextBtn.addActionListener(this)
            lastBtn.addActionListener(this)

            newBtn.addActionListener(this)
            copyBtn.addActionListener(this)
            editBtn.addActionListener(this)
            deleteBtn.addActionListener(this)
            saveBtn.addActionListener(this)
            closeBtn.addActionListener(this)
            val bottomPanel = JPanel()
            bottomPanel.add(firstBtn)
            bottomPanel.add(prevBtn)
            bottomPanel.add(nextBtn)
            bottomPanel.add(lastBtn)
            addVSeparator(bottomPanel)
            bottomPanel.add(newBtn)
            bottomPanel.add(copyBtn)
            bottomPanel.add(editBtn)
            bottomPanel.add(deleteBtn)
            addVSeparator(bottomPanel)
            bottomPanel.add(saveBtn)
            bottomPanel.add(closeBtn)

            contentPane.layout = BorderLayout()
            contentPane.add(scrollPane, BorderLayout.CENTER)
            contentPane.add(bottomPanel, BorderLayout.SOUTH)

            pack()

            Utils.installKeyStrokeEscClosing(this)
        }

        private fun addVSeparator(panel: JPanel) {
            val separator1 = JSeparator(SwingConstants.VERTICAL)
            separator1.preferredSize = Dimension(separator1.preferredSize.width, 20)
            panel.add(separator1)
        }

        fun initDialog() {
            val customListArray = loadList()
            model.clear()
            firstElement = getFirstElement()
            model.addElement(firstElement)

            customListArray.forEach(model::addElement)
        }

        inner class CustomCellRenderer : ListCellRenderer<Any?> {
            private val cellPanel: JPanel = JPanel(BorderLayout())
            private val titlePanel: JPanel = JPanel(BorderLayout())
            private val titleLabel: JLabel = JLabel("")
            private val valueTA: JTextArea = JTextArea()

            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?, index: Int, isSelected: Boolean,
                hasFocus: Boolean
            ): Component {
                val element = value as CustomElement
                titleLabel.text = element.title
                if (firstElement != null && firstElement!!.title == element.title) {
                    if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                        titleLabel.foreground = Color(0xC05050)
                    } else {
                        titleLabel.foreground = Color(0x900000)
                    }
                } else if (element.tableBar) {
                    titleLabel.text += " - TableBar"
                    if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                        titleLabel.foreground = Color(0x50C050)
                    } else {
                        titleLabel.foreground = Color(0x009000)
                    }
                } else {
                    if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                        titleLabel.foreground = Color(0x7070E0)
                    } else {
                        titleLabel.foreground = Color(0x000090)
                    }
                }
                valueTA.text = element.value

                valueTA.updateUI()

                if (isSelected) {
                    if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                        titlePanel.background = Color(0x56595B)
                        valueTA.background = Color(0x56595B)
                    } else {
                        titlePanel.background = Color.LIGHT_GRAY
                        valueTA.background = Color.LIGHT_GRAY
                    }
                } else {
                    if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                        titlePanel.background = Color(0x46494B)
                        valueTA.background = Color(0x46494B)
                    } else {
                        titlePanel.background = Color.WHITE
                        valueTA.background = Color.WHITE
                    }
                }
                return cellPanel
            }

            init {
                titleLabel.foreground = Color(0x000090)
                titleLabel.font = titleLabel.font.deriveFont(titleLabel.font.style or Font.BOLD)
                titlePanel.add(titleLabel, BorderLayout.NORTH)
                cellPanel.add(titlePanel, BorderLayout.NORTH)

                valueTA.lineWrap = true
                valueTA.wrapStyleWord = true
                cellPanel.add(valueTA, BorderLayout.CENTER)
            }
        }

        override fun actionPerformed(event: ActionEvent) {
            if (event.source == firstBtn) {
                val startIdx = if (firstElement == null) 0 else 1
                jList.valueIsAdjusting = true
                val selectedIdx = jList.selectedIndex
                if (model.size >= (3 - startIdx)) {
                    val selection = jList.selectedValue
                    if (firstElement == null || firstElement!!.title != selection.title) {
                        model.remove(selectedIdx)
                        model.add(startIdx, selection)
                        jList.selectedIndex = startIdx
                    }
                }
                jList.valueIsAdjusting = false
            } else if (event.source == prevBtn) {
                val startIdx = if (firstElement == null) 0 else 1
                jList.valueIsAdjusting = true
                val selectedIdx = jList.selectedIndex
                if (model.size >= (3 - startIdx) && selectedIdx > startIdx) {
                    val selection = jList.selectedValue
                    if (firstElement == null || firstElement!!.title != selection.title) {
                        model.remove(selectedIdx)
                        model.add(selectedIdx - 1, selection)
                        jList.selectedIndex = selectedIdx - 1
                    }
                }
                jList.valueIsAdjusting = false
            } else if (event.source == nextBtn) {
                val startIdx = if (firstElement == null) 0 else 1
                jList.valueIsAdjusting = true
                val selectedIdx = jList.selectedIndex
                if (model.size >= (3 - startIdx) && selectedIdx >= startIdx && selectedIdx < (model.size() - 1)) {
                    val selection = jList.selectedValue
                    if (firstElement == null || firstElement!!.title != selection.title) {
                        model.remove(selectedIdx)
                        model.add(selectedIdx + 1, selection)
                        jList.selectedIndex = selectedIdx + 1
                    }
                }
                jList.valueIsAdjusting = false
            } else if (event.source == lastBtn) {
                val startIdx = if (firstElement == null) 0 else 1
                jList.valueIsAdjusting = true
                val selectedIdx = jList.selectedIndex
                if (model.size >= (3 - startIdx)) {
                    val selection = jList.selectedValue
                    if (firstElement == null || firstElement!!.title != selection.title) {
                        model.remove(selectedIdx)
                        model.add(model.size(), selection)
                        jList.selectedIndex = model.size() - 1
                    }
                }
                jList.valueIsAdjusting = false
            } else if (event.source == newBtn) {
                val editDialog = EditDialog(this, CMD_NEW, "", "", false)
                editDialog.setLocationRelativeTo(this)
                editDialog.isVisible = true
            } else if (event.source == copyBtn) {
                if (jList.selectedIndex >= 0) {
                    val selection = jList.selectedValue
                    val editDialog = EditDialog(this, CMD_COPY, selection.title, selection.value, selection.tableBar)
                    editDialog.setLocationRelativeTo(this)
                    editDialog.isVisible = true
                }
            } else if (event.source == editBtn) {
                if (jList.selectedIndex >= 0) {
                    val selection = jList.selectedValue
                    val cmd = if (firstElement == null || firstElement!!.title != selection.title) {
                        CMD_EDIT
                    } else {
                        CMD_COPY
                    }
                    val editDialog = EditDialog(this, cmd, selection.title, selection.value, selection.tableBar)
                    editDialog.setLocationRelativeTo(this)
                    editDialog.isVisible = true
                }
            } else if (event.source == deleteBtn) {
                if (jList.selectedIndex >= 0) {
                    jList.valueIsAdjusting = true
                    val selectedIdx = jList.selectedIndex
                    model.remove(jList.selectedIndex)
                    if (selectedIdx > 0) {
                        jList.selectedIndex = selectedIdx - 1
                    }
                    jList.valueIsAdjusting = false
                }
            } else if (event.source == saveBtn) {
                val customListArray = ArrayList<CustomElement>()
                for (item in model.elements()) {
                    if (firstElement == null || firstElement!!.title != item.title) {
                        customListArray.add(item)
                    }
                }

                saveList(customListArray)

                logPanel.updateTableBar(customListArray)
            } else if (event.source == closeBtn) {
                dispose()
            }
        }

        private fun updateElement(cmd: Int, prevTitle: String, element: CustomElement) {
            if (cmd == CMD_EDIT) {
                for (item in model.elements()) {
                    if (item.title == title) {
                        item.value = element.value
                        return
                    }
                }
                jList.valueIsAdjusting = true
                val selectedIdx = jList.selectedIndex
                model.remove(selectedIdx)
                model.add(selectedIdx, element)
                jList.selectedIndex = selectedIdx
                jList.valueIsAdjusting = false
            } else {
                model.addElement(element)
            }
        }

        internal inner class EditDialog(
            private var parent: CustomDialog,
            private var cmd: Int,
            title: String,
            value: String,
            tableBar: Boolean
        ) : JDialog(parent, "Edit", true), ActionListener {
            private var okBtn: JButton = GButton(STRINGS.ui.ok)
            private var cancelBtn: JButton = GButton(STRINGS.ui.cancel)
            private var titleLabel: JLabel = JLabel("Title")
            private var valueLabel: JLabel = JLabel("Value")
            private var tableBarLabel: JLabel = JLabel("Add TableBar")
            private var titleTF: JTextField = JTextField(title)
            private var valueTF: JTextField = JTextField(value)
            private var tableBarCheck: JCheckBox = JCheckBox()
            private var titleStatusLabel: JLabel = JLabel("Good")
            private var valueStatusLabel: JLabel = JLabel("Good")

            private var prevTitle = title

            init {
                okBtn.addActionListener(this)
                cancelBtn.addActionListener(this)

                titleLabel.preferredSize = Dimension(50, 30)
                valueLabel.preferredSize = Dimension(50, 30)

                titleTF.document.addDocumentListener(TitleDocumentHandler())
                titleTF.preferredSize = Dimension(488, 30)
                valueTF.document.addDocumentListener(ValueDocumentHandler())
                valueTF.preferredSize = Dimension(488, 30)

                tableBarCheck.isSelected = tableBar


                val titleStatusPanel = JPanel(FlowLayout(FlowLayout.CENTER))
                titleStatusPanel.border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
                titleStatusPanel.add(titleStatusLabel)
                titleStatusPanel.preferredSize = Dimension(200, 30)

                val valueStatusPanel = JPanel(FlowLayout(FlowLayout.CENTER))
                valueStatusPanel.border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
                valueStatusPanel.add(valueStatusLabel)
                valueStatusPanel.preferredSize = Dimension(200, 30)


                val panel1 = JPanel(GridLayout(2, 1, 0, 2))
                panel1.add(titleLabel)
                panel1.add(valueLabel)

                val panel2 = JPanel(GridLayout(2, 1, 0, 2))
                panel2.add(titleTF)
                panel2.add(valueTF)

                val panel3 = JPanel(GridLayout(2, 1, 0, 2))
                panel3.add(titleStatusPanel)
                panel3.add(valueStatusPanel)

                val titleValuePanel = JPanel()
                titleValuePanel.add(panel1)
                titleValuePanel.add(panel2)
                titleValuePanel.add(panel3)

                val tableBarPanel = JPanel()
                tableBarPanel.add(tableBarLabel)
                tableBarPanel.add(tableBarCheck)

                val confirmPanel = JPanel()
                confirmPanel.preferredSize = Dimension(400, 40)
                confirmPanel.add(okBtn)
                confirmPanel.add(cancelBtn)

                val panel = JPanel()
                panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
                panel.add(titleValuePanel)
                panel.add(tableBarPanel)
                panel.add(confirmPanel)

                contentPane.add(panel)
                pack()

                var isValid = true
                if (titleTF.text.trim().isEmpty()) {
                    titleStatusLabel.text = "Empty"
                    isValid = false
                } else if (firstElement != null && firstElement!!.title == titleTF.text.trim()) {
                    titleStatusLabel.text = "Not allow : ${firstElement!!.title}"
                    isValid = false
                } else if (cmd == CMD_COPY) {
                    titleStatusLabel.text = "Copy : duplicated"
                    isValid = false
                }

                updateTitleStatusLabelForeground(isValid)

                if (valueTF.text.trim().isEmpty()) {
                    if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                        valueStatusLabel.foreground = Color(0xC07070)
                    } else {
                        valueStatusLabel.foreground = Color.RED
                    }
                    valueStatusLabel.text = "Empty"
                    isValid = false
                } else {
                    if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                        valueStatusLabel.foreground = Color(0x7070C0)
                    } else {
                        valueStatusLabel.foreground = Color.BLUE
                    }
                }

                okBtn.isEnabled = isValid

                Utils.installKeyStrokeEscClosing(this)
            }

            override fun actionPerformed(e: ActionEvent) {
                if (e.source == okBtn) {
                    parent.updateElement(
                        cmd,
                        prevTitle,
                        CustomElement(titleTF.text, valueTF.text, tableBarCheck.isSelected)
                    )
                    dispose()
                } else if (e.source == cancelBtn) {
                    dispose()
                }
            }

            internal inner class TitleDocumentHandler : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) {
                    checkText()
                }

                override fun removeUpdate(event: DocumentEvent) {
                    checkText()
                }

                override fun changedUpdate(event: DocumentEvent) {
                    checkText()
                }

                private fun checkText() {
                    var isValid = true
                    val title = titleTF.text.trim()
                    if (title.isEmpty()) {
                        titleStatusLabel.text = "Empty"
                        isValid = false
                    } else if (firstElement != null && firstElement!!.title == title) {
                        titleStatusLabel.text = "Not allow : ${firstElement!!.title}"
                        isValid = false
                    } else {
                        for (item in model.elements()) {
                            if (item.title == title) {
                                if (cmd != CMD_EDIT || (cmd == CMD_EDIT && prevTitle != title)) {
                                    titleStatusLabel.text = "Duplicated"
                                    isValid = false
                                }
                                break
                            }
                        }
                    }

                    updateTitleStatusLabelForeground(isValid)

                    okBtn.isEnabled = titleStatusLabel.text == "Good" && valueStatusLabel.text == "Good"
                }
            }

            internal inner class ValueDocumentHandler : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) {
                    checkText()
                }

                override fun removeUpdate(e: DocumentEvent) {
                    checkText()
                }

                override fun changedUpdate(e: DocumentEvent) {
                    checkText()
                }

                private fun checkText() {
                    var isValid = true

                    val value = valueTF.text.trim()
                    if (value.isEmpty()) {
                        valueStatusLabel.text = "Empty"
                        isValid = false
                    }

                    updateTitleStatusLabelForeground(isValid)

                    okBtn.isEnabled = titleStatusLabel.text == "Good" && valueStatusLabel.text == "Good"
                }
            }

            fun updateTitleStatusLabelForeground(isValid: Boolean) {
                if (isValid) {
                    if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                        titleStatusLabel.foreground = Color(0x7070C0)
                    } else {
                        titleStatusLabel.foreground = Color.BLUE
                    }
                } else {
                    if (Theme.isDark(ThemeSettings.getInstance().theme)) {
                        titleStatusLabel.foreground = Color(0xC07070)
                    } else {
                        titleStatusLabel.foreground = Color.RED
                    }
                }
            }
        }
    }
}
