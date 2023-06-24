package me.gegenbauer.catspy.ui.script

import me.gegenbauer.catspy.ui.button.IconBarButton
import me.gegenbauer.catspy.utils.loadThemedIcon
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JPanel

class ScriptActionBar: JPanel(), ActionListener {
    private val edit = IconBarButton(loadThemedIcon("edit.svg", 20))
    private var editAction: (() -> Unit) = {}

    init {
        isOpaque = false
        layout = FlowLayout(FlowLayout.TRAILING)

        edit.addActionListener(this)

        add(edit)
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.source) {
            edit -> editAction()
        }
    }

    fun setEditAction(action: () -> Unit) {
        editAction = action
    }
}