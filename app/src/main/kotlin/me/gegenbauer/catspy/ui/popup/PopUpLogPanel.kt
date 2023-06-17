package me.gegenbauer.catspy.ui.popup

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.ui.MainUI
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class PopUpLogPanel : JPopupMenu(), ActionListener {
    private val reconnectItem = JMenuItem("Reconnect adb")
    private val startItem = JMenuItem("Start")
    private val stopItem = JMenuItem("Stop")
    private val clearItem = JMenuItem("Clear")

    init {
        reconnectItem.addActionListener(this)
        startItem.addActionListener(this)
        stopItem.addActionListener(this)
        clearItem.addActionListener(this)
        add(reconnectItem)
        add(startItem)
        add(stopItem)
        add(clearItem)
    }

    override fun actionPerformed(event: ActionEvent) {
        val mainUI = DarkUIUtil.getParentOfType(this, MainUI::class.java)
        when (event.source) {
            reconnectItem -> {
                mainUI.reconnectAdb()
            }

            startItem -> {
                mainUI.startAdbLog()
            }

            stopItem -> {
                mainUI.stopAdbLog()
            }

            clearItem -> {
                mainUI.clearAdbLog()
            }
        }
    }
}