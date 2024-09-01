package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.java.ext.EMPTY_STRING

class TableBarButton(title:String) : GButton(title){
    var value = EMPTY_STRING

    init {
        if (title.length > MAX_TITLE) {
            text = title.substring(0, MAX_TITLE) + ".."
        }
    }

    companion object {
        private const val MAX_TITLE = 15
    }
}