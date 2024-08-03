package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.hint.HintManager
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.TransferHandler

interface TabPanel : Context {

    val tag: String

    val hint: HintManager.Hint?
        get() = null

    fun setup(bundle: Bundle?)

    fun onTabSelected()

    fun onTabUnselected()

    fun setTabNameController(controller: (String) -> Unit)

    fun setTabTooltipController(controller: (String?) -> Unit)

    fun isDataImportSupported(info: TransferHandler.TransferSupport): Boolean = false

    fun handleDataImport(info: TransferHandler.TransferSupport): Boolean = false

    fun getTabContent(): JComponent

    fun getDroppedFiles(info: TransferHandler.TransferSupport): List<File> {
        GLog.d(tag, "[importData] info = $info")
        info.takeIf { it.isDrop } ?: return emptyList()

        val fileList: MutableList<File> = mutableListOf()

        if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            runCatching {
                val data = info.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                data?.mapNotNull { it as? File }?.forEach { fileList.add(it) }
            }.onFailure {
                GLog.e(tag, "[importData]", it)
            }
        }

        val os = currentPlatform
        GLog.d(tag, "os:$os, drop:${info.dropAction},sourceDrop:${info.sourceDropActions},userDrop:${info.userDropAction}")
        return fileList
    }

    fun pendingOpenFiles(files: List<File>) {
        // no-op
    }
}