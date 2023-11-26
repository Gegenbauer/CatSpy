package me.gegenbauer.catspy.ui.dialog

import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.network.update.data.Release
import me.gegenbauer.catspy.strings.STRINGS
import javax.swing.JFrame
import javax.swing.JOptionPane

class UpdateDialog(
   private val frame: JFrame, private val newRelease: Release,
    private val onDownloadConfirm: (Release) -> Unit
) {
    fun show() {
        val result = JOptionPane.showOptionDialog(
            frame,
            String.format(STRINGS.ui.updateDialogMessage, newRelease.name),
            STRINGS.ui.updateDialogTitle,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            arrayOf(STRINGS.ui.updateDialogConfirm, STRINGS.ui.cancel),
            STRINGS.ui.updateDialogConfirm
        )
        if (result == JOptionPane.OK_OPTION) {
            onDownloadConfirm(newRelease)
        }
        if (result == JOptionPane.OK_OPTION || result == JOptionPane.NO_OPTION) {
            UIConfManager.uiConf.ignoredRelease.add(newRelease.name)
        }
    }
}