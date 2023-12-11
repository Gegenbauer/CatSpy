package me.gegenbauer.catspy.view.panel

import me.gegenbauer.catspy.view.button.IconBarButton
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JPanel

interface StatusIconManager {
    fun addStatusIcon(icon: StatusIcon)

    fun removeStatusIcon(icon: StatusIcon)

    fun getStatusIcons(): List<StatusIcon>

    fun updateAllIcons()
}

interface StatusIcon {
    val icon: Icon

    val onClick: () -> Unit

    var state: State

    var host: Component?

    enum class State {
        NORMAL, WARNING, ERROR
    }
}

class StatusIconsBar: JPanel(), StatusIconManager {
    private val icons = mutableListOf<StatusIcon>()

    init {
        border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
    }

    override fun addStatusIcon(icon: StatusIcon) {
        icons.add(icon)
        val label = IconBarButton(icon.icon)
        label.isFocusable = false
        icon.host = label
        label.addActionListener { icon.onClick() }
        add(label)
        parent?.revalidate()
    }

    override fun removeStatusIcon(icon: StatusIcon) {
        icons.remove(icon)
        remove(icon.host)
    }

    override fun getStatusIcons(): List<StatusIcon> {
        return icons
    }

    override fun updateAllIcons() {
        icons.forEach { it.state = StatusIcon.State.NORMAL }
    }

}