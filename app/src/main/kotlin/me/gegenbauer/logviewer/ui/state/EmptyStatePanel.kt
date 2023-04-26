package me.gegenbauer.logviewer.ui.state

import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.utils.loadIcon
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.TransferHandler

class EmptyStatePanel(private val content: JComponent) : JPanel() {
    private val emptyImage = JLabel(loadIcon("empty_state.svg", w = 40, h = 40))

    var contentVisible: Boolean = false
        set(value) {
            if (value == field) return
            GLog.d(TAG, "[contentVisible] set $value")
            field = value
            if (value) {
                remove(emptyImage)
                add(content, BorderLayout.CENTER)
            } else {
                remove(content)
                add(emptyImage, BorderLayout.CENTER)
            }
            revalidate()
            repaint()
        }

    init {
        layout = BorderLayout()

        add(emptyImage, BorderLayout.CENTER)

        content.transferHandler?.let {
            transferHandler = object : TransferHandler() {
                override fun canImport(support: TransferSupport?): Boolean {
                    return it.canImport(support)
                }

                override fun importData(support: TransferSupport?): Boolean {
                    return it.importData(support)
                }
            }
        }
    }

    companion object {
        private const val TAG = "EmptyStatePanel"
    }
}