package me.gegenbauer.catspy.log.ui.tab

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.recent.RecentFile
import me.gegenbauer.catspy.log.recent.RecentLogFiles
import me.gegenbauer.catspy.platform.userHome
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.findFrameFromParent
import me.gegenbauer.catspy.utils.ui.showSelectSingleFileDialog
import me.gegenbauer.catspy.view.button.CloseButton
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.button.GButton
import me.gegenbauer.catspy.view.label.EllipsisLabel
import me.gegenbauer.catspy.view.panel.HorizontalFlexibleHeightLayout
import me.gegenbauer.catspy.view.panel.HoverStateAwarePanel
import me.gegenbauer.catspy.view.panel.ScrollConstrainedScrollablePanel
import me.gegenbauer.catspy.view.panel.VerticalFlexibleWidthLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

class FileLogGuidePanel(
    private val onOpenFile: (String) -> Unit
) : JPanel() {
    private val openFileButton = GButton(STRINGS.ui.openFile)
    private val recentFilesContainer = JScrollPane()
    private val recentFilesPanel = RecentFilesPanel()

    private val recentFilesChangeObserver: (List<RecentFile>) -> Unit = {
        loadRecentFiles()
    }

    init {
        border = BorderFactory.createTitledBorder(STRINGS.ui.logFile)
        val actionPanel = JPanel(HorizontalFlexibleHeightLayout())
        actionPanel.add(openFileButton)

        recentFilesContainer.setViewportView(recentFilesPanel)
        recentFilesContainer.border = BorderFactory.createTitledBorder(STRINGS.ui.recentFiles)
        recentFilesContainer.horizontalScrollBar.unitIncrement = 20
        recentFilesContainer.verticalScrollBar.unitIncrement = 20

        layout = TableLayout(
            arrayOf(
                doubleArrayOf(FILL),
                doubleArrayOf(PREFERRED, 300.0)
            )
        )
        add(actionPanel, "0,0")
        add(recentFilesContainer, "0,1")

        loadRecentFiles()
        openFileButton.addActionListener { onClickFileOpen() }
        RecentLogFiles.registerRecentFileChangeListener(recentFilesChangeObserver)
    }

    private fun loadRecentFiles() {
        val files = RecentLogFiles.getRecentFiles()
        recentFilesPanel.setFiles(files)
    }

    private fun onClickFileOpen() {
        val files = showSelectSingleFileDialog(
            findFrameFromParent(),
            STRINGS.ui.openFile,
            RecentLogFiles.getLastOpenDir(),
        )
        files.firstOrNull()?.let {
            onOpenFile(it.absolutePath)
            RecentLogFiles.onNewFileOpen(it.absolutePath)
        }
    }

    private inner class RecentFilesPanel : ScrollConstrainedScrollablePanel(
        horizontalScrollable = false
    ), FileActionListener {
        private val fileItems = mutableListOf<RecentFile>()

        init {
            layout = VerticalFlexibleWidthLayout()
        }

        fun setFiles(files: List<RecentFile>) {
            if (fileItems == files) {
                return
            }
            fileItems.clear()
            val sortedFiles = files.sortedWith { o1, o2 ->
                if (o1.isStarred && !o2.isStarred) {
                    return@sortedWith -1
                } else if (!o1.isStarred && o2.isStarred) {
                    return@sortedWith 1
                }
                return@sortedWith o2.lastOpenTime.compareTo(o1.lastOpenTime)
            }
            fileItems.addAll(sortedFiles)

            removeAll()
            fileItems.forEach(::addItem)
            revalidate()
            repaint()
        }

        private fun addItem(file: RecentFile) {
            val itemPanel = FileItemPanel()
            itemPanel.setFile(file)
            itemPanel.listener = this
            add(itemPanel)
        }

        override fun onFileOpen(file: RecentFile) {
            onOpenFile(file.path)
            RecentLogFiles.onNewFileOpen(file.path)
        }

        override fun onFileStared(file: RecentFile) {
            val files = fileItems.map { if (it == file) it.copy(isStarred = !it.isStarred) else it.copy() }
            RecentLogFiles.saveRecentFiles(files)
        }

        override fun onFileDelete(file: RecentFile) {
            RecentLogFiles.saveRecentFiles(fileItems - file)
        }

        override fun onFileHover(file: RecentFile) {
            val index = fileItems.indexOf(file)
            for (i in 0 until componentCount) {
                if (i == index) continue
                val item = getComponent(i) as FileItemPanel
                item.setHover(false)
            }
        }
    }

    private class FileItemPanel : HoverStateAwarePanel(), ActionListener, FileActionListener {

        private val nameLabel = EllipsisLabel()
        private val lastOpenTimeLabel = JLabel()
        private val starButton = ColorToggleButton(
            GIcons.State.NotStared.get(STAR_ICON_SIZE, STAR_ICON_SIZE),
            GIcons.State.Stared.get(STAR_ICON_SIZE, STAR_ICON_SIZE)
        )
        private val deleteButton = CloseButton(DELETE_ICON_SIZE) { onFileDelete(fileItem) }

        private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")

        var listener: FileActionListener? = null

        private var fileItem: RecentFile = RecentFile()

        init {
            layout = TableLayout(
                arrayOf(
                    doubleArrayOf(FILL, 200.0, PREFERRED, PREFERRED),
                    doubleArrayOf(PREFERRED)
                )
            )
            nameLabel.border = BorderFactory.createEmptyBorder(0, 4, 0, 4)

            starButton.border = BorderFactory.createEmptyBorder(0, 8, 0, 20)
            nameLabel.isFocusable = true

            add(nameLabel, "0,0")
            add(lastOpenTimeLabel, "1,0")
            add(starButton, "2,0")
            add(deleteButton, "3,0")

            starButton.addActionListener(this)
            configureOpenFileAction()
        }

        override fun onHoverStateChanged(isHover: Boolean) {
            setHover(isHover)
            if (isHover) {
                listener?.onFileHover(fileItem)
            }
        }

        fun setHover(isHover: Boolean) {
            if (isHover) {
                background = UIManager.getColor("List.selectionBackground")
            } else {
                background = null
            }
        }

        private fun configureOpenFileAction() {
            nameLabel.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    onFileOpen(fileItem)
                }
            })
        }

        fun setFile(file: RecentFile) {
            nameLabel.text = file.name
            nameLabel.toolTipText = file.path
            lastOpenTimeLabel.text = file.lastOpenTime.toDateString()
            starButton.isSelected = file.isStarred

            fileItem = file
        }

        private fun Long.toDateString(): String {
            return dateFormat.format(Date(this))
        }

        override fun actionPerformed(e: ActionEvent) {
            when (e.source) {
                starButton -> onFileStared(fileItem)
            }
        }

        override fun onFileDelete(file: RecentFile) {
            listener?.onFileDelete(file)
        }

        override fun onFileOpen(file: RecentFile) {
            listener?.onFileOpen(file)
        }

        override fun onFileStared(file: RecentFile) {
            listener?.onFileStared(file)
        }

        companion object {
            private const val STAR_ICON_SIZE = 20
            private const val DELETE_ICON_SIZE = 20
        }

    }

    private interface FileActionListener {
        fun onFileOpen(file: RecentFile) {}

        fun onFileStared(file: RecentFile) {}

        fun onFileDelete(file: RecentFile) {}

        fun onFileHover(file: RecentFile) {}
    }
}