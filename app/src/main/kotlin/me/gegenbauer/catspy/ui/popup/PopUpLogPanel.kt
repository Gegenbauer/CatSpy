package me.gegenbauer.catspy.ui.popup

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.log.LogMainUI
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
        val logMainUI = DarkUIUtil.getParentOfType(this, LogMainUI::class.java)
        when (event.source) {
            reconnectItem -> {
                logMainUI.reconnectAdb()
            }

            startItem -> {
                logMainUI.startAdbLog()
            }

            stopItem -> {
                logMainUI.stopAdbLog()
            }

            clearItem -> {
                logMainUI.clearAdbLog()
            }
        }
    }
}