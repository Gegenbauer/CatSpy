package me.gegenbauer.logviewer.ui.panel

import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.resource.strings.STRINGS
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.log.LogPanel
import me.gegenbauer.logviewer.ui.log.LogTableModel
import me.gegenbauer.logviewer.utils.currentPlatform
import java.awt.datatransfer.DataFlavor
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.File
import java.net.URI
import javax.swing.JOptionPane
import javax.swing.JSplitPane
import javax.swing.TransferHandler

class SplitLogPane(
    private val mainUI: MainUI,
    fullTableModel: LogTableModel,
    filteredTableModel: LogTableModel
) : JSplitPane(), FocusListener {

    var onFocusGained: (Boolean) -> Unit = {}

    val fullLogPanel = LogPanel(mainUI, fullTableModel, null, this)
    val filteredLogPanel = LogPanel(mainUI, filteredTableModel, fullLogPanel, this)
    var rotation: Orientation = Orientation.ROTATION_LEFT_RIGHT
        set(value) {
            if (field == value) return
            field = value
            forceRotate(value)
        }

    init {
        continuousLayout = false
        orientation = HORIZONTAL_SPLIT
        add(fullLogPanel, LEFT)
        add(filteredLogPanel, RIGHT)

        transferHandler = TableTransferHandler()
    }

    fun forceRotate(orientation: Orientation = rotation) {
        remove(filteredLogPanel)
        remove(fullLogPanel)
        when (orientation) {
            Orientation.ROTATION_LEFT_RIGHT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, LEFT)
                add(filteredLogPanel, RIGHT)
                resizeWeight = SPLIT_WEIGHT
            }

            Orientation.ROTATION_TOP_BOTTOM -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, TOP)
                add(filteredLogPanel, BOTTOM)
                resizeWeight = SPLIT_WEIGHT
            }

            Orientation.ROTATION_RIGHT_LEFT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, RIGHT)
                add(filteredLogPanel, LEFT)
                resizeWeight = 1 - SPLIT_WEIGHT
            }

            Orientation.ROTATION_BOTTOM_TOP -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, BOTTOM)
                add(filteredLogPanel, TOP)
                resizeWeight = 1 - SPLIT_WEIGHT
            }
        }
    }

    internal inner class TableTransferHandler : TransferHandler() {
        override fun canImport(info: TransferSupport): Boolean {
            if (info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return true
            }

            if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return true
            }

            return false
        }

        override fun importData(info: TransferSupport): Boolean {
            GLog.d(TAG, "importData")
            if (!info.isDrop) {
                return false
            }

            val fileList: MutableList<File> = mutableListOf()

            if (info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val data: String
                try {
                    data = info.transferable.getTransferData(DataFlavor.stringFlavor) as String
                    val splitData = data.split("\n")

                    for (item in splitData) {
                        if (item.isNotEmpty()) {
                            GLog.d(TAG, "importData item = $item")
                            fileList.add(File(URI(item.trim())))
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    return false
                }
            }

            if (fileList.size == 0 && info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                val listFile: Any
                try {
                    listFile = info.transferable.getTransferData(DataFlavor.javaFileListFlavor)
                    if (listFile is List<*>) {
                        val iterator = listFile.iterator()
                        while (iterator.hasNext()) {
                            fileList.add(iterator.next() as File)
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    return false
                }
            }

            if (fileList.size > 0) {
                val os = currentPlatform
                GLog.d(TAG, "os = $os, drop = ${info.dropAction}, source drop = ${info.sourceDropActions}, user drop = ${info.userDropAction}")
                val action = os.getFileDropAction(info)

                var value = 1
                if (action == COPY) {
                    val options = arrayOf<Any>(
                        STRINGS.ui.append,
                        STRINGS.ui.open,
                        STRINGS.ui.cancel
                    )
                    value = JOptionPane.showOptionDialog(
                        mainUI, STRINGS.ui.msgSelectOpenMode,
                        "",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options,
                        options[0]
                    )
                }

                when (value) {
                    0 -> {
                        for (file in fileList) {
                            mainUI.openFile(file.absolutePath, true)
                        }
                    }

                    1 -> {
                        var isFirst = true
                        for (file in fileList) {
                            if (isFirst) {
                                mainUI.openFile(file.absolutePath, false)
                                isFirst = false
                            } else {
                                mainUI.openFile(file.absolutePath, true)
                            }
                        }
                    }

                    else -> {
                        GLog.d(TAG, "select cancel")
                    }
                }
            }
            return true
        }
    }

    fun rotate(orientation: Orientation = rotation.next()) {
        rotation = orientation
    }

    override fun focusGained(e: FocusEvent) {
        onFocusGained.invoke(e.source == filteredLogPanel)
    }

    override fun focusLost(e: FocusEvent) {
        // do nothing
    }

    companion object {
        private const val TAG = "SplitLogPane"
        private const val SPLIT_WEIGHT = 0.7
    }

}

enum class Orientation {
    ROTATION_LEFT_RIGHT,
    ROTATION_TOP_BOTTOM,
    ROTATION_RIGHT_LEFT,
    ROTATION_BOTTOM_TOP,
}

fun Orientation.next(): Orientation {
    return when (this) {
        Orientation.ROTATION_LEFT_RIGHT -> {
            Orientation.ROTATION_TOP_BOTTOM
        }

        Orientation.ROTATION_TOP_BOTTOM -> {
            Orientation.ROTATION_RIGHT_LEFT
        }

        Orientation.ROTATION_RIGHT_LEFT -> {
            Orientation.ROTATION_BOTTOM_TOP
        }

        Orientation.ROTATION_BOTTOM_TOP -> {
            Orientation.ROTATION_LEFT_RIGHT
        }
    }
}