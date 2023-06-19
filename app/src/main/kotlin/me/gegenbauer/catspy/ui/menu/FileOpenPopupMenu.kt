package me.gegenbauer.catspy.ui.menu

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextScope
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.Menu
import me.gegenbauer.catspy.utils.loadThemedIcon
import me.gegenbauer.catspy.utils.userHome
import java.awt.Dimension
import java.awt.event.ActionListener
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.filechooser.FileNameExtensionFilter

class FileOpenPopupMenu(override val contexts: Contexts = Contexts.default) : JPopupMenu(), Context {
    override val scope: ContextScope = ContextScope.COMPONENT

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
        }
    }

    var onFileSelected: (File) -> Unit = {}
    var onFileListSelected: (Array<File>) -> Unit = {}
    var onFilesAppendSelected: (Array<File>) -> Unit = {}
    var onFileFollowSelected: (File) -> Unit = {}

    init {
        add(itemFileOpen)
        add(itemFileFollow)
        add(itemFileOpenFiles)
        add(itemFileAppendFiles)
        addSeparator()

        itemFileOpen.addActionListener(actionHandler)
        itemFileFollow.addActionListener(actionHandler)
        itemFileOpenFiles.addActionListener(actionHandler)
        itemFileAppendFiles.addActionListener(actionHandler)
    }

    fun onClickFileOpen() {
        chooseSingleFile(STRINGS.ui.open) {
            it?.let(onFileSelected)
        }
    }

    private fun onClickFileFollow() {
        chooseSingleFile(STRINGS.ui.follow) {
            it?.let(onFileFollowSelected)
        }
    }

    private fun onClickFileAppendFiles() {
        chooseMultiFiles(STRINGS.ui.appendFiles, true) {
            onFilesAppendSelected(it)
        }
    }

    private fun onClickFileOpenFiles() {
        chooseMultiFiles(STRINGS.ui.openFiles, false) {
            onFileListSelected(it)
        }
    }

    private fun chooseSingleFile(title: String, onFileSelected: (File?) -> Unit) {
        chooseMultiFiles(title, false) {
            onFileSelected(it.firstOrNull())
        }
    }

    private fun chooseMultiFiles(
        title: String,
        multiSelection: Boolean,
        onFilesSelected: (Array<File>) -> Unit
    ) {
        val frame = contexts.getContext(MainUI::class.java) as? JFrame ?: return
        val chooser = JFileChooser(userHome)
        chooser.dialogTitle = STRINGS.ui.file + " " + title
        chooser.preferredSize = Dimension(
            (frame.size.width / 2).coerceAtLeast(600),
            (frame.size.height / 2).coerceAtLeast(300)
        )
        chooser.addChoosableFileFilter(FileNameExtensionFilter("Log File", "txt"))
        // TODO support archives
        chooser.addChoosableFileFilter(FileNameExtensionFilter("Log Archive", "zip", "rar", "gz", "tar"))
        chooser.isMultiSelectionEnabled = multiSelection
        chooser.showOpenDialog(frame)
        val files = chooser.selectedFiles
        val file = chooser.selectedFile
        if (!files.isNullOrEmpty()) {
            onFilesSelected(files)
        } else if (file != null) {
            onFilesSelected(arrayOf(file))
        }
    }
}