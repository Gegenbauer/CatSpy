package me.gegenbauer.catspy.ui.dialog

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.SettingsContainer
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.configuration.SettingsManager.settings
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.ddmlib.adb.*
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceMonitor
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.DefaultDocumentListener
import me.gegenbauer.catspy.utils.ui.getDefaultFileChooser
import java.awt.Component
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileFilter

class AdbSettingsGroup(
    scope: CoroutineScope,
    container: SettingsContainer
): BaseSettingsGroup(STRINGS.ui.adb, scope, container) {

    private var adbPath: String = EMPTY_STRING
        set(value) {
            field = value
            ServiceManager.getContextService(AdamDeviceMonitor::class.java).configure(AdbConf(value))
        }

    private val adbStatusField = JTextField().apply { isEditable = false }

    private val currentAdbDir: File
        get() {
            val file = File(adbPath)
            return if (file.exists()) file.parentFile else File(".")
        }

    private val adbFileFilter = object : FileFilter() {
        override fun accept(f: File): Boolean {
            return f.isDirectory || currentPlatform.adbExecutable == f.name
        }

        override fun getDescription(): String {
            return STRINGS.ui.adbFileDescription
        }
    }

    override fun initGroup() {
        adbPath = SettingsManager.adbPath
        addRow(STRINGS.ui.adbPath, buildAdbPathConfigurePanel())
        addRow(STRINGS.ui.adbServerStatus, buildAdbStatusPanel())
        end()
    }

    private fun buildAdbPathConfigurePanel(): JPanel {
        val adbPathField = JTextField(SettingsManager.adbPath)
        adbPathField.document.addDocumentListener(object : DefaultDocumentListener() {
            override fun contentUpdate(content: String) {
                adbPath = content
                settings.adbPath = content
            }
        })
        val adbPathSelectBtn = JButton(STRINGS.ui.change)
        adbPathSelectBtn.addActionListener {
            val fileChooser = getDefaultFileChooser()
            fileChooser.currentDirectory = currentAdbDir
            fileChooser.dialogTitle = STRINGS.ui.selectAdbPath
            fileChooser.isAcceptAllFileFilterUsed = false
            fileChooser.fileFilter = adbFileFilter
            val result = fileChooser.showOpenDialog(container as Component)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                adbPathField.text = file.absolutePath
                SettingsManager.updateSettings {
                    this.adbPath = file.absolutePath
                    this@AdbSettingsGroup.adbPath = file.absolutePath
                }
            }
        }
        val checkBtn = JButton(STRINGS.ui.startAdbServer)
        checkBtn.addActionListener {
            scope.launch {
                checkBtn.isEnabled = false
                SettingsManager.updateSettings { adbPath = adbPathField.text}
                val result = withContext(Dispatchers.GIO) {
                    startServer(AdbConf(adbPathField.text))
                }
                informAdbServerStartResult(result)
                updateAdbStatus(isServerRunning())
                checkBtn.isEnabled = true
            }
        }
        val panel = JPanel()
        val layout = TableLayout(
            doubleArrayOf(0.8, PREFERRED, PREFERRED),
            doubleArrayOf(PREFERRED)
        )
        layout.hGap = 10
        panel.layout = layout

        panel.add(adbPathField, "0,0")
        panel.add(adbPathSelectBtn, "1,0")
        panel.add(checkBtn, "2,0")
        return panel
    }

    private fun buildAdbStatusPanel(): JPanel {
        val panel = JPanel()

        val layout = TableLayout(
            doubleArrayOf(0.8, PREFERRED),
            doubleArrayOf(PREFERRED)
        )
        layout.hGap = 10
        panel.layout = layout

        val refreshAdbServerStatusBtn = JButton(STRINGS.ui.refresh)
        refreshAdbServerStatusBtn.addActionListener {
            refreshAdbStatus()
        }

        adbStatusField.parent?.removeAll()
        panel.add(adbStatusField, "0,0")
        panel.add(refreshAdbServerStatusBtn, "1,0")

        checkAdbStatus()
        return panel
    }

    private fun checkAdbStatus() {
        scope.launch {
            val adbStarted = isServerRunning()
            withContext(Dispatchers.UI) {
                updateAdbStatus(adbStarted)
            }
        }
    }

    private fun refreshAdbStatus() {
        scope.launch {
            val status = isServerRunning()
            updateAdbStatus(status)
            val adbStatus = if (status) STRINGS.ui.adbServerStarted else STRINGS.ui.adbServerNotStarted
            JOptionPane.showMessageDialog(
                container as Component,
                adbStatus,
                EMPTY_STRING,
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    private fun updateAdbStatus(status: Boolean) {
        val adbStatus = if (status) STRINGS.ui.adbServerStarted else STRINGS.ui.adbServerNotStarted
        adbStatusField.text = adbStatus
    }

    private fun informAdbServerStartResult(result: AdbServerStartResult) {
        if (result.isSuccess) {
            JOptionPane.showMessageDialog(
                container as Component,
                STRINGS.ui.adbPathValid,
                EMPTY_STRING,
                JOptionPane.INFORMATION_MESSAGE
            )
        } else {
            val message = when (result) {
                AdbServerStartResult.FAILURE_WRONG_EXECUTABLE -> STRINGS.ui.adbPathInvalid
                AdbServerStartResult.FAILURE_FILE_NOT_EXIST -> STRINGS.ui.adbPathNotExist
                AdbServerStartResult.FAILURE_ADB_PATH_UNSPECIFIED -> STRINGS.ui.adbPathEmpty
                else -> STRINGS.ui.adbPathInvalid
            }
            JOptionPane.showMessageDialog(
                container as Component,
                message,
                EMPTY_STRING,
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}