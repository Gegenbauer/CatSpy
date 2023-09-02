package me.gegenbauer.catspy.log.ui.dialog

import me.gegenbauer.catspy.utils.Key
import me.gegenbauer.catspy.utils.keyEventInfo
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

class GoToDialog(parent: JFrame) : JDialog(parent, "GoTo line", true) {

    var line = INVALID_LINE_NUM
        private set

    private val textField = JTextField()
    private val label = JLabel(" GoTo : ")

    init {
        textField.addKeyListener(KeyHandler())
        textField.alignmentX = JTextField.CENTER_ALIGNMENT
        textField.preferredSize = Dimension(60, 30)
        label.preferredSize = Dimension(70, 30)
        val panel = JPanel(BorderLayout())
        panel.add(textField, BorderLayout.CENTER)
        panel.add(label, BorderLayout.WEST)
        contentPane.add(panel)
        pack()
    }

    internal inner class KeyHandler : KeyAdapter() {
        override fun keyReleased(event: KeyEvent) {
            when(event.keyEventInfo) {
                Key.ESCAPE -> {
                    line = INVALID_LINE_NUM
                    dispose()
                }
                Key.ENTER -> {
                    line = textField.text.trim().takeIf { it.isNotEmpty() }?.runCatching {
                        textField.text.toInt()
                    }?.getOrNull() ?: INVALID_LINE_NUM
                    dispose()
                }
            }
        }
    }

    companion object {
        const val INVALID_LINE_NUM = -1
    }
}