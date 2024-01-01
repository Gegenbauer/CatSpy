package me.gegenbauer.catspy.log.ui.dialog

import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.iconset.appIcons
import me.gegenbauer.catspy.log.ui.panel.FullLogPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.installKeyStrokeEscClosing
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel

class LogTableDialog(
    private val logPanel: FullLogPanel,
    private val onWindowClosing: () -> Unit = {}
) : JFrame(STRINGS.ui.fullLogDialogTitle) {
    private var frameX = 0
    private var frameY = 0
    private var frameWidth = 1280
    private var frameHeight = 720

    init {
        iconImages = appIcons

        defaultCloseOperation = DISPOSE_ON_CLOSE
        setLocation(frameX, frameY)
        setSize(frameWidth, frameHeight)
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

    companion object {
        private const val TAG = "LogTableDialog"
    }
}