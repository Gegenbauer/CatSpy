package me.gegenbauer.catspy.ui.button

class TableBarButton(title:String) : GButton(title){
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