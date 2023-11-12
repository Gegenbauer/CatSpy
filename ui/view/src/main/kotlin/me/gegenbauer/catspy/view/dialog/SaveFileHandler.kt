package me.gegenbauer.catspy.view.dialog

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.context.Disposable
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.strings.STRINGS
import java.awt.Component
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane

class FileSaveHandler private constructor(
    private val onFileSpecified: suspend (File) -> Unit,
    private val onCancel: () -> Unit = {},
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

    private fun onFileSpecified(file: File) {
        scope.launch {
            if (file.exists()) {
                val result = JOptionPane.showConfirmDialog(
                    parent,
                    String.format(STRINGS.ui.fileExistsMessage, file.absolutePath),
                    STRINGS.ui.fileExistsTitle,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                )
                if (result == JOptionPane.YES_OPTION) {
                    onFileSpecified.invoke(file)
                    onFileSaved(file)
                } else {
                    onCancel()
                }
            } else {
                onFileSpecified.invoke(file)
                onFileSaved(file)
            }
        }
    }

    private fun onFileSaved(file: File) {
        GLog.d(TAG, "[onFileSaved] file=${file.absolutePath}")
        val result = JOptionPane.showOptionDialog(
            parent,
            STRINGS.ui.fileSaveCompleteMessage,
            STRINGS.ui.fileSaveCompleteTitle,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            arrayOf(STRINGS.ui.showFileInFileManager, STRINGS.ui.cancel),
            STRINGS.ui.showFileInFileManager
        )
        if (result == 0) {
            Desktop.getDesktop().open(file.parentFile)
        }
    }

    override fun destroy() {
        scope.cancel()
    }

    class Builder(private val parent: Component) {
        private var onFileSpecified: suspend (File) -> Unit = {}
        private var onCancel: () -> Unit = {}

        fun onFileSpecified(onFileSpecified: suspend (File) -> Unit) = apply {
            this.onFileSpecified = onFileSpecified
        }

        fun onCancel(onCancel: () -> Unit) = apply {
            this.onCancel = onCancel
        }

        fun build() = FileSaveHandler(onFileSpecified, onCancel, parent)
    }

    companion object {
        private const val TAG = "SaveFileHandler"
    }
}