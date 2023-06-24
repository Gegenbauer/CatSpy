package me.gegenbauer.catspy.ui.button

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
    }

    companion object {
        private const val MAX_TITLE = 15
    }
}