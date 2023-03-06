package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.ui.container.WrapLayout
import javax.swing.JPanel

class ButtonPanel : JPanel() {
    init {
        layout = WrapLayout(HORIZONTAL_GAP, VERTICAL_GAP)
    }

    companion object {
        private const val HORIZONTAL_GAP = 3
        private const val VERTICAL_GAP = 3
    }
}