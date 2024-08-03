package me.gegenbauer.catspy.log.ui.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.recent.RecentLogFiles
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.log.serialize.toLogMetadata
import me.gegenbauer.catspy.log.ui.customize.LogMetadataTemplateComboBox
import me.gegenbauer.catspy.log.ui.customize.OnMetadataChangedListener
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.persistence.Preferences
import me.gegenbauer.catspy.utils.ui.Key
import me.gegenbauer.catspy.utils.ui.registerStroke
import me.gegenbauer.catspy.utils.ui.showWarningDialog
import me.gegenbauer.catspy.view.panel.StatusBar
import java.awt.Component
import java.io.File
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.TransferHandler

open class FileLogMainPanel : BaseLogMainPanel() {
    override val tag: String = "FileLogMainPanel"

    protected val templateSelector = LogMetadataTemplateComboBox()

    private val filePopupMenu = FileOpenPopupMenu().apply {
        onFileSelected = { file ->
            openFile(file.absolutePath)
        }
    }
    private val logMetadataChangeListener = object : OnMetadataChangedListener {
        override fun onLogMetadataSelected(metadata: LogMetadataModel) {
            changeLogMetadata(metadata, true)
        }

        override fun onSelectedMetadataModified(modifiedMetadata: LogMetadataModel) {
            // parser of built-in metadata cannot be modified and doesn't need to reload file
            val needReloadFile = modifiedMetadata.isBuiltIn.not() && showReloadLogMetadataWarningDialog()
            changeLogMetadata(modifiedMetadata, needReloadFile)
        }
    }

    private val pendingLogFiles = mutableListOf<File>()

    private var currentLogFile = ""

    override fun onInitialMetadataAcquired(metadata: LogMetadata) {
        super.onInitialMetadataAcquired(metadata)
        templateSelector.selectMetadata(metadata.logType)
    }

    override fun getCustomToolbarComponents(): List<Component> {
        return listOf(templateSelector)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        filePopupMenu.setParent(this)
    }

    override fun registerEvent() {
        super.registerEvent()
        splitLogWithStatefulPanel.onClickOpen = {
            SwingUtilities.updateComponentTreeUI(filePopupMenu)
            filePopupMenu.onClickFileOpen()
        }
        templateSelector.addOnSelectedMetadataChangedListener(logMetadataChangeListener)
    }

    private fun changeLogMetadata(metadata: LogMetadataModel, needReloadFile: Boolean = false) {
        logConf.setLogMetadata(metadata.toLogMetadata())
        currentLogFile.takeIf { it.isNotEmpty() && needReloadFile }?.let { openFile(it) }
        configureLogTablePopupActions()
        Preferences.putString(LogMetadata.KEY, metadata.logType)
    }

    private fun showReloadLogMetadataWarningDialog(): Boolean {
        val actions = listOf(
            STRINGS.ui.load to { true },
            STRINGS.ui.cancel to { false }
        )

        return showWarningDialog(
            this,
            "",
            STRINGS.ui.reloadFileLogMetadataWarning,
            actions,
        )
    }

    override fun registerStrokes() {
        super.registerStrokes()
        registerStroke(Key.C_O, "Select Log File") { filePopupMenu.onClickFileOpen() }
    }

    private fun openFile(path: String) {
        stopAll()

        logViewModel.startProduceFileLog(path)

        GLog.d(tag, "[openFile] Opening: $path")
        updateLogFilter()

        currentLogFile = path
        RecentLogFiles.onNewFileOpen(path)
    }

    override fun afterTaskStateChanged(state: TaskUIState) {
        super.afterTaskStateChanged(state)
        when (state) {
            is TaskIdle -> {
                startBtn.isEnabled = false
                if (isLogTableEmpty) {
                    logStatus = StatusBar.LogStatusIdle(STRINGS.ui.open)
                } else {
                    logStatus = StatusBar.LogStatusIdle(STRINGS.ui.open, currentLogFile)
                }
            }

            is TaskStarted -> {
                logStatus = StatusBar.LogStatusRunning(STRINGS.ui.open, currentLogFile)
            }
        }
    }

    override fun afterLogStatusChanged(status: StatusBar.LogStatus) {
        super.afterLogStatusChanged(status)
        val fileName = status.path.substringAfterLast(File.separator)
        setTabName(fileName)
        setTabTooltip(status.path)
    }

    override fun clearAllLogs() {
        super.clearAllLogs()
        if (taskState is TaskIdle) {
            logStatus = StatusBar.LogStatus.NONE
            currentLogFile = ""
        }
    }

    override fun isDataImportSupported(info: TransferHandler.TransferSupport): Boolean {
        return true
    }

    override fun pendingOpenFiles(files: List<File>) {
        if (isVisible) {
            handleFileImport(files)
        } else {
            pendingLogFiles.clear()
            pendingLogFiles.addAll(files)
        }
    }

    override fun handleDataImport(info: TransferHandler.TransferSupport): Boolean {
        handleFileImport(getDroppedFiles(info))
        return true
    }

    private fun handleFileImport(files: List<File>) {
        pendingLogFiles.clear()
        if (files.isEmpty()) {
            return
        }

        fun openFileLog(files: List<File>) {
            files.firstOrNull()?.let { openFile(it.absolutePath) }
        }

        fun isLogEmpty(): Boolean {
            return fullTableModel.dataSize == 0
        }

        if (isLogEmpty()) {
            openFileLog(files)
            return
        }

        val options = listOf<Pair<String, (List<File>) -> Unit>>(
            STRINGS.ui.open to { openFileLog(it) },
            STRINGS.ui.cancel to { GLog.d(tag, "[onDragLogFile] select cancel") }
        )
        val value = JOptionPane.showOptionDialog(
            this, STRINGS.ui.dragLogFileWarning,
            "",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options.map { it.first }.toTypedArray(),
            STRINGS.ui.append
        )
        options[value].second.invoke(files)
    }
}