package me.gegenbauer.logviewer.ui.menu

import me.gegenbauer.logviewer.file.Log
import me.gegenbauer.logviewer.strings.STRINGS
import me.gegenbauer.logviewer.utils.findFrameFromParent
import java.awt.FileDialog
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.JMenu
import javax.swing.JMenuItem

class FileMenu : JMenu() {
    private val itemFileOpen = JMenuItem(STRINGS.ui.open)
    private val itemFileFollow = JMenuItem(STRINGS.ui.follow)
    private val itemFileOpenFiles = JMenuItem(STRINGS.ui.openFiles)
    private val itemFileAppendFiles = JMenuItem(STRINGS.ui.appendFiles)
    private val itemFileExit = JMenuItem(STRINGS.ui.exit)

    private val actionHandler = ActionListener {
        when (it.source) {
            itemFileOpen -> {
                onClickFileOpen()
            }

            itemFileFollow -> {
                onClickFileFollow()
            }

            itemFileOpenFiles -> {
                onClickFileOpenFiles()
            }

            itemFileAppendFiles -> {
                onClickFileAppendFiles()
            }

            itemFileExit -> {
                onClickFileExit()
            }
        }
    }

    private var onFileSelected: (File) -> Unit = {}
    private var onFileListSelected: (Array<File>) -> Unit = {}
    private var onFilesAppendSelected: (Array<File>) -> Unit = {}
    private var onFileFollowSelected: (File) -> Unit = {}
    private var onExit: () -> Unit = {}

    init {
        text = STRINGS.ui.file
        mnemonic = KeyEvent.VK_F


        itemFileOpen.addActionListener(actionHandler)
        add(itemFileOpen)

        itemFileFollow.addActionListener(actionHandler)
        add(itemFileFollow)

        itemFileOpenFiles.addActionListener(actionHandler)
        add(itemFileOpenFiles)

        itemFileAppendFiles.addActionListener(actionHandler)
        add(itemFileAppendFiles)

        addSeparator()

        itemFileExit.addActionListener(actionHandler)
        add(itemFileExit)
    }

    fun setOnFileSelected(onFileSelected: (File) -> Unit) {
        this.onFileSelected = onFileSelected
    }

    fun setOnFileListSelected(onFileListSelected: (Array<File>) -> Unit) {
        this.onFileListSelected = onFileListSelected
    }

    fun setOnFilesAppendSelected(onFilesAppendSelected: (Array<File>) -> Unit) {
        this.onFilesAppendSelected = onFilesAppendSelected
    }

    fun setOnFileFollowSelected(onFileFollowChanged: (File) -> Unit) {
        this.onFileFollowSelected = onFileFollowChanged
    }

    fun setOnExit(onExit: () -> Unit) {
        this.onExit = onExit
    }

    private fun onClickFileOpen() {
        val frame = findFrameFromParent(this)
        val fileDialog = FileDialog(frame, STRINGS.ui.file + " " + STRINGS.ui.open, FileDialog.LOAD)
        fileDialog.isMultipleMode = false
        fileDialog.directory = Log.file?.parent
        fileDialog.isVisible = true
        if (fileDialog.file != null) {
            val file = File(fileDialog.directory + fileDialog.file)
            onFileSelected(file)
        }
    }

    private fun onClickFileFollow() {
        val frame = findFrameFromParent(this)
        val fileDialog = FileDialog(frame, STRINGS.ui.file + " " + STRINGS.ui.follow, FileDialog.LOAD)
        fileDialog.isMultipleMode = false
        fileDialog.directory = Log.file?.parent
        fileDialog.isVisible = true
        if (fileDialog.file != null) {
            val file = File(fileDialog.directory + fileDialog.file)
            onFileFollowSelected(file)
        }
    }

    private fun onClickFileAppendFiles() {
        val frame = findFrameFromParent(this)
        val fileDialog = FileDialog(frame, STRINGS.ui.file + " " + STRINGS.ui.appendFiles, FileDialog.LOAD)
        fileDialog.isMultipleMode = true
        fileDialog.directory = Log.file?.parent
        fileDialog.isVisible = true
        val fileList = fileDialog.files
        if (fileList != null) {
            onFilesAppendSelected(fileList)
        }
    }

    private fun onClickFileExit() {
        onExit()
    }

    private fun onClickFileOpenFiles() {
        val frame = findFrameFromParent(this)
        val fileDialog = FileDialog(frame, STRINGS.ui.file + " " + STRINGS.ui.openFiles, FileDialog.LOAD)
        fileDialog.isMultipleMode = true
        fileDialog.directory = Log.file?.parent
        fileDialog.isVisible = true
        val fileList = fileDialog.files
        if (fileList != null) {
            onFileListSelected(fileList)
        }
    }
}