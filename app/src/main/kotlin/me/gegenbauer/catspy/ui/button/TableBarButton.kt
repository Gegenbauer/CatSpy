package me.gegenbauer.catspy.ui.button

import me.gegenbauer.catspy.utils.setHeight

class TableBarButton(title:String) : GButton(title){
    var value = ""

    init {
        if (title.length > MAX_TITLE) {
            text = title.substring(0, MAX_TITLE) + ".."
        }
        configureHeight()
    }

    override fun updateUI() {
        super.updateUI()
        configureHeight()
    }

    private fun configureHeight() {
        val fontMetrics = getFontMetrics(font)
        setHeight(fontMetrics.height + 10)
    }

    companion object {
        private const val MAX_TITLE = 15
    }
}