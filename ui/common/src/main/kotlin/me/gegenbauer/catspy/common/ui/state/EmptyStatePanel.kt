package me.gegenbauer.catspy.common.ui.state

import me.gegenbauer.catspy.common.support.EmptyStatePanelTheme
import me.gegenbauer.catspy.common.ui.button.GButton
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.utils.loadIcon
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.TransferHandler

class EmptyStatePanel : JPanel() {
    var action: (JComponent) -> Unit = { _ -> }
    private lateinit var content: JComponent

    private val emptyImage = GButton(loadIcon("empty_state.svg", w = 60, h = 60)).apply {
        preferredSize = Dimension(120, 120)
        background = EmptyStatePanelTheme.iconBackground
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
            SwingUtilities.updateComponentTreeUI(this)
        }

    init {
        layout = BorderLayout()

        emptyImage.isBorderPainted = false
        emptyImage.addActionListener { action(it.source as JComponent) }

        add(emptyContainer, BorderLayout.CENTER)
    }

    fun setContent(content: JComponent) {
        this.content = content
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