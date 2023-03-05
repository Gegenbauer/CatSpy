package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.ui.MainUI
import java.awt.Color
import javax.swing.JButton

open class ColorButton(title:String) : JButton(title){
    init {
        if (ConfigManager.LaF == MainUI.CROSS_PLATFORM_LAF) {
            background = Color(0xE5, 0xE5, 0xE5)
        }
    }
}

class TableBarButton(title:String) : ColorButton(title){
    var value = ""

    init {
        if (title.length > MAX_TITLE) {
            text = title.substring(0, MAX_TITLE) + ".."
        }
    }

    companion object {
        private const val MAX_TITLE = 15
    }
}