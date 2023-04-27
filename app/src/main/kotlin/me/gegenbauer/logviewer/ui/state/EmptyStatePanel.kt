package me.gegenbauer.logviewer.ui.state

import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.ui.button.GButton
import me.gegenbauer.logviewer.utils.loadIcon
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class EmptyStatePanel(private val content: JComponent, private val action: () -> Unit = {}) : JPanel() {
    private val emptyImage = GButton(loadIcon("empty_state.svg", w = 60, h = 60)).apply {
        preferredSize = Dimension(120, 120)
    }
    private val emptyContainer = JPanel().apply {
        layout = GridBagLayout()
        add(emptyImage, GridBagConstraints())
    }

    var contentVisible: Boolean = false
        set(value) {
            if (value == field) return
            GLog.d(TAG, "[contentVisible] set $value")
            field = value
            if (value) {
                remove(emptyContainer)
                add(content, BorderLayout.CENTER)
            } else {
                remove(content)
                add(emptyContainer, BorderLayout.CENTER)
            }
            revalidate()
            repaint()
        }

    init {
        layout = BorderLayout()

        emptyImage.isBorderPainted = false
        emptyImage.addActionListener {
            action()
        }

        add(emptyContainer, BorderLayout.CENTER)

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