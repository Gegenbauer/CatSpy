package me.gegenbauer.logviewer.ui.button

import javax.swing.JButton

class TableBarButton(title:String) : JButton(title){
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