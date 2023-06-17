package me.gegenbauer.catspy.ui.panel

import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.log.FilteredLogPanel
import me.gegenbauer.catspy.ui.log.FullLogPanel
import me.gegenbauer.catspy.ui.log.LogTableModel
import me.gegenbauer.catspy.utils.currentPlatform
import java.awt.datatransfer.DataFlavor
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.File
import java.net.URI
import javax.swing.JOptionPane
import javax.swing.JSplitPane
import javax.swing.SwingUtilities
import javax.swing.TransferHandler

class SplitLogPane(
    private val mainUI: MainUI,
    fullTableModel: LogTableModel,
    filteredTableModel: LogTableModel
) : JSplitPane(), FocusListener {

    var onFocusGained: (Boolean) -> Unit = {}

    val fullLogPanel = FullLogPanel(fullTableModel)
    val filteredLogPanel = FilteredLogPanel(filteredTableModel, this, fullLogPanel)
    var rotation: Rotation = Rotation.ROTATION_LEFT_RIGHT
        set(value) {
            field = value
            changeRotation(value)
        }

    init {
        continuousLayout = false
        orientation = HORIZONTAL_SPLIT
        add(fullLogPanel, LEFT)
        add(filteredLogPanel, RIGHT)

        transferHandler = TableTransferHandler()
    }

    private fun changeRotation(rotation: Rotation) {
        remove(filteredLogPanel)
        remove(fullLogPanel)
        when (rotation) {
            Rotation.ROTATION_LEFT_RIGHT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, LEFT)
                add(filteredLogPanel, RIGHT)
                resizeWeight = SPLIT_WEIGHT
            }

            Rotation.ROTATION_TOP_BOTTOM -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, TOP)
                add(filteredLogPanel, BOTTOM)
                resizeWeight = SPLIT_WEIGHT
            }

            Rotation.ROTATION_RIGHT_LEFT -> {
                setOrientation(HORIZONTAL_SPLIT)
                add(fullLogPanel, RIGHT)
                add(filteredLogPanel, LEFT)
                resizeWeight = 1 - SPLIT_WEIGHT
            }

            Rotation.ROTATION_BOTTOM_TOP -> {
                setOrientation(VERTICAL_SPLIT)
                add(fullLogPanel, BOTTOM)
                add(filteredLogPanel, TOP)
                resizeWeight = 1 - SPLIT_WEIGHT
            }
        }
        SwingUtilities.updateComponentTreeUI(this)
    }

    fun resetWithCurrentRotation() {
        changeRotation(rotation)
    }

    internal inner class TableTransferHandler : TransferHandler() {
        override fun canImport(info: TransferSupport): Boolean {
            if (info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return true
            }

            return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
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

                val options = arrayOf<Any>(
                    STRINGS.ui.append,
                    STRINGS.ui.open,
                    STRINGS.ui.cancel
                )
                val value = JOptionPane.showOptionDialog(
                    mainUI, STRINGS.ui.msgSelectOpenMode,
                    "",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
                )

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

enum class Rotation {
    ROTATION_LEFT_RIGHT,
    ROTATION_TOP_BOTTOM,
    ROTATION_RIGHT_LEFT,
    ROTATION_BOTTOM_TOP,
}

fun Rotation.next(): Rotation {
    return when (this) {
        Rotation.ROTATION_LEFT_RIGHT -> {
            Rotation.ROTATION_TOP_BOTTOM
        }

        Rotation.ROTATION_TOP_BOTTOM -> {
            Rotation.ROTATION_RIGHT_LEFT
        }

        Rotation.ROTATION_RIGHT_LEFT -> {
            Rotation.ROTATION_BOTTOM_TOP
        }

        Rotation.ROTATION_BOTTOM_TOP -> {
            Rotation.ROTATION_LEFT_RIGHT
        }
    }
}