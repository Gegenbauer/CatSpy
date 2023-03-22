package me.gegenbauer.logviewer.ui.log

import me.gegenbauer.logviewer.Utils
import me.gegenbauer.logviewer.configuration.UIConfManager
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.manager.LogCmdManager
import me.gegenbauer.logviewer.strings.STRINGS
import me.gegenbauer.logviewer.strings.app
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.addHSeparator
import java.awt.*
import java.awt.event.*
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel


class LogCmdSettingsDialog(parent: MainUI) :JDialog(parent, "${STRINGS.ui.logCmd} ${STRINGS.ui.setting}", true), ActionListener {
    private var adbCmdBtn: JButton
    private var adbSaveBtn: JButton
    private var okBtn: JButton
    private var cancelBtn: JButton

    private var adbCmdLabel: JLabel
    private var adbSaveLabel: JLabel
    private var prefixLabel: JLabel
    private var prefixLabel2: JLabel

    private var adbCmdTF: JTextField
    private var adbSaveTF: JTextField
    private var prefixTF: JTextField

    private var logCmdTable: JTable
    private var logCmdTableModel: LogCmdTableModel
    private var logCmdLabel1: JLabel
    private var logCmdLabel2: JLabel

    inner class LogCmdTableModel(logCommands: Array<Array<Any>>, columnNames: Array<String>) : DefaultTableModel(logCommands, columnNames) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }

    inner class LogCmdMouseHandler : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e != null) {
                if (e.clickCount == 2) {
                    if (logCmdTable.selectedRow > 0) {
                        val logCmdDialog = LogCmdDialog(this@LogCmdSettingsDialog)
                        logCmdDialog.setLocationRelativeTo(this@LogCmdSettingsDialog)
                        logCmdDialog.isVisible = true
                    }
                }
            }
            super.mouseClicked(e)
        }
    }

    private val mainUI = parent

    init {
        val rowHeight = 30
        adbCmdBtn = JButton(STRINGS.ui.select)
        adbCmdBtn.addActionListener(this)
        adbCmdBtn.preferredSize = Dimension(adbCmdBtn.preferredSize.width, rowHeight)
        adbSaveBtn = JButton(STRINGS.ui.select)
        adbSaveBtn.addActionListener(this)
        okBtn = JButton(STRINGS.ui.ok)
        okBtn.addActionListener(this)
        cancelBtn = JButton(STRINGS.ui.cancel)
        cancelBtn.addActionListener(this)

        adbCmdLabel = JLabel(STRINGS.ui.adbPath)
        adbCmdLabel.preferredSize = Dimension(adbCmdLabel.preferredSize.width, rowHeight)
        adbSaveLabel = JLabel(STRINGS.ui.logPath)
        prefixLabel = JLabel("Prefix")
        prefixLabel2 = JLabel("Default : LogViewer, Do not use \\ / : * ? \" < > |")

        adbCmdTF = JTextField(LogCmdManager.adbCmd)
        adbCmdTF.preferredSize = Dimension(488, rowHeight)
        adbSaveTF = JTextField(LogCmdManager.logSavePath)
        adbSaveTF.preferredSize = Dimension(488, rowHeight)
        prefixTF = JTextField(LogCmdManager.prefix)
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

        for (idx in logCommands.indices) {
            val item = ConfigManager.getItem("${ConfigManager.ITEM_ADB_LOG_CMD}_$idx")
            if (idx != 0 && item != null) {
                logCommands[idx][1] = item
            }
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

        logCmdLabel1 = JLabel("<html><b><font color=\"#7070FF\">logcat -v threadtime</font></b> <br>&nbsp;&nbsp;&nbsp;&nbsp => RUN : <b><font color=\"#7070FF\">adb -s DEVICE logcat -v threadtime</font></b></html>")
        logCmdLabel1.preferredSize = Dimension(488, logCmdLabel1.preferredSize.height)
        logCmdLabel2 = JLabel("<html><b><font color=\"#7070FF\">${LogCmdManager.TYPE_CMD_PREFIX}cmdABC</font></b> <br>&nbsp;&nbsp;&nbsp;&nbsp => RUN : <b><font color=\"#7070FF\">cmdABC DEVICE</font></b></html>")
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

        val logCmdLable1Panel = JPanel()
        logCmdLable1Panel.add(logCmdLabel1)

        val logCmdLable2Panel = JPanel()
        logCmdLable2Panel.add(logCmdLabel2)

        val logCmdPanel = JPanel()
        logCmdPanel.layout = BoxLayout(logCmdPanel, BoxLayout.Y_AXIS)
        addHSeparator(logCmdPanel, STRINGS.ui.logCmd)
        logCmdPanel.add(logCmdTablePanel)
        logCmdPanel.add(logCmdLable1Panel)
        logCmdPanel.add(logCmdLable2Panel)

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

            prefixLabel2 = JLabel("Default : LogViewer, Do not use \\ / : * ? \" < > |")
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

            for (idx in 0 until logCmdTable.rowCount) {
                ConfigManager.setItem("${ConfigManager.ITEM_ADB_LOG_CMD}_$idx", logCmdTableModel.getValueAt(idx, 1).toString())
            }
            ConfigManager.saveConfig()


            UIConfManager.uiConf.adbLogSavePath = LogCmdManager.logSavePath
            UIConfManager.uiConf.adbLogCommand = LogCmdManager.logCmd
            UIConfManager.uiConf.adbCommand = LogCmdManager.adbCmd
            UIConfManager.uiConf.adbPrefix = LogCmdManager.prefix
            mainUI.updateLogCmdCombo(true)
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

            cmdBtn = JButton(STRINGS.ui.select)
            cmdBtn.addActionListener(this)
            cmdBtn.preferredSize = Dimension(cmdBtn.preferredSize.width, rowHeight)

            okBtn = JButton(STRINGS.ui.ok)
            okBtn.addActionListener(this)
            cancelBtn = JButton(STRINGS.ui.cancel)
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

        }
    }

    companion object {
        private const val TAG = "LogCmdSettingsDialog"
    }
}
