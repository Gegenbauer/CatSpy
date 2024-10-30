package me.gegenbauer.catspy.view.panel

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.view.tab.TabPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JPanel

interface FileDropHandler {
    fun handleFileDrop(files: List<File>) = Unit

    fun isFileAcceptable(files: List<File>): Boolean = false
}

class FileDropPanelWrapper(val content: TabPanel): JPanel(), FileDropHandler, TabPanel by content {

    private val dropTarget = object : DropTargetAdapter() {
        override fun dragEnter(dtde: DropTargetDragEvent) {
            GLog.d(TAG, "[dragEnter] drag enter")
            if (isDragAcceptable(dtde)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
                onDragAccept()
            } else {
                dtde.rejectDrag()
            }
        }

        override fun dragExit(dte: DropTargetEvent) {
            GLog.d(TAG, "[dragExit] drag enter")
            onDragExit()
        }

        override fun drop(dtde: DropTargetDropEvent) {
            GLog.d(TAG, "[drop] drag enter")
            onDragExit()
            dtde.acceptDrop(DnDConstants.ACTION_COPY)
            val files = getFiles(dtde)
            if (isFileAcceptable(files)) {
                handleFileDrop(files)
            } else {
                dtde.rejectDrop()
            }
        }

        private fun getFiles(dragEvent: DropTargetEvent): List<File> {
            return runCatching {
                val transferable = if (dragEvent is DropTargetDragEvent) {
                    dragEvent.transferable
                } else if (dragEvent is DropTargetDropEvent) {
                    dragEvent.transferable
                } else {
                    null
                } ?: return emptyList()
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    (transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>)
                        .filterIsInstance<File>()
                } else {
                    emptyList()
                }
            }.onFailure {
                GLog.e(TAG, "[getFiles] get files failed", it)
            }.getOrDefault(emptyList())
        }

        private fun isDragAcceptable(dragEvent: DropTargetDragEvent): Boolean {
            return isFileAcceptable(getFiles(dragEvent))
        }
    }

    init {
        layout = BorderLayout()
        add(content.getTabContent(), BorderLayout.CENTER)

        DropTarget(this, dropTarget)
    }

    private fun onDragAccept() {
        border = BorderFactory.createLineBorder(Color.GREEN, 2)
    }

    private fun onDragExit() {
        border = BorderFactory.createEmptyBorder()
    }

    override fun getTabContent(): JPanel {
        return this
    }

    companion object {
        private const val TAG = "FileDropPanelWrapper"
    }
}