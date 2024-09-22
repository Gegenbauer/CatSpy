package me.gegenbauer.catspy.log.ui.tab

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import me.gegenbauer.catspy.file.toHumanReadableSize
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.recent.RecentFile
import me.gegenbauer.catspy.log.recent.RecentLogFiles
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.ui.findFrameFromParent
import me.gegenbauer.catspy.utils.ui.isLeftClick
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.BorderFactory
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.UIManager

class FileLogGuidancePanel(
    private val onOpenFile: (File) -> Unit
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
        actionPanel.border = BorderFactory.createEmptyBorder(6, 10, 6, 0)
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
            onOpenFile(it)
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

        override fun onFileOpen(recentFile: RecentFile) {
            val file = File(recentFile.path)
            if (file.exists().not()) {
                showFileNotExistsWarning(recentFile)
                onFileDelete(recentFile)
            } else {
                onOpenFile(file)
                RecentLogFiles.onNewFileOpen(recentFile.path)
            }
        }

        private fun showFileNotExistsWarning(file: RecentFile) {
            val message = STRINGS.ui.fileNotExistWarning.get(file.path)
            JOptionPane.showMessageDialog(
                this,
                message,
                EMPTY_STRING,
                JOptionPane.WARNING_MESSAGE
            )
        }

        override fun onFileStared(recentFile: RecentFile) {
            val files = fileItems.map { if (it == recentFile) it.copy(isStarred = !it.isStarred) else it.copy() }
            RecentLogFiles.saveRecentFiles(files)
        }

        override fun onFileDelete(recentFile: RecentFile) {
            RecentLogFiles.saveRecentFiles(fileItems - recentFile)
        }

        override fun onFileHover(recentFile: RecentFile) {
            val index = fileItems.indexOf(recentFile)
            for (i in 0 until componentCount) {
                if (i == index) continue
                val item = getComponent(i) as FileItemPanel
                item.setHover(false)
            }
        }
    }

    private class FileItemPanel : HoverStateAwarePanel(), ActionListener, FileActionListener {

        var listener: FileActionListener? = null

        private val nameLabel = EllipsisLabel()
        private val lastOpenTimeLabel = EllipsisLabel()
        private val fileSizeLabel = EllipsisLabel()
        private val starButton = ColorToggleButton(
            GIcons.State.NotStared.get(STAR_ICON_SIZE, STAR_ICON_SIZE),
            GIcons.State.Stared.get(STAR_ICON_SIZE, STAR_ICON_SIZE)
        )
        private val deleteButton = CloseButton(DELETE_ICON_SIZE) { onFileDelete(fileItem) }

        private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")

        private var fileItem: RecentFile = RecentFile()
        private val clickToOpenListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.isLeftClick) {
                    onFileOpen(fileItem)
                }
            }
        }

        init {
            layout = TableLayout(
                arrayOf(
                    doubleArrayOf(FILL, 200.0, 100.0, PREFERRED, PREFERRED),
                    doubleArrayOf(PREFERRED)
                )
            )
            nameLabel.border = BorderFactory.createEmptyBorder(0, 4, 0, 4)

            starButton.border = BorderFactory.createEmptyBorder(0, 8, 0, 20)
            nameLabel.isFocusable = true

            add(nameLabel, "0,0")
            add(lastOpenTimeLabel, "1,0")
            add(fileSizeLabel, "2,0")
            add(starButton, "3,0")
            add(deleteButton, "4,0")

            starButton.addActionListener(this)
            configureOpenFileAction()
        }

        override fun onHoverStateChanged(isHover: Boolean, e: MouseEvent) {
            super.onHoverStateChanged(isHover, e)
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
            nameLabel.addMouseListener(clickToOpenListener)
            lastOpenTimeLabel.addMouseListener(clickToOpenListener)
            fileSizeLabel.addMouseListener(clickToOpenListener)
        }

        fun setFile(file: RecentFile) {
            nameLabel.text = file.name
            nameLabel.toolTipText = file.path
            file.lastOpenTime.toDateString().also {
                lastOpenTimeLabel.text = it
                lastOpenTimeLabel.toolTipText = it
            }
            File(file.path).length().toHumanReadableSize().also {
                fileSizeLabel.text = it
                fileSizeLabel.toolTipText = it
            }
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

        override fun onFileDelete(recentFile: RecentFile) {
            listener?.onFileDelete(recentFile)
        }

        override fun onFileOpen(recentFile: RecentFile) {
            listener?.onFileOpen(recentFile)
        }

        override fun onFileStared(recentFile: RecentFile) {
            listener?.onFileStared(recentFile)
        }

        companion object {
            private const val STAR_ICON_SIZE = 20
            private const val DELETE_ICON_SIZE = 20
        }

    }

    private interface FileActionListener {
        fun onFileOpen(recentFile: RecentFile) {}

        fun onFileStared(recentFile: RecentFile) {}

        fun onFileDelete(recentFile: RecentFile) {}

        fun onFileHover(recentFile: RecentFile) {}
    }
}