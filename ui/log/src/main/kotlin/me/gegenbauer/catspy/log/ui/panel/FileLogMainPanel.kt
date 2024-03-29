package me.gegenbauer.catspy.log.ui.panel

import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.combobox.filterComboBox
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.rebuild
import java.io.File
import javax.swing.JOptionPane
import javax.swing.TransferHandler

class FileLogMainPanel: BaseLogMainPanel() {
    override val tag: String = "FileLogMainPanel"
    override val processFilterToggle = ColorToggleButton(GlobalStrings.PID, STRINGS.toolTip.pidToggle)
    override val processFilterCombo = filterComboBox(tooltip = STRINGS.toolTip.pidToggle)

    override val currentPidFilter: FilterItem
        get() = if (logMainBinding.pidFilterEnabled.getValueNonNull()) {
            processFilterCombo.filterItem.rebuild(logMainBinding.filterMatchCaseEnabled.getValueNonNull())
        } else {
            FilterItem.EMPTY_ITEM
        }
    override val currentPackageFilter: FilterItem = FilterItem.EMPTY_ITEM

    private val pendingLogFiles = mutableListOf<File>()

    override fun bindProcessComponents(mainBinding: LogMainBinding) {
        mainBinding.apply {
            bindLogFilter(
                processFilterCombo,
                processFilterToggle,
                pidFilterSelectedIndex,
                pidFilterHistory,
                pidFilterEnabled,
                pidFilterCurrentContent,
                pidFilterErrorMessage
            )
        }
    }

    override fun createUI() {
        super.createUI()
        deviceCombo.isVisible = false
        saveBtn.isVisible = false
    }

    override fun bindDeviceComponents(mainBinding: LogMainBinding) {
        // do nothing
    }

    override fun afterTaskStateChanged(state: TaskUIState) {
        if (state is TaskIdle) {
            startBtn.isEnabled = false
        }
    }

    fun pendingOpenFiles(files: List<File>) {
        if (isVisible) {
            handleFileImport(files)
        } else {
            pendingLogFiles.clear()
            pendingLogFiles.addAll(files)
        }
    }

    override fun onVisible() {
        super.onVisible()
        handleFileImport(pendingLogFiles)
    }

    private fun handleFileImport(files: List<File>) {
        pendingLogFiles.clear()
        if (files.isEmpty()) {
            return
        }

        fun openFileLog(files: List<File>) {
            files.firstOrNull()?.let { openFile(it.absolutePath) }
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

    private fun isLogEmpty(): Boolean {
        return fullTableModel.dataSize == 0
    }

    override fun isDataImportSupported(info: TransferHandler.TransferSupport): Boolean {
        return true
    }

    override fun handleDataImport(info: TransferHandler.TransferSupport): Boolean {
        handleFileImport(getDroppedFiles(info))
        return true
    }
}