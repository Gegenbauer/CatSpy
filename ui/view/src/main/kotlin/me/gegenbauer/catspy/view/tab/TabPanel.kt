package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.currentPlatform
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.hint.HintManager
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.TransferHandler

interface TabPanel : Context {
    val tabName: String

    val tabIcon: Icon?

    val hint: HintManager.Hint?
        get() = null

    val tabTooltip: String?
        get() = STRINGS.toolTip.tab

    val tabMnemonic: Char
        get() = ' '

    val closeable: Boolean
        get() = true

    fun setup()

    fun onTabSelected()

    fun onTabUnselected()

    fun isDataImportSupported(info: TransferHandler.TransferSupport): Boolean = false

    fun handleDataImport(info: TransferHandler.TransferSupport): Boolean = false

    fun getTabContent(): JComponent

    fun getDroppedFiles(info: TransferHandler.TransferSupport): List<File> {
        GLog.d(tabName, "[importData] info = $info")
        info.takeIf { it.isDrop } ?: return emptyList()

        val fileList: MutableList<File> = mutableListOf()

        if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            runCatching {
                val data = info.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                data?.mapNotNull { it as? File }?.forEach { fileList.add(it) }
            }.onFailure {
                GLog.e(tabName, "[importData]", it)
            }
        }

        val os = currentPlatform
        GLog.d(tabName, "os:$os, drop:${info.dropAction},sourceDrop:${info.sourceDropActions},userDrop:${info.userDropAction}")
        return fileList
    }
}