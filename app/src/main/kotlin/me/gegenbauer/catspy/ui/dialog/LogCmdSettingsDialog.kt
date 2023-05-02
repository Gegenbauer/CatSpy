package me.gegenbauer.catspy.ui.dialog

import me.gegenbauer.catspy.command.LogCmdManager
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.resource.strings.app
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.addHSeparator
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.utils.Utils
import me.gegenbauer.catspy.utils.isDoubleClick
import java.awt.*
import java.awt.event.*
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel


// TODO refactor
class LogCmdSettingsDialog(parent: MainUI) :JDialog(parent, "${STRINGS.ui.logCmd} ${STRINGS.ui.setting}", true), ActionListener {
    private val adbCmdBtn: JButton = GButton(STRINGS.ui.select)
    private val adbSaveBtn: JButton = GButton(STRINGS.ui.select)
    private val okBtn: JButton = GButton(STRINGS.ui.ok)
    private val cancelBtn: JButton = GButton(STRINGS.ui.cancel)
    private val adbCmdLabel: JLabel = JLabel(STRINGS.ui.adbPath)
    private val adbSaveLabel: JLabel = JLabel(STRINGS.ui.logPath)
    private val prefixLabel: JLabel = JLabel("Prefix")
    private val prefixLabel2: JLabel = JLabel("Default : ${STRINGS.ui.app}, Do not use \\ / : * ? \" < > |")
    private val adbCmdTF: JTextField = JTextField(LogCmdManager.adbCmd)
    private val adbSaveTF: JTextField = JTextField(LogCmdManager.logSavePath)
    private val prefixTF: JTextField = JTextField(LogCmdManager.prefix)
    private val logCmdLabel1: JLabel = JLabel("Double click to edit")
    private val logCmdLabel2: JLabel = JLabel("Default : ${STRINGS.ui.app} -c")
    private val logCmdTableModel: LogCmdTableModel
    private val logCmdTable: JTable

