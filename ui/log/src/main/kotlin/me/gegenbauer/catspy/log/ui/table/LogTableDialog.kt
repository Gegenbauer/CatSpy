package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.iconset.appIcons
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.installKeyStrokeEscClosing
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel

class LogTableDialog(
    private val logPanel: FullLogPanel,
    private val onWindowClosing: () -> Unit = {}
) : JFrame(STRINGS.ui.fullLogDialogTitle) {

    init {
        iconImages = appIcons

        defaultCloseOperation = DISPOSE_ON_CLOSE
        currentSettings.windowSettings.loadWindowSettings(
            this,
            Rectangle(DEFAULT_WINDOW_X, DEFAULT_WINDOW_Y, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT)
        )
        val panel = JPanel(BorderLayout())
        panel.add(this.logPanel, BorderLayout.CENTER)
        contentPane.add(panel)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                GLog.i(TAG, "[windowClosing] exit table dialog")
                onWindowClosing()
            }
        })

        installKeyStrokeEscClosing(this)
    }

    override fun dispose() {
        super.dispose()
        SettingsManager.updateSettings {
            windowSettings.saveWindowSettings(this@LogTableDialog)
        }
    }

    companion object {
        private const val TAG = "LogTableDialog"

        private const val DEFAULT_WINDOW_X = 0
        private const val DEFAULT_WINDOW_Y = 0
        private const val DEFAULT_WINDOW_WIDTH = 1280
        private const val DEFAULT_WINDOW_HEIGHT = 720
    }
}