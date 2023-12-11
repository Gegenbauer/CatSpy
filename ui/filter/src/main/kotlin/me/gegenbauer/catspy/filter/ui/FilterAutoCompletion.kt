package me.gegenbauer.catspy.filter.ui

import me.gegenbauer.catspy.java.ext.getFieldDeeply
import me.gegenbauer.catspy.java.ext.invokeMethod
import org.fife.ui.autocomplete.AutoCompletion
import org.fife.ui.autocomplete.Completion
import org.fife.ui.autocomplete.CompletionProvider
import javax.swing.JWindow

class FilterAutoCompletion(
    provider: CompletionProvider,
    private var onCompletionsShow: (() -> Unit)?
) : AutoCompletion(provider) {

    fun insertCurrentCompletion() {
        val completion = getCurrentSelectedCompletion()
        completion?.let { insertCompletion(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentSelectedCompletion(): Completion? {
        val popupWindow = getPopupWindow()
        return if (popupWindow != null && popupWindow.isVisible) {
            val result = popupWindow.invokeMethod("getSelection") as? Completion
            result
        } else {
            null
        }
    }

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