package me.gegenbauer.catspy.filter.ui

import me.gegenbauer.catspy.java.ext.getFieldDeeply
import org.fife.ui.autocomplete.AutoCompletion
import org.fife.ui.autocomplete.CompletionProvider
import javax.swing.JWindow

class FilterAutoCompletion(
    provider: CompletionProvider,
    private var onCompletionsShow: (() -> Unit)?
) : AutoCompletion(provider) {

    override fun setPopupVisible(visible: Boolean) {
        if (getPopupWindow()?.isVisible == false && visible) {
            onCompletionsShow?.invoke()
        }
        super.setPopupVisible(visible)
    }

    override fun uninstall() {
        super.uninstall()
        onCompletionsShow = null
    }

    private fun getPopupWindow(): JWindow? {
        val popupField = getFieldDeeply("popupWindow")
        return popupField[this] as? JWindow
    }
}