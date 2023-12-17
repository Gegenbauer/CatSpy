package me.gegenbauer.catspy.ui.dialog

import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.network.update.data.Release
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import javax.swing.JFrame
import javax.swing.JOptionPane

class UpdateDialog(
   private val frame: JFrame, private val newRelease: Release,
    private val onDownloadConfirm: (Release) -> Unit
) {
    fun show() {
        val result = JOptionPane.showOptionDialog(
            frame,
            STRINGS.ui.updateDialogMessage.get(newRelease.name),
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
            SettingsManager.settings.ignoredRelease.add(newRelease.name)
        }
    }
}