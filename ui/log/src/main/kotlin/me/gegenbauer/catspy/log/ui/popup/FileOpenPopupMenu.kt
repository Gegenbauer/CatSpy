package me.gegenbauer.catspy.log.ui.popup

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.configuration.Menu
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.platform.userHome
import java.awt.Dimension
import java.awt.event.ActionListener
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.filechooser.FileNameExtensionFilter

class FileOpenPopupMenu(override val contexts: Contexts = Contexts.default) : JPopupMenu(), Context {

    private val itemFileOpen = JMenuItem(STRINGS.ui.open).apply {
        icon = GIcons.Files.File.get(Menu.MENU_ITEM_ICON_SIZE, Menu.MENU_ITEM_ICON_SIZE)
    }

    private val actionHandler = ActionListener {
        when (it.source) {
            itemFileOpen -> {
                onClickFileOpen()
            }
        }
    }

    var onFileSelected: (File) -> Unit = {}
    var onFileListSelected: (Array<File>) -> Unit = {}
    var onFilesAppendSelected: (Array<File>) -> Unit = {}
    var onFileFollowSelected: (File) -> Unit = {}

    init {
        add(itemFileOpen)

        itemFileOpen.addActionListener(actionHandler)
    }

    fun onClickFileOpen() {
        chooseSingleFile(STRINGS.ui.open) {
            it?.let(onFileSelected)
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
        val frame = DarkUIUtil.getParentOfType(JFrame::class.java, invoker)
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