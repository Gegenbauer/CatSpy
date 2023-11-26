package me.gegenbauer.catspy.view.button

import com.github.weisj.darklaf.iconset.AllIcons
import me.gegenbauer.catspy.utils.setBorderless
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JButton

class CloseButton(private val onClose: () -> Unit = {}): JButton() {

    init {
        icon = closeIconNormal

        isRolloverEnabled = false
        isContentAreaFilled = false
        setBorderless()

        preferredSize = Dimension(CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                setHover()
            }

            override fun mouseExited(e: MouseEvent) {
                setNormal()
            }
        })

        addActionListener { onClose()  }
    }

    fun setHover() {
        icon = closeIconHover
    }

    fun setNormal() {
        icon = closeIconNormal
    }

    companion object {
        private const val CLOSE_BUTTON_SIZE = 20
        private val closeIconNormal: Icon by lazy { AllIcons.Navigation.Close.get() }
        private val closeIconHover: Icon by lazy { AllIcons.Navigation.Close.hovered() }
    }
}