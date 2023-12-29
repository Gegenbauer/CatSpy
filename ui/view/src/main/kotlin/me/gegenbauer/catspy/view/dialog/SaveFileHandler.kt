package me.gegenbauer.catspy.view.dialog

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.context.Disposable
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import java.awt.Component
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane

class FileSaveHandler private constructor(
    private val onFileSpecified: suspend (File) -> Result<File?>,
    private val onCancel: () -> Unit = {},
    private val defaultName: String = "",
    private val parent: Component
): Disposable {

    private val fileChooser = JFileChooser()
    private val scope = MainScope()

    init {
        fileChooser.dialogTitle = STRINGS.ui.saveFileTitle
        fileChooser.isMultiSelectionEnabled = false
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.isAcceptAllFileFilterUsed = false
    }

    fun show() {
        checkAndSetDefaultFile()
        val result = fileChooser.showSaveDialog(parent)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            if (file != null) {
                onFileSpecified(file)
            }
        } else {
            onCancel()
        }
    }

    private fun checkAndSetDefaultFile() {
        val lastFileSaveDir = SettingsManager.settings.lastFileSaveDir
        if (lastFileSaveDir.isNotEmpty()) {
            fileChooser.currentDirectory = File(lastFileSaveDir)
        } else {
            fileChooser.currentDirectory = null
        }
        if (defaultName.isNotEmpty()) {
            fileChooser.selectedFile = File(fileChooser.currentDirectory, defaultName)
        }
    }

    private fun onFileSpecified(file: File) {
        scope.launch {
            if (file.exists()) {
                val result = JOptionPane.showConfirmDialog(
                    parent,
                    STRINGS.ui.fileExistsMessage.get(file.absolutePath),
                    STRINGS.ui.fileExistsTitle,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                )
                if (result == JOptionPane.YES_OPTION) {
                    val saveResult = onFileSpecified.invoke(file)
                    handleSaveResult(saveResult)
                } else {
                    onCancel()
                }
            } else {
                onFileSpecified.invoke(file)
                onFileSavedSuccess(file)
            }
        }
    }

    private fun handleSaveResult(result: Result<File?>) {
        when {
            result.exceptionOrNull() is CancellationException -> {
                onCancel()
            }
            result.isSuccess -> {
                onFileSavedSuccess(result.getOrNull())
            }
            result.isFailure -> {
                onFileSaveFailed(result.getOrNull(), result.exceptionOrNull()!!)
            }
        }
    }

    private fun onFileSavedSuccess(file: File?) {
        file ?: return
        GLog.d(TAG, "[onFileSaved] file=${file.absolutePath}")
        SettingsManager.settings.lastFileSaveDir = file.parent
        val result = JOptionPane.showOptionDialog(
            parent,
            STRINGS.ui.fileSaveSuccessMessage.get(file.absolutePath),
            STRINGS.ui.fileSaveTitle,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            arrayOf(STRINGS.ui.showFileInFileManager, STRINGS.ui.cancel),
            STRINGS.ui.showFileInFileManager
        )
        if (result == JOptionPane.OK_OPTION) {
            currentPlatform.showFileInExplorer(file)
        }
    }

    private fun onFileSaveFailed(file: File?, throwable: Throwable) {
        file ?: return
        GLog.d(TAG, "[onFileSaveFailed] file=${file.absolutePath}")
        val errorMsg = throwable.message ?: STRINGS.ui.unknownError
        JOptionPane.showMessageDialog(
            parent,
            STRINGS.ui.fileSaveErrorMessage.get(file.absolutePath, errorMsg),
            STRINGS.ui.saveFileTitle,
            JOptionPane.ERROR_MESSAGE
        )
    }

    override fun destroy() {
        scope.cancel()
    }

    class Builder(private val parent: Component) {
        private var onFileSpecified: suspend (File) -> Result<File?> = { Result.success(null) }
        private var onCancel: () -> Unit = {}
        private var defaultName: String = ""

        fun onFileSpecified(onFileSpecified: suspend (File) -> Result<File?>) = apply {
            this.onFileSpecified = onFileSpecified
        }

        fun onCancel(onCancel: () -> Unit) = apply {
            this.onCancel = onCancel
        }

        fun setDefaultName(defaultPath: String) = apply {
            this.defaultName = defaultPath
        }

        fun build() = FileSaveHandler(onFileSpecified, onCancel, defaultName, parent)
    }

    companion object {
        private const val TAG = "SaveFileHandler"
    }
}