    inner class LogCmdTableModel(logCommands: Array<Array<Any>>, columnNames: Array<String>) : DefaultTableModel(logCommands, columnNames) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }

    inner class LogCmdMouseHandler : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.isDoubleClick) {
                if (logCmdTable.selectedRow > 0) {
                    val logCmdDialog = LogCmdDialog(this@LogCmdSettingsDialog)
                    logCmdDialog.setLocationRelativeTo(this@LogCmdSettingsDialog)
                    logCmdDialog.isVisible = true
                }
            }
            super.mouseClicked(e)
        }
    }

    private val mainUI = parent

    init {
        val rowHeight = 30
        adbCmdBtn.preferredSize = Dimension(adbCmdBtn.preferredSize.width, rowHeight)
        adbSaveBtn.addActionListener(this)
        okBtn.addActionListener(this)
        cancelBtn.addActionListener(this)

        adbCmdLabel.preferredSize = Dimension(adbCmdLabel.preferredSize.width, rowHeight)

        adbCmdTF.preferredSize = Dimension(488, rowHeight)
        adbSaveTF.preferredSize = Dimension(488, rowHeight)
        prefixTF.preferredSize = Dimension(300, rowHeight)

        val columnNames = arrayOf("Num", "Cmd")

        val logCommands = arrayOf(
                arrayOf<Any>("1(fixed)", LogCmdManager.DEFAULT_LOGCAT),
                arrayOf<Any>("2", ""),
                arrayOf<Any>("3", ""),
                arrayOf<Any>("4", ""),
                arrayOf<Any>("5", ""),
                arrayOf<Any>("6", ""),
                arrayOf<Any>("7", ""),
                arrayOf<Any>("8", ""),
                arrayOf<Any>("9", ""),
                arrayOf<Any>("10", ""),
        )

        UIConfManager.uiConf.logCmdHistory.forEachIndexed { index, cmd ->
            logCommands[index][1] = cmd
        }

        logCmdTableModel = LogCmdTableModel(logCommands, columnNames)
        logCmdTable = JTable(logCmdTableModel)
        logCmdTable.preferredSize = Dimension(488, 200)
        logCmdTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        logCmdTable.showHorizontalLines = true
        logCmdTable.showVerticalLines = true
        val renderer = DefaultTableCellRenderer()
        renderer.horizontalAlignment = JLabel.CENTER
        logCmdTable.columnModel.getColumn(0).cellRenderer = renderer
        logCmdTable.addMouseListener(LogCmdMouseHandler())

        logCmdTableModel.rowCount = LogCmdManager.LOG_CMD_MAX
        logCmdTable.columnModel.getColumn(0).preferredWidth = 70
        logCmdTable.columnModel.getColumn(1).preferredWidth = 330

        logCmdLabel1.preferredSize = Dimension(488, logCmdLabel1.preferredSize.height)
        logCmdLabel2.preferredSize = Dimension(488, logCmdLabel2.preferredSize.height)

        val panel1 = JPanel(GridLayout(4, 1, 0, 2))
        panel1.add(adbCmdLabel)
        panel1.add(adbSaveLabel)
        panel1.add(prefixLabel)

        val panel2 = JPanel(GridLayout(4, 1, 0, 2))
        panel2.add(adbCmdTF)
        panel2.add(adbSaveTF)
        panel2.add(prefixTF)
        panel2.add(prefixLabel2)

        val panel3 = JPanel(GridLayout(4, 1, 0, 2))
        panel3.add(adbCmdBtn)
        panel3.add(adbSaveBtn)

        val cmdPathPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        cmdPathPanel.add(panel1)
        cmdPathPanel.add(panel2)
        cmdPathPanel.add(panel3)

        val pathPanel = JPanel()
        pathPanel.layout = BoxLayout(pathPanel, BoxLayout.Y_AXIS)
        addHSeparator(pathPanel, "ADB " + STRINGS.ui.setting)
        pathPanel.add(cmdPathPanel, BorderLayout.NORTH)

        val cmdPanel = JPanel(BorderLayout())
        cmdPanel.add(pathPanel, BorderLayout.NORTH)

        val logCmdTablePanel = JPanel()
        logCmdTablePanel.add(logCmdTable)

        val logCmdLabel1Panel = JPanel()
        logCmdLabel1Panel.add(logCmdLabel1)

        val logCmdLabel2Panel = JPanel()
        logCmdLabel2Panel.add(logCmdLabel2)

        val logCmdPanel = JPanel()
        logCmdPanel.layout = BoxLayout(logCmdPanel, BoxLayout.Y_AXIS)
        addHSeparator(logCmdPanel, STRINGS.ui.logCmd)
        logCmdPanel.add(logCmdTablePanel)
        logCmdPanel.add(logCmdLabel1Panel)
        logCmdPanel.add(logCmdLabel2Panel)

        cmdPanel.add(logCmdPanel, BorderLayout.CENTER)

        val confirmPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        confirmPanel.preferredSize = Dimension(400, 40)
        confirmPanel.alignmentX = JPanel.RIGHT_ALIGNMENT
        confirmPanel.add(okBtn)
        confirmPanel.add(cancelBtn)

        val panel = JPanel(BorderLayout())
        panel.add(cmdPanel, BorderLayout.CENTER)
        panel.add(confirmPanel, BorderLayout.SOUTH)

        contentPane.add(panel)
        pack()

        Utils.installKeyStrokeEscClosing(this)
    }

    override fun actionPerformed(e: ActionEvent) {
        if (e.source == adbCmdBtn) {
            val fileDialog = FileDialog(this@LogCmdSettingsDialog, "Adb command", FileDialog.LOAD)
            fileDialog.isVisible = true
            if (fileDialog.file != null) {
                val file = File(fileDialog.directory + fileDialog.file)
                GLog.d(TAG, "adb command : ${file.absolutePath}")
                adbCmdTF.text = file.absolutePath
            } else {
                GLog.d(TAG, "Cancel Open")
            }
        } else if (e.source == adbSaveBtn) {
            val chooser = JFileChooser()
            chooser.currentDirectory = File(".")
            chooser.dialogTitle = "Adb Save Dir"
            chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            chooser.isAcceptAllFileFilterUsed = false

            if (chooser.showOpenDialog(this@LogCmdSettingsDialog) == JFileChooser.APPROVE_OPTION) {
                GLog.d(TAG, "getSelectedFile() : ${chooser.selectedFile}")
                adbSaveTF.text = chooser.selectedFile.absolutePath
            } else {
                GLog.d(TAG, "No Selection ")
            }
        } else if (e.source == okBtn) {
            LogCmdManager.adbCmd = adbCmdTF.text
            LogCmdManager.logSavePath = adbSaveTF.text
            val prefix = prefixTF.text.trim()

            if (prefix.contains('\\')
                    || prefix.contains('/')
                    || prefix.contains(':')
                    || prefix.contains('*')
                    || prefix.contains('?')
                    || prefix.contains('"')
                    || prefix.contains("<")
                    || prefix.contains(">")
                    || prefix.contains("|")) {
                JOptionPane.showMessageDialog(this, "Invalid prefix : ${prefixTF.text}", "Error", JOptionPane.ERROR_MESSAGE)
                return
            }

            if (prefix.isEmpty()) {
                LogCmdManager.prefix = STRINGS.ui.app
            } else {
                LogCmdManager.prefix = prefix
            }

            UIConfManager.uiConf.logCmdHistory.clear()
            for (idx in 0 until logCmdTable.rowCount) {
                UIConfManager.uiConf.logCmdHistory.add(logCmdTable.getValueAt(idx, 1).toString())
            }

            UIConfManager.uiConf.adbLogSavePath = LogCmdManager.logSavePath
            UIConfManager.uiConf.adbLogCommand = LogCmdManager.logCmd
            UIConfManager.uiConf.adbCommand = LogCmdManager.adbCmd
            UIConfManager.uiConf.adbPrefix = LogCmdManager.prefix
            mainUI.updateLogCmdCombo()
            UIConfManager.saveUI()
            dispose()
        } else if (e.source == cancelBtn) {
            dispose()
        }
    }

    inner class LogCmdDialog(parent: JDialog) :JDialog(parent, STRINGS.ui.logCmd, true), ActionListener, FocusListener {
        private var adbRadio: JRadioButton
        private var cmdRadio: JRadioButton

        private var adbTF: JTextField
        private var cmdTF: JTextField

        private var cmdBtn: JButton

        private var okBtn: JButton
        private var cancelBtn: JButton

        init {
            val rowHeight = 30
            adbRadio = JRadioButton(STRINGS.ui.adb)
            adbRadio.preferredSize = Dimension(60, rowHeight)
            cmdRadio = JRadioButton(STRINGS.ui.cmd)

            val buttonGroup = ButtonGroup()
            buttonGroup.add(adbRadio)
            buttonGroup.add(cmdRadio)

            adbTF = JTextField()
            adbTF.preferredSize = Dimension(488, rowHeight)
            adbTF.addFocusListener(this)
            cmdTF = JTextField()
            cmdTF.addFocusListener(this)

            val initCmd = logCmdTable.getValueAt(logCmdTable.selectedRow, 1) as String?
            if (initCmd?.startsWith(LogCmdManager.TYPE_CMD_PREFIX) == true) {
                cmdTF.text = initCmd.substring(LogCmdManager.TYPE_CMD_PREFIX_LEN)
                cmdRadio.isSelected = true
            } else {
                adbTF.text = initCmd
                adbRadio.isSelected = true
            }

            cmdBtn = GButton(STRINGS.ui.select)
            cmdBtn.addActionListener(this)
            cmdBtn.preferredSize = Dimension(cmdBtn.preferredSize.width, rowHeight)

            okBtn = GButton(STRINGS.ui.ok)
            okBtn.addActionListener(this)
            cancelBtn = GButton(STRINGS.ui.cancel)
            cancelBtn.addActionListener(this)

            val panel1 = JPanel(GridLayout(2, 1, 0, 2))
            panel1.add(adbRadio)
            panel1.add(cmdRadio)

            val panel2 = JPanel(GridLayout(2, 1, 0, 2))
            panel2.add(adbTF)
            panel2.add(cmdTF)

            val panel3 = JPanel(GridLayout(2, 1, 0, 2))
            panel3.add(JPanel())
            panel3.add(cmdBtn)

            val cmdPathPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            cmdPathPanel.add(panel1)
            cmdPathPanel.add(panel2)
            cmdPathPanel.add(panel3)

            val pathPanel = JPanel()
            pathPanel.layout = BoxLayout(pathPanel, BoxLayout.Y_AXIS)
            pathPanel.add(cmdPathPanel, BorderLayout.NORTH)

            val cmdPanel = JPanel(BorderLayout())
            cmdPanel.add(pathPanel, BorderLayout.NORTH)

            val confirmPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
            confirmPanel.preferredSize = Dimension(400, 40)
            confirmPanel.alignmentX = JPanel.RIGHT_ALIGNMENT
            confirmPanel.add(okBtn)
            confirmPanel.add(cancelBtn)

            val panel = JPanel(BorderLayout())
            panel.add(cmdPanel, BorderLayout.CENTER)
            panel.add(confirmPanel, BorderLayout.SOUTH)

            contentPane.add(panel)
            pack()

            Utils.installKeyStrokeEscClosing(this)
        }

        override fun actionPerformed(e: ActionEvent) {
            if (e.source == cmdBtn) {
                val fileDialog = FileDialog(this@LogCmdDialog, STRINGS.ui.cmd, FileDialog.LOAD)
                fileDialog.isVisible = true
                if (fileDialog.file != null) {
                    val file = File(fileDialog.directory + fileDialog.file)
                    GLog.d(TAG, "command : ${file.absolutePath}")
                    cmdTF.text = file.absolutePath
                } else {
                    GLog.d(TAG, "Cancel Open")
                }
            } else if (e.source == okBtn) {
                val text = if (cmdRadio.isSelected) {
                    if (cmdTF.text.isNotEmpty()) {
                        "${LogCmdManager.TYPE_CMD_PREFIX}${cmdTF.text}"
                    } else {
                        ""
                    }
                } else {
                    adbTF.text
                }
                logCmdTable.setValueAt(text, logCmdTable.selectedRow, 1)
                dispose()
            } else if (e.source == cancelBtn) {
                dispose()
            }
        }

        override fun focusGained(e: FocusEvent) {
            if (e.source == adbTF) {
                adbRadio.isSelected = true
            }
            else if (e.source == cmdTF) {
                cmdRadio.isSelected = true
            }
        }

        override fun focusLost(e: FocusEvent) {
            // do nothing
        }
    }

    companion object {
        private const val TAG = "LogCmdSettingsDialog"
    }
}
