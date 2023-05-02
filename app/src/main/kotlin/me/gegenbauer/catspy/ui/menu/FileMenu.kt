package me.gegenbauer.catspy.ui.menu

import me.gegenbauer.catspy.file.Log
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.Menu
import me.gegenbauer.catspy.utils.findFrameFromParent
import me.gegenbauer.catspy.utils.loadIcon
import me.gegenbauer.catspy.utils.loadThemedIcon
import java.awt.FileDialog
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.JMenu
import javax.swing.JMenuItem

class FileMenu : JMenu() {
    private val itemFileOpen = JMenuItem(STRINGS.ui.open).apply {
        icon = loadThemedIcon("file.svg", Menu.MENU_ITEM_ICON_SIZE)
    }
    private val itemFileFollow = JMenuItem(STRINGS.ui.follow).apply {
        icon = loadThemedIcon("append_file.svg", Menu.MENU_ITEM_ICON_SIZE)
    }
    private val itemFileOpenFiles = JMenuItem(STRINGS.ui.openFiles).apply {
        icon = loadThemedIcon("files.svg", Menu.MENU_ITEM_ICON_SIZE)
    }
    private val itemFileAppendFiles = JMenuItem(STRINGS.ui.appendFiles).apply {
        icon = loadThemedIcon("append_files.svg", Menu.MENU_ITEM_ICON_SIZE)
    }
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

    var onFileSelected: (File) -> Unit = {}
    var onFileListSelected: (Array<File>) -> Unit = {}
    var onFilesAppendSelected: (Array<File>) -> Unit = {}
    var onFileFollowSelected: (File) -> Unit = {}
    var onExit: () -> Unit = {}

    init {
        text = STRINGS.ui.file
        mnemonic = KeyEvent.VK_F

        add(itemFileOpen)
        add(itemFileFollow)
        add(itemFileOpenFiles)
        add(itemFileAppendFiles)
        addSeparator()
        add(itemFileExit)

        itemFileOpen.addActionListener(actionHandler)
        itemFileOpen.addActionListener(actionHandler)
        itemFileFollow.addActionListener(actionHandler)
        itemFileOpenFiles.addActionListener(actionHandler)
        itemFileAppendFiles.addActionListener(actionHandler)
        itemFileExit.addActionListener(actionHandler)
    }

    fun onClickFileOpen() {
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