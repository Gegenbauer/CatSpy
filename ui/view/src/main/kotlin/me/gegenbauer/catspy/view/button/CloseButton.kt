package me.gegenbauer.catspy.view.button

import com.github.weisj.darklaf.iconset.AllIcons
import me.gegenbauer.catspy.utils.ui.setBorderless
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JButton

class CloseButton(
    size: Int = DEFAULT_CLOSE_BUTTON_SIZE,
    var onClose: () -> Unit = {},
) : JButton(), ActionListener {

    constructor(onClose: () -> Unit) : this(DEFAULT_CLOSE_BUTTON_SIZE, onClose)

    init {
        icon = closeIconNormal

        isRolloverEnabled = false
        isContentAreaFilled = false
        setBorderless()

        preferredSize = Dimension(size, size)

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                setHover()
            }

            override fun mouseExited(e: MouseEvent) {
                setNormal()
            }
        })

        addActionListener(this)
    }

    fun setHover() {
        icon = closeIconHover
    }

    fun setNormal() {
        icon = closeIconNormal
    }

    companion object {
        private const val DEFAULT_CLOSE_BUTTON_SIZE = 20
        private val closeIconNormal: Icon by lazy { AllIcons.Navigation.Close.get() }
        private val closeIconHover: Icon by lazy { AllIcons.Navigation.Close.hovered() }
    }

    override fun actionPerformed(e: ActionEvent) {
        onClose()
    }
}