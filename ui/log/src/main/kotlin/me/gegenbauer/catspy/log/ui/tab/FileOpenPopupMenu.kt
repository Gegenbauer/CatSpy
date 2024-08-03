package me.gegenbauer.catspy.log.ui.tab

import me.gegenbauer.catspy.configuration.Menu
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.recent.RecentLogFiles
import me.gegenbauer.catspy.platform.userHome
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.showSelectSingleFileDialog
import java.awt.event.ActionListener
import java.io.File
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

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

    init {
        add(itemFileOpen)

        itemFileOpen.addActionListener(actionHandler)
    }

    fun onClickFileOpen() {
        val files = showSelectSingleFileDialog(
            contexts.getContext(JFrame::class.java)!!,
            STRINGS.ui.openFile,
            RecentLogFiles.getLastOpenDir(),
        )
        files.firstOrNull()?.let { onFileSelected(it) }
    }
}