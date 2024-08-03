package me.gegenbauer.catspy.ui.dialog

import me.gegenbauer.catspy.network.update.data.Release
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.ui.MainViewModel
import me.gegenbauer.catspy.utils.persistence.Preferences
import javax.swing.JFrame
import javax.swing.JOptionPane

class UpdateDialog(
   private val frame: JFrame, private val newRelease: Release,
    private val onDownloadConfirm: (Release) -> Unit
) {
    fun show() {
        val message = STRINGS.ui.updateDialogMessage.get(newRelease.name)
        val releaseDescription = STRINGS.ui.updateDescription.get(newRelease.body)
        val result = JOptionPane.showOptionDialog(
            frame,
            "$message\n\n$releaseDescription",
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
            val ignoredReleases = Preferences.getStringList(MainViewModel.IGNORED_RELEASES_KEY).toMutableList()
            ignoredReleases.add(0, newRelease.name)
            Preferences.putStringList(MainViewModel.IGNORED_RELEASES_KEY, ignoredReleases, MAX_IGNORED_RELEASES)
        }
    }

    companion object {
        private const val MAX_IGNORED_RELEASES = 10
    }
}