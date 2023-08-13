package me.gegenbauer.catspy.log.ui.dialog

import com.github.weisj.darklaf.properties.icons.DerivableImageIcon
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.log.ui.panel.FullLogPanel
import me.gegenbauer.catspy.utils.Utils
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel

class LogTableDialog(
    private val logPanel: FullLogPanel,
    private val onWindowClosing: () -> Unit = {}
) : JFrame("FullLog") {
    private var frameX = 0
    private var frameY = 0
    private var frameWidth = 1280
    private var frameHeight = 720

    init {
        val img = GIcons.Logo.get() as DerivableImageIcon
        iconImage = img.image

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

        Utils.installKeyStrokeEscClosing(this)
    }

    companion object {
        private const val TAG = "LogTableDialog"
    }
}