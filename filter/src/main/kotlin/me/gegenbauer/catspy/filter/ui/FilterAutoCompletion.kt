package me.gegenbauer.catspy.filter.ui

import me.gegenbauer.catspy.databinding.property.support.getFieldDeeply
import me.gegenbauer.catspy.databinding.property.support.invokeMethod
import org.fife.ui.autocomplete.AutoCompletion
import org.fife.ui.autocomplete.Completion
import org.fife.ui.autocomplete.CompletionProvider
import javax.swing.JWindow

class FilterAutoCompletion(provider: CompletionProvider) : AutoCompletion(provider) {

    fun insertCurrentCompletion() {
        val completion = getCurrentSelectedCompletion()
        completion?.let { insertCompletion(it) }
    }

    private fun getCurrentSelectedCompletion(): Completion? {
        val popupWindow = getPopupWindow()
        return if (popupWindow != null && popupWindow.isVisible) {
            val result = popupWindow.invokeMethod("getSelection") as? Result<Completion>
            result?.getOrNull()
        } else {
            null
        }
    }

    private fun getPopupWindow(): JWindow? {
        val popupField = getFieldDeeply("popupWindow")
        return popupField.get(this) as? JWindow
    }
}